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

package com.ramanbabich.investment.limetraderclient.account;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.ramanbabich.investment.limetraderclient.account.model.AccountBalanceChangedEvent;
import com.ramanbabich.investment.limetraderclient.account.model.AccountErrorEvent;
import com.ramanbabich.investment.limetraderclient.account.model.AccountOrderChangedEvent;
import com.ramanbabich.investment.limetraderclient.account.model.AccountPositionsChangedEvent;
import com.ramanbabich.investment.limetraderclient.account.model.AccountTradeChangedEvent;
import com.ramanbabich.investment.limetraderclient.account.model.DataProcessingFailedException;
import com.ramanbabich.investment.limetraderclient.common.websocket.TextSubscriber;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Raman Babich
 */
public class AccountTextSubscriber implements TextSubscriber {

  private static final Logger LOG = LoggerFactory.getLogger(AccountTextSubscriber.class);

  private static final String DATA_TYPE_PROPERTY = "t";
  private static final String BALANCE_CHANGED_DATA_TYPE = "b";
  private static final String POSITIONS_CHANGED_DATA_TYPE = "p";
  private static final String ORDER_CHANGED_DATA_TYPE = "o";
  private static final String TRADE_CHANGED_DATA_TYPE = "t";
  private static final String ERROR_DATA_TYPE = "e";
  private static final String DATA_PROPERTY = "data";

  private final JsonMapper jsonMapper;
  private AccountBalanceChangedEventSubscriber balanceChangedEventSubscriber;
  private AccountPositionsChangedEventSubscriber positionsChangedEventSubscriber;
  private AccountOrderChangedEventSubscriber orderChangedEventSubscriber;
  private AccountTradeChangedEventSubscriber tradeChangedEventSubscriber;
  private AccountErrorEventSubscriber errorEventSubscriber;

  public AccountTextSubscriber(JsonMapper jsonMapper) {
    this.jsonMapper = Objects.requireNonNull(jsonMapper, "Json mapper should be specified.");
  }

  @Override
  public void onText(String text) {
    try {
      doOnText(text);
    } catch (IllegalArgumentException | JsonProcessingException ex) {
      throw new DataProcessingFailedException(String.format(
          "Error occurred during processing '%s' text.", text),
          ex);
    }
  }

  private void doOnText(String text) throws JsonProcessingException {
    Map<String, Object> parsedData = jsonMapper.readValue(text,
        TypeFactory.defaultInstance()
            .constructMapType(Map.class, String.class, Object.class));
    Object possibleDataType = parsedData.get(DATA_TYPE_PROPERTY);
    if (!(possibleDataType instanceof String dataType)) {
      LOG.warn("The data '{}' with unrecognizable type has arrived. "
          + "The data will be skipped.", text);
      return;
    }
    if (BALANCE_CHANGED_DATA_TYPE.equals(dataType)) {
      List<AccountBalanceChangedEvent> events = jsonMapper.convertValue(
          parsedData.get(DATA_PROPERTY), TypeFactory.defaultInstance()
              .constructCollectionType(List.class, AccountBalanceChangedEvent.class));
      AccountBalanceChangedEventSubscriber threadSafeSubscriber = balanceChangedEventSubscriber;
      if (threadSafeSubscriber != null) {
        events.forEach(threadSafeSubscriber::onEvent);
      } else {
        LOG.warn("Balance changed event subscriber is not set but events '{}' "
                + "have arrived. Events will be skipped.",
            text);
      }
    } else if (POSITIONS_CHANGED_DATA_TYPE.equals(dataType)) {
      List<AccountPositionsChangedEvent> events = jsonMapper.convertValue(
          parsedData.get(DATA_PROPERTY), TypeFactory.defaultInstance()
              .constructCollectionType(List.class, AccountPositionsChangedEvent.class));
      AccountPositionsChangedEventSubscriber threadSafeSubscriber = positionsChangedEventSubscriber;
      if (threadSafeSubscriber != null) {
        events.forEach(threadSafeSubscriber::onEvent);
      } else {
        LOG.warn("Positions changed event subscriber is not set but events '{}' "
                + "have arrived. Events will be skipped.",
            text);
      }
    } else if (ORDER_CHANGED_DATA_TYPE.equals(dataType)) {
      List<AccountOrderChangedEvent> events = jsonMapper.convertValue(
          parsedData.get(DATA_PROPERTY), TypeFactory.defaultInstance()
              .constructCollectionType(List.class, AccountOrderChangedEvent.class));
      AccountOrderChangedEventSubscriber threadSafeSubscriber = orderChangedEventSubscriber;
      if (threadSafeSubscriber != null) {
        events.forEach(threadSafeSubscriber::onEvent);
      } else {
        LOG.warn("Order changed event subscriber is not set but events '{}' "
                + "have arrived. Events will be skipped.",
            text);
      }
    } else if (TRADE_CHANGED_DATA_TYPE.equals(dataType)) {
      List<AccountTradeChangedEvent> events = jsonMapper.convertValue(
          parsedData.get(DATA_PROPERTY), TypeFactory.defaultInstance()
              .constructCollectionType(List.class, AccountTradeChangedEvent.class));
      AccountTradeChangedEventSubscriber threadSafeSubscriber = tradeChangedEventSubscriber;
      if (threadSafeSubscriber != null) {
        events.forEach(threadSafeSubscriber::onEvent);
      } else {
        LOG.warn("Trade changed event subscriber is not set but events '{}' "
                + "have arrived. Events will be skipped.",
            text);
      }
    } else if (ERROR_DATA_TYPE.equals(dataType)) {
      AccountErrorEvent event = jsonMapper.convertValue(parsedData, AccountErrorEvent.class);
      AccountErrorEventSubscriber threadSafeSubscriber = errorEventSubscriber;
      if (threadSafeSubscriber != null) {
        threadSafeSubscriber.onEvent(event);
      } else {
        LOG.warn("Error event subscriber is not set but events '{}' "
                + "have arrived. Events will be skipped.",
            text);
      }
    } else {
      LOG.warn("The data '{}' with unknown type has arrived. The data will be skipped.", text);
    }
  }

  public void setSubscriber(AccountBalanceChangedEventSubscriber balanceChangedEventSubscriber) {
    this.balanceChangedEventSubscriber = balanceChangedEventSubscriber;
  }

  public void setSubscriber(
      AccountPositionsChangedEventSubscriber positionsChangedEventSubscriber) {
    this.positionsChangedEventSubscriber = positionsChangedEventSubscriber;
  }

  public void setSubscriber(AccountOrderChangedEventSubscriber orderChangedEventSubscriber) {
    this.orderChangedEventSubscriber = orderChangedEventSubscriber;
  }

  public void setSubscriber(AccountTradeChangedEventSubscriber tradeChangedEventSubscriber) {
    this.tradeChangedEventSubscriber = tradeChangedEventSubscriber;
  }

  public void setSubscriber(AccountErrorEventSubscriber accountErrorEventSubscriber) {
    this.errorEventSubscriber = accountErrorEventSubscriber;
  }

}
