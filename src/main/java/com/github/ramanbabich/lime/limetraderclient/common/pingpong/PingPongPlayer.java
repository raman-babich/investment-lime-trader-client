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

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Raman Babich
 */
public class PingPongPlayer implements PongSubscriber, Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(PingPongPlayer.class);
  private static final long DEFAULT_PING_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(30);

  private final String id;
  private final PingSender pingSender;
  private final long pingIntervalMillis;
  private final GameOverSubscriber gameOverSubscriber;
  private volatile boolean pongReceived;

  public PingPongPlayer(String id, PingSender pingSender, GameOverSubscriber gameOverSubscriber) {
    this(id, pingSender, null, gameOverSubscriber);
  }

  public PingPongPlayer(String id, PingSender pingSender, Long pingIntervalMillis,
      GameOverSubscriber gameOverSubscriber) {
    this.id = Objects.requireNonNull(id, "Id should be specified.");
    this.pingSender = Objects.requireNonNull(pingSender, "Ping sender should be specified.");
    this.pingIntervalMillis = Objects.requireNonNullElse(
        pingIntervalMillis, DEFAULT_PING_INTERVAL_MILLIS);
    if (this.pingIntervalMillis <= 0) {
      throw new IllegalArgumentException("Ping interval should be greater than zero.");
    }
    this.gameOverSubscriber =
        Objects.requireNonNull(gameOverSubscriber, "Game over subscriber should be specified.");
  }

  @Override
  public void run() {
    boolean gameIsOver = false;
    try {
      while (!gameIsOver && !Thread.currentThread().isInterrupted()) {
        try {
          gameIsOver = isGameOver();
          if (gameIsOver) {
            gameOverSubscriber.onGameOver(this);
          }
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    } catch (Throwable throwable) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Player '{}' realizes that ping pong game is over because of the error. "
            + "Game over subscribers will be notified.", id, throwable);
      } else {
        LOG.info("Player '{}' realizes that ping pong game is over because of the error. "
            + "Game over subscribers will be notified.", id);
      }
      gameOverSubscriber.onGameOver(this);
    }
  }

  private boolean isGameOver() throws InterruptedException {
    pongReceived = false;
    pingSender.send();
    TimeUnit.MILLISECONDS.sleep(pingIntervalMillis);
    return !pongReceived;
  }

  @Override
  public void onPong() {
    pongReceived = true;
  }

  public PingSender getPingSender() {
    return pingSender;
  }
}
