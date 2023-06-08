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

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Raman Babich
 */
class WebSocketPingSenderTest {

  private static final String MESSAGE = "ping-pong";

  private final WebSocket webSocket = Mockito.mock(WebSocket.class);
  private final WebSocketPingSender sender = new WebSocketPingSender("test-sender", webSocket);

  @Test
  @SuppressWarnings("unchecked")
  void shouldSend() throws Exception {
    CompletableFuture<WebSocket> webSocketFuture =
        (CompletableFuture<WebSocket>) Mockito.mock(CompletableFuture.class);
    Mockito.when(webSocket.sendPing(ByteBuffer.wrap(MESSAGE.getBytes(StandardCharsets.UTF_8))))
            .thenReturn(webSocketFuture);

    sender.send();

    Mockito.verify(webSocketFuture).get(10, TimeUnit.SECONDS);
  }

  @Test
  void shouldThrowExceptionIfPingSendingFailed() {
    Mockito.when(webSocket.sendPing(ByteBuffer.wrap(MESSAGE.getBytes(StandardCharsets.UTF_8))))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException()));

    Assertions.assertThrows(PingSendingFailedException.class, sender::send);
  }

}
