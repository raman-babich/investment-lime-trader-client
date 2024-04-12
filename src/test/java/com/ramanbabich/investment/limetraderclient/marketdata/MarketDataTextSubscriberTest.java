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

package com.ramanbabich.investment.limetraderclient.marketdata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.ramanbabich.investment.limetraderclient.marketdata.model.DataProcessingFailedException;
import com.ramanbabich.investment.limetraderclient.marketdata.model.MarketDataErrorEvent;
import com.ramanbabich.investment.limetraderclient.marketdata.model.QuoteChangedEvent;
import com.ramanbabich.investment.limetraderclient.marketdata.model.TradeMadeEvent;
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
class MarketDataTextSubscriberTest {

  private static final String DATA_TYPE_PROPERTY = "t";
  private static final String QUOTE_CHANGED_DATA_TYPE = "a";
  private static final String TRADE_MADE_DATA_TYPE = "t";
  private static final String ERROR_DATA_TYPE = "e";

  private final JsonMapper jsonMapper = Mockito.mock(JsonMapper.class);
  private final MarketDataTextSubscriber textSubscriber = new MarketDataTextSubscriber(jsonMapper);

  @Test
  void shouldInvokeQuoteChangedEventSubscriber() throws Exception {
    List<QuoteChangedEvent> actual = new ArrayList<>();
    textSubscriber.setSubscriber((QuoteChangedEventSubscriber) actual::add);
    QuoteChangedEvent expected = buildQuoteChangedEventSample();
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample(QUOTE_CHANGED_DATA_TYPE);
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);
    Mockito.when(jsonMapper.convertValue(parsedData, QuoteChangedEvent.class)).thenReturn(expected);

    textSubscriber.onText(text);

    Assertions.assertEquals(List.of(expected), actual);
  }

  private static Map<String, Object> buildParsedDataSample(String type) {
    return Map.of(DATA_TYPE_PROPERTY, type);
  }

  private static QuoteChangedEvent buildQuoteChangedEventSample() {
    return new QuoteChangedEvent(
        "symbol",
        BigDecimal.ONE,
        1,
        BigDecimal.ONE,
        1,
        BigDecimal.ONE,
        1,
        1,
        Instant.ofEpochMilli(0),
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE);
  }

  @Test
  void shouldInvokeTradeMadeEventSubscriber() throws Exception {
    List<TradeMadeEvent> actual = new ArrayList<>();
    textSubscriber.setSubscriber((TradeMadeEventSubscriber) actual::add);
    TradeMadeEvent expected = buildTradeMadeEventSample();
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample(TRADE_MADE_DATA_TYPE);
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);
    Mockito.when(jsonMapper.convertValue(parsedData, TradeMadeEvent.class)).thenReturn(expected);

    textSubscriber.onText(text);

    Assertions.assertEquals(List.of(expected), actual);
  }

  private static TradeMadeEvent buildTradeMadeEventSample() {
    return new TradeMadeEvent("symbol", 1, "lastMarket", BigDecimal.ONE, Instant.ofEpochMilli(0));
  }

  @Test
  void shouldInvokeErrorEventSubscriber() throws Exception {
    List<MarketDataErrorEvent> actual = new ArrayList<>();
    textSubscriber.setSubscriber((MarketDataErrorEventSubscriber) actual::add);
    MarketDataErrorEvent expected = buildMarketDataErrorEventSample();
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample(ERROR_DATA_TYPE);
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);
    Mockito.when(jsonMapper.convertValue(parsedData, MarketDataErrorEvent.class))
        .thenReturn(expected);

    textSubscriber.onText(text);

    Assertions.assertEquals(List.of(expected), actual);
  }

  private static MarketDataErrorEvent buildMarketDataErrorEventSample() {
    return new MarketDataErrorEvent("code", "description");
  }

  @Test
  void shouldSkipIfNoSubscriberSpecified() throws Exception {
    textSubscriber.setSubscriber((QuoteChangedEventSubscriber) null);
    QuoteChangedEvent event = buildQuoteChangedEventSample();
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample(QUOTE_CHANGED_DATA_TYPE);
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);
    Mockito.when(jsonMapper.convertValue(parsedData, QuoteChangedEvent.class)).thenReturn(event);

    textSubscriber.onText(text);
  }

  @Test
  void shouldSkipIfUnknownTypeIsParsed() throws Exception {
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample("unknown");
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);

    textSubscriber.onText(text);
  }

  @Test
  void shouldThrowExceptionIfEventTypeIsValidButContentIsInvalid() throws Exception {
    String text = "text";
    Map<String, Object> parsedData = buildParsedDataSample(QUOTE_CHANGED_DATA_TYPE);
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class)))
        .thenReturn(parsedData);
    Mockito.when(jsonMapper.convertValue(parsedData, QuoteChangedEvent.class))
        .thenThrow(IllegalArgumentException.class);

    Assertions.assertThrows(DataProcessingFailedException.class, () -> textSubscriber.onText(text));
  }

  @Test
  void shouldThrowExceptionIfTextCantBeParsed() throws Exception {
    String text = "text";
    Mockito.when(jsonMapper.readValue(
            text,
            TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class)))
        .thenThrow(JsonProcessingException.class);

    Assertions.assertThrows(DataProcessingFailedException.class, () -> textSubscriber.onText(text));
  }
}
