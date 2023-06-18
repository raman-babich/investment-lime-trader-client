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
import com.ramanbabich.investment.limetraderclient.account.model.AccountPositionsChangedEvent.Position;
import com.ramanbabich.investment.limetraderclient.account.model.AccountTradeChangedEvent;
import com.ramanbabich.investment.limetraderclient.account.model.DataProcessingFailedException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Raman Babich
 */
class AccountTextSubscriberTest {

  private static final String DATA_TYPE_PROPERTY = "t";
  private static final String BALANCE_CHANGED_DATA_TYPE = "b";
  private static final String POSITIONS_CHANGED_DATA_TYPE = "p";
  private static final String ORDER_CHANGED_DATA_TYPE = "o";
  private static final String TRADE_CHANGED_DATA_TYPE = "t";
  private static final String ERROR_DATA_TYPE = "e";
  private static final String DATA_PROPERTY = "data";

  private final JsonMapper jsonMapper = Mockito.mock(JsonMapper.class);
  private final AccountTextSubscriber textSubscriber = new AccountTextSubscriber(jsonMapper);

  @Test
  void shouldInvokeBalanceChangedEventSubscriber() throws Exception {
    List<AccountBalanceChangedEvent> actual = new ArrayList<>();
    textSubscriber.setSubscriber((AccountBalanceChangedEventSubscriber) actual::add);
    List<AccountBalanceChangedEvent> expected = List.of(
        buildAccountBalanceChangedEventSample(),
        buildAccountBalanceChangedEventSample());
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample(BALANCE_CHANGED_DATA_TYPE);
    Mockito.when(jsonMapper.readValue(
        text,
        TypeFactory.defaultInstance()
            .constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);
    Mockito.when(jsonMapper.convertValue(
        parsedData.get(DATA_PROPERTY),
        TypeFactory.defaultInstance()
            .constructCollectionType(List.class, AccountBalanceChangedEvent.class)))
        .thenReturn(expected);

    textSubscriber.onText(text);

    Assertions.assertEquals(expected, actual);
  }

  private static Map<String, Object> buildParsedDataSample(String type) {
    if (ERROR_DATA_TYPE.equals(type)) {
      return Map.of(DATA_TYPE_PROPERTY, type);
    }
    return Map.of(
        DATA_TYPE_PROPERTY, type,
        DATA_PROPERTY, "data");
  }

  private static AccountBalanceChangedEvent buildAccountBalanceChangedEventSample() {
    return new AccountBalanceChangedEvent(
        "accountNumber",
        "tradePlatform",
        "marginType",
        "restriction",
        "restrictionReason",
        1,
        BigDecimal.ONE,
        BigDecimal.ONE,
        1,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE);
  }

  @Test
  void shouldInvokePositionsChangedEventSubscriber() throws Exception {
    List<AccountPositionsChangedEvent> actual = new ArrayList<>();
    textSubscriber.setSubscriber((AccountPositionsChangedEventSubscriber) actual::add);
    List<AccountPositionsChangedEvent> expected = List.of(
        buildAccountPositionsChangedEventSample(),
        buildAccountPositionsChangedEventSample());
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample(POSITIONS_CHANGED_DATA_TYPE);
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance()
                .constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);
    Mockito.when(jsonMapper.convertValue(
            parsedData.get(DATA_PROPERTY),
            TypeFactory.defaultInstance()
                .constructCollectionType(List.class, AccountPositionsChangedEvent.class)))
        .thenReturn(expected);

    textSubscriber.onText(text);

    Assertions.assertEquals(expected, actual);
  }

  private static AccountPositionsChangedEvent buildAccountPositionsChangedEventSample() {
    return new AccountPositionsChangedEvent(
        "accountNumber",
        List.of(new Position(
            "symbol",
            1,
            BigDecimal.ONE,
            BigDecimal.ONE,
            "securityType")));
  }

  @Test
  void shouldInvokeOrderChangedEventSubscriber() throws Exception {
    List<AccountOrderChangedEvent> actual = new ArrayList<>();
    textSubscriber.setSubscriber((AccountOrderChangedEventSubscriber) actual::add);
    List<AccountOrderChangedEvent> expected = List.of(
        buildAccountOrderChangedEventSample(),
        buildAccountOrderChangedEventSample());
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample(ORDER_CHANGED_DATA_TYPE);
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance()
                .constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);
    Mockito.when(jsonMapper.convertValue(
            parsedData.get(DATA_PROPERTY),
            TypeFactory.defaultInstance()
                .constructCollectionType(List.class, AccountOrderChangedEvent.class)))
        .thenReturn(expected);

    textSubscriber.onText(text);

    Assertions.assertEquals(expected, actual);
  }

  private static AccountOrderChangedEvent buildAccountOrderChangedEventSample() {
    return new AccountOrderChangedEvent(
        "accountNumber",
        "orderId",
        "symbol",
        1,
        BigDecimal.ONE,
        BigDecimal.ONE,
        "timeInForce",
        "orderType",
        "orderSide",
        "exchange",
        "comment",
        1,
        BigDecimal.ONE,
        Instant.ofEpochMilli(0),
        "orderStatus");
  }

  @Test
  void shouldInvokeTradeChangedEventSubscriber() throws Exception {
    List<AccountTradeChangedEvent> actual = new ArrayList<>();
    textSubscriber.setSubscriber((AccountTradeChangedEventSubscriber) actual::add);
    List<AccountTradeChangedEvent> expected = List.of(
        buildAccountTradeChangedEventSample(),
        buildAccountTradeChangedEventSample());
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample(TRADE_CHANGED_DATA_TYPE);
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance()
                .constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);
    Mockito.when(jsonMapper.convertValue(
            parsedData.get(DATA_PROPERTY),
            TypeFactory.defaultInstance()
                .constructCollectionType(List.class, AccountTradeChangedEvent.class)))
        .thenReturn(expected);

    textSubscriber.onText(text);

    Assertions.assertEquals(expected, actual);
  }

  private static AccountTradeChangedEvent buildAccountTradeChangedEventSample() {
    return new AccountTradeChangedEvent(
        "accountNumber",
        "symbol",
        Instant.ofEpochMilli(0),
        1,
        BigDecimal.ONE,
        BigDecimal.ONE,
        "side");
  }

  @Test
  void shouldInvokeErrorEventSubscriber() throws Exception {
    List<AccountErrorEvent> actual = new ArrayList<>();
    textSubscriber.setSubscriber((AccountErrorEventSubscriber) actual::add);
    AccountErrorEvent expected = buildAccountErrorEventSample();
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample(ERROR_DATA_TYPE);
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance()
                .constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);
    Mockito.when(jsonMapper.convertValue(parsedData, AccountErrorEvent.class))
        .thenReturn(expected);

    textSubscriber.onText(text);

    Assertions.assertEquals(List.of(expected), actual);
  }

  private static AccountErrorEvent buildAccountErrorEventSample() {
    return new AccountErrorEvent(
        "code",
        "description");
  }

  @Test
  void shouldSkipIfNoSubscriberSpecified() throws Exception {
    textSubscriber.setSubscriber((AccountBalanceChangedEventSubscriber) null);
    List<AccountBalanceChangedEvent> events = List.of(
        buildAccountBalanceChangedEventSample(),
        buildAccountBalanceChangedEventSample());
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample(BALANCE_CHANGED_DATA_TYPE);
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance()
                .constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);
    Mockito.when(jsonMapper.convertValue(
            parsedData.get(DATA_PROPERTY),
            TypeFactory.defaultInstance()
                .constructCollectionType(List.class, AccountBalanceChangedEvent.class)))
        .thenReturn(events);

    textSubscriber.onText(text);
  }

  @Test
  void shouldSkipIfUnknownTypeIsParsed() throws Exception {
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample("unknown");
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance()
                .constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);

    textSubscriber.onText(text);
  }

  @Test
  void shouldThrowExceptionIfEventTypeIsValidButContentIsInvalid() throws Exception {
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample(BALANCE_CHANGED_DATA_TYPE);
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance()
                .constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);
    Mockito.when(jsonMapper.convertValue(
            parsedData.get(DATA_PROPERTY),
            TypeFactory.defaultInstance()
                .constructCollectionType(List.class, AccountBalanceChangedEvent.class)))
        .thenThrow(IllegalArgumentException.class);

    Assertions.assertThrows(
        DataProcessingFailedException.class,
        () -> textSubscriber.onText(text));
  }

  @Test
  void shouldThrowExceptionIfTextCantBeParsed() throws Exception {
    String text = "text";
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance()
                .constructMapType(Map.class, String.class, Object.class)))
        .thenThrow(JsonProcessingException.class);

    Assertions.assertThrows(
        DataProcessingFailedException.class,
        () -> textSubscriber.onText(text));
  }

}
