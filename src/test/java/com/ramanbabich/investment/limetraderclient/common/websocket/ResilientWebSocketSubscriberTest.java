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

import com.ramanbabich.investment.limetraderclient.common.websocket.ResilientWebSocketSubscriber.CloseSubscriber;
import com.ramanbabich.investment.limetraderclient.common.websocket.ResilientWebSocketSubscriber.ErrorSubscriber;
import com.ramanbabich.investment.limetraderclient.common.websocket.ResilientWebSocketSubscriber.PongSubscriber;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Raman Babich
 */
class ResilientWebSocketSubscriberTest {

  private final TextSubscriber textSubscriber = Mockito.mock(TextSubscriber.class);
  private final PongSubscriber pongSubscriber = Mockito.mock(PongSubscriber.class);
  private final CloseSubscriber closeSubscriber = Mockito.mock(CloseSubscriber.class);
  private final ErrorSubscriber errorSubscriber = Mockito.mock(ErrorSubscriber.class);
  private final ResilientWebSocketSubscriber subscriber = new ResilientWebSocketSubscriber(
      textSubscriber, pongSubscriber, closeSubscriber, errorSubscriber);

  @Test
  void shouldListen() {
    String data = "data";
    Assertions.assertFalse(subscriber.listen(false));
    WebSocket webSocket = Mockito.mock(WebSocket.class);
    subscriber.onText(webSocket, data, true);
    Mockito.verifyNoInteractions(textSubscriber);
    Assertions.assertFalse(subscriber.listen(true));
    Assertions.assertTrue(subscriber.listen(true));
    subscriber.onText(webSocket, data, true);
    Mockito.verify(textSubscriber).onText(data);
    Mockito.verify(webSocket, Mockito.times(2)).request(1);
  }

  @Test
  void shouldOnTextAggregateTextAndInvokeTextSubscriber() {
    String data = "data";
    subscriber.listen(true);
    WebSocket webSocket = Mockito.mock(WebSocket.class);
    subscriber.onText(webSocket, data.substring(0, data.length() / 2), false);
    Mockito.verifyNoInteractions(textSubscriber);
    subscriber.onText(webSocket, data.substring(data.length() / 2), true);
    Mockito.verify(textSubscriber).onText(data);
    Mockito.verify(webSocket, Mockito.times(2)).request(1);
  }

  @Test
  void shouldOnPongInvokePongSubscriber() {
    WebSocket webSocket = Mockito.mock(WebSocket.class);
    ByteBuffer message = ByteBuffer.wrap("data".getBytes(StandardCharsets.UTF_8));
    subscriber.onPong(webSocket, message);
    Mockito.verify(pongSubscriber).onPong(webSocket, message);
    Mockito.verify(webSocket).request(1);
  }

  @Test
  void shouldOnCloseInvokeCloseSubscriber() {
    WebSocket webSocket = Mockito.mock(WebSocket.class);
    int statusCode = WebSocket.NORMAL_CLOSURE;
    String reason = "end-of-session";
    subscriber.onClose(webSocket, statusCode, reason);
    Mockito.verify(closeSubscriber).onClose(webSocket, statusCode, reason);
    Mockito.verifyNoInteractions(webSocket);
  }

  @Test
  void shouldOnErrorInvokeErrorSubscriber() {
    WebSocket webSocket = Mockito.mock(WebSocket.class);
    Throwable throwable = new RuntimeException();
    subscriber.onError(webSocket, throwable);
    Mockito.verify(errorSubscriber).onError(webSocket, throwable);
    Mockito.verifyNoInteractions(webSocket);
  }
}
