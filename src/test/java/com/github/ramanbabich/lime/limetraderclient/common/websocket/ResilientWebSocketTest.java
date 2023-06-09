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

import java.net.URI;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Builder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Raman Babich
 */
class ResilientWebSocketTest {

  private final Builder webSocketBuilder = Mockito.mock(Builder.class);
  private final WebSocketBuilderProvider webSocketBuilderProvider =
      Mockito.mock(WebSocketBuilderProvider.class);

  {
    Mockito.doReturn(webSocketBuilder)
        .when(webSocketBuilderProvider).provide();
  }

  private final URI uri = URI.create("wss://localhost/websocket-api");
  private final TextSubscriber textSubscriber = Mockito.mock(TextSubscriber.class);
  private final ResilientWebSocket resilientWebSocket = new ResilientWebSocket(
      "test-web-socket",
      uri,
      webSocketBuilderProvider,
      Long.MAX_VALUE,
      textSubscriber);

  @Test
  @SuppressWarnings("unchecked")
  void shouldInsertToStateIfAbsentAndRemoveFromStateWith() throws Exception {
    String property = "property";
    String dataToSubscribe = "data-to-subscribe";
    String dataToUnsubscribe = "data-to-unsubscribe";
    CompletableFuture<WebSocket> webSocketFuture =
        (CompletableFuture<WebSocket>) Mockito.mock(CompletableFuture.class);
    Mockito.doReturn(webSocketFuture)
        .when(webSocketBuilder).buildAsync(Mockito.eq(uri), Mockito.any());
    WebSocket webSocket = Mockito.mock(WebSocket.class);
    Mockito.doReturn(webSocket)
        .when(webSocketFuture).get(10, TimeUnit.SECONDS);
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendPing(Mockito.any());
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendText(dataToSubscribe, true);

    Assertions.assertTrue(resilientWebSocket.insertToStateIfAbsent(property, dataToSubscribe));
    Assertions.assertFalse(resilientWebSocket.insertToStateIfAbsent(property, dataToSubscribe));
    Assertions.assertEquals(dataToSubscribe, resilientWebSocket.getFromState(property));
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendText(dataToUnsubscribe, true);
    Assertions.assertEquals(
        dataToSubscribe,
        resilientWebSocket.removeFromStateWith(property, dataToUnsubscribe));
    Assertions.assertNull(resilientWebSocket.getFromState(property));

    Mockito.verify(webSocketFuture, Mockito.atLeast(3))
        .get(10, TimeUnit.SECONDS);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldUpdateState() throws Exception {
    String property = "property";
    String data = "data";
    CompletableFuture<WebSocket> webSocketFuture =
        (CompletableFuture<WebSocket>) Mockito.mock(CompletableFuture.class);
    Mockito.doReturn(webSocketFuture)
        .when(webSocketBuilder).buildAsync(Mockito.eq(uri), Mockito.any());
    WebSocket webSocket = Mockito.mock(WebSocket.class);
    Mockito.doReturn(webSocket)
        .when(webSocketFuture).get(10, TimeUnit.SECONDS);
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendPing(Mockito.any());
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendText(data, true);

    Assertions.assertNull(resilientWebSocket.updateState(property, data, oldData -> {
      Assertions.assertNull(oldData);
      return null;
    }));
    Assertions.assertEquals(data, resilientWebSocket.updateState(property, data, oldData -> {
      Assertions.assertEquals(data, oldData);
      return null;
    }));
    Assertions.assertEquals(data, resilientWebSocket.getFromState(property));
    String newData = "new-" + data;
    String transitionData = "transition-" + data;
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendText(transitionData, true);
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendText(newData, true);
    Assertions.assertEquals(data, resilientWebSocket.updateState(property, newData, oldData -> {
      Assertions.assertEquals(data, oldData);
      return transitionData;
    }));
    Assertions.assertEquals(newData, resilientWebSocket.getFromState(property));
    Assertions.assertEquals(newData, resilientWebSocket.updateState(property, null, oldData -> {
      Assertions.assertEquals(newData, oldData);
      return transitionData;
    }));
    Assertions.assertNull(resilientWebSocket.getFromState(property));

    Mockito.verify(webSocketFuture, Mockito.atLeast(5))
        .get(10, TimeUnit.SECONDS);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetFromState() throws Exception {
    String property = "property";
    String dataToSubscribe = "data-to-subscribe";
    String dataToUnsubscribe = "data-to-unsubscribe";
    CompletableFuture<WebSocket> webSocketFuture =
        (CompletableFuture<WebSocket>) Mockito.mock(CompletableFuture.class);
    Mockito.doReturn(webSocketFuture)
        .when(webSocketBuilder).buildAsync(Mockito.eq(uri), Mockito.any());
    WebSocket webSocket = Mockito.mock(WebSocket.class);
    Mockito.doReturn(webSocket)
        .when(webSocketFuture).get(10, TimeUnit.SECONDS);
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendPing(Mockito.any());
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendText(dataToSubscribe, true);

    Assertions.assertTrue(resilientWebSocket.insertToStateIfAbsent(property, dataToSubscribe));
    Assertions.assertFalse(resilientWebSocket.insertToStateIfAbsent(property, dataToSubscribe));
    Assertions.assertEquals(dataToSubscribe, resilientWebSocket.getFromState(property));
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendText(dataToUnsubscribe, true);
    Assertions.assertEquals(
        dataToSubscribe,
        resilientWebSocket.removeFromStateWith(property, dataToUnsubscribe));
    Assertions.assertNull(resilientWebSocket.getFromState(property));

    Mockito.verify(webSocketFuture, Mockito.atLeast(3))
        .get(10, TimeUnit.SECONDS);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldClose() throws Exception {
    String property = "property";
    String data = "data";
    CompletableFuture<WebSocket> webSocketFuture =
        (CompletableFuture<WebSocket>) Mockito.mock(CompletableFuture.class);
    Mockito.doReturn(webSocketFuture)
        .when(webSocketBuilder).buildAsync(Mockito.eq(uri), Mockito.any());
    WebSocket webSocket = Mockito.mock(WebSocket.class);
    Mockito.doReturn(webSocket)
        .when(webSocketFuture).get(10, TimeUnit.SECONDS);
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendPing(Mockito.any());
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendText(data, true);
    Assertions.assertTrue(resilientWebSocket.insertToStateIfAbsent(property, data));
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendClose(Mockito.eq(WebSocket.NORMAL_CLOSURE), Mockito.any());
    Mockito.doReturn(false, true)
        .when(webSocket).isOutputClosed();
    Mockito.doReturn(true)
        .when(webSocket).isInputClosed();


    resilientWebSocket.close();

    Assertions.assertThrows(IllegalResilientWebSocketStateException.class,
        () -> resilientWebSocket.insertToStateIfAbsent(property, data));
    Assertions.assertThrows(IllegalResilientWebSocketStateException.class,
        () -> resilientWebSocket.removeFromStateWith(property, data));
    Assertions.assertThrows(IllegalResilientWebSocketStateException.class,
        () -> resilientWebSocket.updateState(property, data, oldData -> null));
    Assertions.assertThrows(IllegalResilientWebSocketStateException.class,
        () -> resilientWebSocket.getFromState(property));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldRecoverAfterWebSocketClose() throws Exception {
    String property = "property";
    String data = "data";
    CompletableFuture<WebSocket> webSocketFuture =
        (CompletableFuture<WebSocket>) Mockito.mock(CompletableFuture.class);
    Mockito.doReturn(webSocketFuture)
        .when(webSocketBuilder).buildAsync(Mockito.eq(uri), Mockito.any());
    WebSocket webSocket = Mockito.mock(WebSocket.class);
    Mockito.doReturn(webSocket)
        .when(webSocketFuture).get(10, TimeUnit.SECONDS);
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendPing(Mockito.any());
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendText(data, true);
    Assertions.assertTrue(resilientWebSocket.insertToStateIfAbsent(property, data));
    ArgumentCaptor<ResilientWebSocketSubscriber> subscriberArgumentCaptor =
        ArgumentCaptor.forClass(ResilientWebSocketSubscriber.class);
    Mockito.verify(webSocketBuilder)
        .buildAsync(Mockito.eq(uri), subscriberArgumentCaptor.capture());
    ResilientWebSocketSubscriber subscriber = subscriberArgumentCaptor.getValue();
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendClose(Mockito.eq(WebSocket.NORMAL_CLOSURE), Mockito.any());
    Mockito.doReturn(false, true)
        .when(webSocket).isOutputClosed();
    Mockito.doReturn(true)
        .when(webSocket).isInputClosed();

    subscriber.onClose(webSocket, WebSocket.NORMAL_CLOSURE, "end-of-session");

    Assertions.assertEquals(data, resilientWebSocket.getFromState(property));

    Mockito.verify(webSocketBuilder, Mockito.times(2))
        .buildAsync(Mockito.eq(uri), Mockito.any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldRecoverAfterWebSocketError() throws Exception {
    String property = "property";
    String data = "data";
    CompletableFuture<WebSocket> webSocketFuture =
        (CompletableFuture<WebSocket>) Mockito.mock(CompletableFuture.class);
    Mockito.doReturn(webSocketFuture)
        .when(webSocketBuilder).buildAsync(Mockito.eq(uri), Mockito.any());
    WebSocket webSocket = Mockito.mock(WebSocket.class);
    Mockito.doReturn(webSocket)
        .when(webSocketFuture).get(10, TimeUnit.SECONDS);
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendPing(Mockito.any());
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendText(data, true);
    Assertions.assertTrue(resilientWebSocket.insertToStateIfAbsent(property, data));
    ArgumentCaptor<ResilientWebSocketSubscriber> subscriberArgumentCaptor =
        ArgumentCaptor.forClass(ResilientWebSocketSubscriber.class);
    Mockito.verify(webSocketBuilder)
        .buildAsync(Mockito.eq(uri), subscriberArgumentCaptor.capture());
    ResilientWebSocketSubscriber subscriber = subscriberArgumentCaptor.getValue();
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendClose(Mockito.eq(WebSocket.NORMAL_CLOSURE), Mockito.any());
    Mockito.doReturn(false, true)
        .when(webSocket).isOutputClosed();
    Mockito.doReturn(true)
        .when(webSocket).isInputClosed();

    subscriber.onError(webSocket, new RuntimeException("Test runtime exception."));

    Assertions.assertEquals(data, resilientWebSocket.getFromState(property));

    Mockito.verify(webSocketBuilder, Mockito.times(2))
        .buildAsync(Mockito.eq(uri), Mockito.any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldRecoverAfterPingPongGameOver() throws Exception {
    long pingIntervalMillis = 250L;
    ResilientWebSocket resilientWebSocket = new ResilientWebSocket(
        "test-web-socket",
        uri,
        webSocketBuilderProvider,
        pingIntervalMillis,
        textSubscriber);
    String property = "property";
    String data = "data";
    CompletableFuture<WebSocket> webSocketFuture =
        (CompletableFuture<WebSocket>) Mockito.mock(CompletableFuture.class);
    Mockito.doReturn(webSocketFuture)
        .when(webSocketBuilder).buildAsync(Mockito.eq(uri), Mockito.any());
    WebSocket webSocket = Mockito.mock(WebSocket.class);
    Mockito.doReturn(webSocket)
        .when(webSocketFuture).get(10, TimeUnit.SECONDS);
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendPing(Mockito.any());
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendText(data, true);
    Assertions.assertTrue(resilientWebSocket.insertToStateIfAbsent(property, data));
    Mockito.doReturn(webSocketFuture)
        .when(webSocket).sendClose(Mockito.eq(WebSocket.NORMAL_CLOSURE), Mockito.any());
    Mockito.doReturn(false, true)
        .when(webSocket).isOutputClosed();
    Mockito.doReturn(true)
        .when(webSocket).isInputClosed();
    TimeUnit.MILLISECONDS.sleep((int) (pingIntervalMillis * 1.5));
    Assertions.assertEquals(data, resilientWebSocket.getFromState(property));

    Mockito.verify(webSocketBuilder, Mockito.atLeast(2))
        .buildAsync(Mockito.eq(uri), Mockito.any());
  }

  @Test
  void shouldGetTextSubscriber() {
    Assertions.assertEquals(textSubscriber, resilientWebSocket.getTextSubscriber(TextSubscriber.class));
  }

}
