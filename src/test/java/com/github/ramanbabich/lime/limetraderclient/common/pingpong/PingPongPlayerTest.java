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

package com.github.ramanbabich.lime.limetraderclient.common.pingpong;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Raman Babich
 */
class PingPongPlayerTest {

  private final PingSender pingSender = Mockito.mock(PingSender.class);
  private final GameOverSubscriber gameOverSubscriber = Mockito.mock(GameOverSubscriber.class);
  private final PingPongPlayer player =
      new PingPongPlayer("test-player", pingSender, 100L, gameOverSubscriber);

  @Test
  void shouldPlay() throws Exception {
    Thread playerThread = new Thread(player);
    AtomicInteger roundNumber = new AtomicInteger();
    Mockito.doAnswer(invocation -> {
      int round = roundNumber.incrementAndGet();
      if (round < 3){
        player.onPong();
      }
      return null;
    }).when(pingSender).send();

    playerThread.start();

    playerThread.join();
    Mockito.verify(gameOverSubscriber).onGameOver(player);
  }

  @Test
  void shouldGracefullyHandleExceptionDuringPingSending() throws Exception {
    Thread playerThread = new Thread(player);
    Mockito.doThrow(RuntimeException.class).when(pingSender).send();

    playerThread.start();

    playerThread.join();
    Mockito.verify(gameOverSubscriber).onGameOver(player);
  }

}
