/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.ramanbabich.lime.limetraderclient.common.websocket;

import com.github.ramanbabich.lime.limetraderclient.common.pingpong.PingPongPlayer;
import java.net.URI;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Raman Babich
 */
public class ResilientWebSocket implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(ResilientWebSocket.class);

  private final String id;
  private final WebSocketBuilderProvider webSocketBuilderProvider;
  private final URI uri;
  private WebSocket webSocket;
  private ResilientWebSocketSubscriber webSocketSubscriber;
  private final TextSubscriber textSubscriber;
  private PingPongPlayer pingPongPlayer;
  private final Long pingIntervalMillis;
  private Thread pingPongPlayerThread;
  private final Map<String, String> state = new TreeMap<>();
  private boolean closed = false;

  public ResilientWebSocket(String id, URI uri, WebSocketBuilderProvider webSocketBuilderProvider,
      TextSubscriber textSubscriber) {
    this(id, uri, webSocketBuilderProvider, null, textSubscriber);
  }

  public ResilientWebSocket(String id, URI uri, WebSocketBuilderProvider webSocketBuilderProvider,
      Long pingIntervalMillis, TextSubscriber textSubscriber) {
    this.id = Objects.requireNonNull(id, "Id should be specified");
    this.uri = Objects.requireNonNull(uri, "Uri should be specified");
    this.webSocketBuilderProvider = Objects.requireNonNull(
        webSocketBuilderProvider, "Web socket builder provider should be specified.");
    Objects.requireNonNull(this.webSocketBuilderProvider.provide(),
        "Web socket builder provider should provide builder for web socket.");
    this.pingIntervalMillis = pingIntervalMillis;
    this.textSubscriber = textSubscriber;
  }

  public synchronized boolean insertToStateIfAbsent(String property, String data) {
    try {
      return doInsertToState(property, data);
    } catch (ExecutionException | TimeoutException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new StateChangeFailedException(
          String.format("Failed during inserting '%s' to resilient web socket '%s' state.",
              property, id),
          ex);
    }
  }

  private boolean doInsertToState(String property, String data)
      throws ExecutionException, TimeoutException, InterruptedException {
    Objects.requireNonNull(property, "Property should be specified.");
    Objects.requireNonNull(data, "Data should be specified.");
    verifyNotClosed();
    if (state.containsKey(property)) {
      return false;
    }
    setupWebSocketRetryingException();
    webSocket.sendText(data, true).get(10, TimeUnit.SECONDS);
    state.put(property, data);
    return true;
  }

  private void setupWebSocketRetryingException() throws InterruptedException {
    if (webSocket != null) {
      return;
    }
    int attemptNumber = 0;
    int backoffMillis = 1000;
    int maxBackoffMillis = 5000;
    boolean setupSuccessfully = false;
    while (!setupSuccessfully) {
      try {
        ++attemptNumber;
        LOG.info("Setting up resilient web socket '{}', attempt number '{}'.",
            id, attemptNumber);
        setupWebSocket();
        setupSuccessfully = true;
        LOG.info("Resilient web socket '{}' is set up during attempt number '{}'.",
            id, attemptNumber);
      } catch (ExecutionException | TimeoutException ex) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Attempt number '{}' failed to setup resilient web socket '{}'",
              attemptNumber, id, ex);
        } else {
          LOG.info("Attempt number '{}' failed to setup resilient web socket '{}'",
              attemptNumber, id);
        }
        if (backoffMillis < maxBackoffMillis) {
          TimeUnit.MILLISECONDS.sleep(backoffMillis);
          backoffMillis += backoffMillis;
        } else {
          TimeUnit.MILLISECONDS.sleep(maxBackoffMillis);
        }
      }
    }
  }

  private void setupWebSocket() throws ExecutionException, TimeoutException, InterruptedException {
    if (webSocket != null) {
      return;
    }
    webSocketSubscriber = buildResilientWebSocketSubscriber(textSubscriber);
    webSocket = webSocketBuilderProvider.provide()
        .buildAsync(uri, webSocketSubscriber)
        .get(10, TimeUnit.SECONDS);
    pingPongPlayer = new PingPongPlayer(
        id,
        new WebSocketPingSender(id, webSocket),
        pingIntervalMillis,
        player -> {
          LOG.info("Resilient web socket '{}' didn't receive pong event. "
              + "Recovering will be performed.", id);
          recoverWebSocket(((WebSocketPingSender) player.getPingSender()).getWebSocket());
        });
    pingPongPlayerThread = new Thread(pingPongPlayer, id + "-ping-pong-player");
    pingPongPlayerThread.setDaemon(true);
    pingPongPlayerThread.start();
    webSocketSubscriber.listen(true);
  }

  private ResilientWebSocketSubscriber buildResilientWebSocketSubscriber(
      TextSubscriber textSubscriber) {
    return new ResilientWebSocketSubscriber(
        textSubscriber,
        (targetWebSocket, message) -> sendPongToPingPongPlayer(targetWebSocket),
        (targetWebSocket, statusCode, reason) -> {
          LOG.info("Resilient web socket '{}' received close event with '{}' status code "
                  + "and '{}' reason. Recovering will be performed.",
              id, statusCode, reason);
          recoverWebSocket(targetWebSocket);
        },
        (targetWebSocket, error) -> {
          LOG.warn("Resilient web socket '{}' received error event. Recovering will be performed.",
              id, error);
          recoverWebSocket(targetWebSocket);
        });
  }

  private synchronized void sendPongToPingPongPlayer(WebSocket targetWebSocket) {
    if (webSocket != targetWebSocket) {
      return;
    }
    PingPongPlayer threadSafePlayer = pingPongPlayer;
    if (threadSafePlayer != null) {
      threadSafePlayer.onPong();
    }
  }

  private synchronized void recoverWebSocket(WebSocket targetWebSocket) {
    try {
      if (webSocket != targetWebSocket) {
        LOG.info("Recovering for '{}' will not be performed because target web socket "
            + "is not available. An attempt will be made to close it.", id);
        try {
          closeWebSocket(targetWebSocket);
        } catch (ExecutionException | TimeoutException ex) {
          LOG.info("Attempt to close target web socket for '{}' is failed. "
              + "It perhaps is closed already. Can't perform any additional actions to free "
              + "the resources, that is why ignoring this exceptional case.", id);
        }
        return;
      }
      closeWebSocketRetryingException();
      if (!state.isEmpty()) {
        Map<String, String> expectedState = new TreeMap<>(state);
        state.clear();
        for (Entry<String, String> propertyData : expectedState.entrySet()) {
          doInsertToStateRetryingException(propertyData.getKey(), propertyData.getValue());
        }
      }
      LOG.info("Resilient web socket '{}' is recovered.", id);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      LOG.error("Resilient web socket '{}' recovering thread is interrupted, "
          + "this led to illegal state. Any further operation could has "
          + "unpredictable behaviour.", id, ex);
    }
  }

  @Override
  public synchronized void close() {
    try {
      closeWebSocketRetryingException();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    state.clear();
    closed = true;
  }

  private void verifyNotClosed() {
    if (closed) {
      throw new IllegalResilientWebSocketStateException(
          String.format("Resilient web socket '%s' is closed.", id));
    }
  }

  private void closeWebSocketRetryingException() throws InterruptedException {
    if (webSocket == null) {
      return;
    }
    int attemptNumber = 0;
    int backoffMillis = 1000;
    int maxBackoffMillis = 5000;
    boolean closedSuccessfully = false;
    while (!closedSuccessfully) {
      try {
        ++attemptNumber;
        LOG.info("Closing resilient web socket '{}', attempt number '{}'.",
            id, attemptNumber);
        closeWebSocket();
        closedSuccessfully = true;
        LOG.info("Resilient web socket '{}' is closed during '{}' attempt number.",
            id, attemptNumber);
      } catch (ExecutionException | TimeoutException ex) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Attempt number '{}' failed to close resilient web socket '{}'.",
              attemptNumber, id, ex);
        } else {
          LOG.info("Attempt number '{}' failed to close resilient web socket '{}'.",
              attemptNumber, id);
        }
        if (backoffMillis < maxBackoffMillis) {
          TimeUnit.MILLISECONDS.sleep(backoffMillis);
          backoffMillis += backoffMillis;
        } else {
          TimeUnit.MILLISECONDS.sleep(maxBackoffMillis);
        }
      }
    }
  }

  private void closeWebSocket() throws ExecutionException, TimeoutException, InterruptedException {
    if (webSocket == null) {
      return;
    }
    webSocketSubscriber.listen(false);
    pingPongPlayerThread.interrupt();
    closeWebSocket(webSocket);
    webSocketSubscriber = null;
    pingPongPlayerThread = null;
    pingPongPlayer = null;
    webSocket = null;
  }

  private void closeWebSocket(WebSocket webSocket)
      throws ExecutionException, TimeoutException, InterruptedException {
    if (!webSocket.isOutputClosed()) {
      webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "end-of-session")
          .get(10, TimeUnit.SECONDS);
      int repeatNumber = 0;
      while (repeatNumber < 5 && (!webSocket.isInputClosed() || !webSocket.isOutputClosed())) {
        ++repeatNumber;
        TimeUnit.SECONDS.sleep(2);
      }
    }
    if (!webSocket.isInputClosed() || !webSocket.isOutputClosed()) {
      LOG.warn("Can't wait more for closing of resilient web socket '{}' the input is closed({}), "
              + "the output is closed({}), that is why web socket will be closed abruptly.",
          id, webSocket.isInputClosed(), webSocket.isOutputClosed());
      webSocket.abort();
    }
  }

  private void doInsertToStateRetryingException(String property, String data)
      throws InterruptedException {
    int attemptNumber = 0;
    int backoffMillis = 1000;
    int maxBackoffMillis = 5000;
    boolean insertedSuccessfully = false;
    while (!insertedSuccessfully) {
      try {
        ++attemptNumber;
        LOG.info("Inserting property '{}' to resilient web socket '{}' state, attempt number '{}'.",
            property, id, attemptNumber);
        doInsertToState(property, data);
        insertedSuccessfully = true;
        LOG.info("Property '{}' is inserted to resilient web socket '{}' state during "
                + "attempt number '{}'.",
            property, id, attemptNumber);
      } catch (ExecutionException | TimeoutException ex) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Attempt number '{}' failed to insert to "
                  + "resilient web socket '{}' property '{}'.",
              attemptNumber, id, property, ex);
        } else {
          LOG.info("Attempt number '{}' failed to insert to "
                  + "resilient web socket '{}' property '{}'.",
              attemptNumber, id, property);
        }
        if (backoffMillis < maxBackoffMillis) {
          TimeUnit.MILLISECONDS.sleep(backoffMillis);
          backoffMillis += backoffMillis;
        } else {
          TimeUnit.MILLISECONDS.sleep(maxBackoffMillis);
        }
      }
    }
  }

  public synchronized String updateState(String property,
      String newData, Function<String, String> stateTransitionDataFunction) {
    try {
      return doUpdateState(property, newData, stateTransitionDataFunction);
    } catch (ExecutionException | TimeoutException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new StateChangeFailedException(
          String.format("Failed during updating '%s' in resilient web socket '%s' state.",
              property, id),
          ex);
    }
  }

  private String doUpdateState(String property, String newData,
      Function<String, String> stateTransitionDataFunction)
      throws ExecutionException, TimeoutException, InterruptedException {
    Objects.requireNonNull(property, "Property should be specified.");
    Objects.requireNonNull(
        stateTransitionDataFunction, "State transition data function should be specified.");
    verifyNotClosed();
    String currentData = state.get(property);
    String dataForStateTransition = stateTransitionDataFunction.apply(currentData);
    if (dataForStateTransition != null) {
      setupWebSocketRetryingException();
      webSocket.sendText(dataForStateTransition, true).get(10, TimeUnit.SECONDS);
    }
    if (newData == null) {
      return state.remove(property);
    }
    if (!newData.equals(currentData)) {
      setupWebSocketRetryingException();
      webSocket.sendText(newData, true).get(10, TimeUnit.SECONDS);
      return state.put(property, newData);
    }
    return currentData;
  }

  public synchronized String removeFromStateWith(String property, String dataForStateTransition) {
    try {
      return doRemoveFromStateWith(property, dataForStateTransition);
    } catch (ExecutionException | TimeoutException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new StateChangeFailedException(
          String.format("Failed during removing '%s' from resilient web socket '%s' state.",
              property, id),
          ex);
    }
  }

  private String doRemoveFromStateWith(String property, String dataForStateTransition)
      throws ExecutionException, TimeoutException, InterruptedException {
    Objects.requireNonNull(property, "Property should be specified.");
    verifyNotClosed();
    if (!state.containsKey(property)) {
      return null;
    }
    if (dataForStateTransition != null) {
      setupWebSocketRetryingException();
      webSocket.sendText(dataForStateTransition, true).get(10, TimeUnit.SECONDS);
    }
    return state.remove(property);
  }

  public synchronized String getFromState(String property) {
    verifyNotClosed();
    return state.get(property);
  }

  @SuppressWarnings("unchecked")
  public <T extends TextSubscriber> T getTextSubscriber(Class<T> clazz) {
    return (T) textSubscriber;
  }
}
