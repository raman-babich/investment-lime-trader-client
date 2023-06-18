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

package com.ramanbabich.investment.limetraderclient.common.websocket;

import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Raman Babich
 */
public class ResilientWebSocketSubscriber implements Listener {

  private static final Logger LOG = LoggerFactory.getLogger(ResilientWebSocketSubscriber.class);

  private volatile boolean listen = false;
  private final StringBuilder dataAccumulator;
  private final TextSubscriber textSubscriber;
  private final PongSubscriber pongSubscriber;
  private final CloseSubscriber closeSubscriber;
  private final ErrorSubscriber errorSubscriber;

  public ResilientWebSocketSubscriber(TextSubscriber textSubscriber,
      PongSubscriber pongSubscriber,
      CloseSubscriber closeSubscriber,
      ErrorSubscriber errorSubscriber) {
    this.dataAccumulator = new StringBuilder();
    this.textSubscriber = textSubscriber;
    this.pongSubscriber = pongSubscriber;
    this.closeSubscriber = closeSubscriber;
    this.errorSubscriber = errorSubscriber;
  }

  public boolean listen(boolean listen) {
    boolean temp = this.listen;
    this.listen = listen;
    return temp;
  }

  @Override
  public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
    if (!listen) {
      webSocket.request(1);
      return null;
    }
    dataAccumulator.append(data);
    if (!last) {
      webSocket.request(1);
      return null;
    }
    String text = dataAccumulator.toString();
    try {
      TextSubscriber threadSafeSubscriber = textSubscriber;
      if (threadSafeSubscriber != null) {
        threadSafeSubscriber.onText(text);
      }
    } catch (Throwable throwable) {
      LOG.warn("Error occurred during processing '{}' text.", text, throwable);
    }
    dataAccumulator.setLength(0);
    webSocket.request(1);
    return null;
  }

  @Override
  public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
    PongSubscriber threadSafeSubscriber = pongSubscriber;
    if (threadSafeSubscriber != null) {
      threadSafeSubscriber.onPong(webSocket, message);
    }
    webSocket.request(1);
    return null;
  }

  @Override
  public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
    CloseSubscriber threadSafeSubscriber = closeSubscriber;
    if (threadSafeSubscriber != null) {
      threadSafeSubscriber.onClose(webSocket, statusCode, reason);
    }
    return null;
  }

  @Override
  public void onError(WebSocket webSocket, Throwable error) {
    ErrorSubscriber threadSafeSubscriber = errorSubscriber;
    if (threadSafeSubscriber != null) {
      threadSafeSubscriber.onError(webSocket, error);
    }
  }

  interface CloseSubscriber {
    void onClose(WebSocket webSocket, int statusCode, String reason);
  }

  interface ErrorSubscriber {
    void onError(WebSocket webSocket, Throwable error);
  }

  interface PongSubscriber {
    void onPong(WebSocket webSocket, ByteBuffer message);
  }

}
