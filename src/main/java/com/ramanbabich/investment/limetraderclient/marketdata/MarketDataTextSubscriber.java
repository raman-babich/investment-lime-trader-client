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
import com.ramanbabich.investment.limetraderclient.common.websocket.TextSubscriber;
import com.ramanbabich.investment.limetraderclient.marketdata.model.DataProcessingFailedException;
import com.ramanbabich.investment.limetraderclient.marketdata.model.MarketDataErrorEvent;
import com.ramanbabich.investment.limetraderclient.marketdata.model.QuoteChangedEvent;
import com.ramanbabich.investment.limetraderclient.marketdata.model.TradeMadeEvent;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Raman Babich
 */
public class MarketDataTextSubscriber implements TextSubscriber {

  private static final Logger LOG = LoggerFactory.getLogger(MarketDataTextSubscriber.class);

  private static final String DATA_TYPE_PROPERTY = "t";
  private static final String QUOTE_CHANGED_DATA_TYPE = "a";
  private static final String TRADE_MADE_DATA_TYPE = "t";
  private static final String ERROR_DATA_TYPE = "e";

  private final JsonMapper jsonMapper;

  private TradeMadeEventSubscriber tradeMadeEventSubscriber;
  private QuoteChangedEventSubscriber quoteChangedEventSubscriber;
  private MarketDataErrorEventSubscriber errorEventSubscriber;

  public MarketDataTextSubscriber(JsonMapper jsonMapper) {
    this.jsonMapper = Objects.requireNonNull(jsonMapper, "Json mapper should be specified.");
  }

  @Override
  public void onText(String text) {
    try {
      doOnText(text);
    } catch (IllegalArgumentException | JsonProcessingException ex) {
      throw new DataProcessingFailedException(
          String.format("Error occurred during processing '%s' text.", text), ex);
    }
  }

  private void doOnText(String text) throws JsonProcessingException {
    Map<String, Object> parsedData = jsonMapper.readValue(
        text,
        TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class));
    Object possibleDataType = parsedData.get(DATA_TYPE_PROPERTY);
    if (!(possibleDataType instanceof String dataType)) {
      LOG.warn(
          "The data '{}' with unrecognizable type has arrived. " + "The data will be skipped.",
          text);
      return;
    }
    if (QUOTE_CHANGED_DATA_TYPE.equals(dataType)) {
      QuoteChangedEvent event = jsonMapper.convertValue(parsedData, QuoteChangedEvent.class);
      QuoteChangedEventSubscriber threadSafeSubscriber = quoteChangedEventSubscriber;
      if (threadSafeSubscriber != null) {
        threadSafeSubscriber.onEvent(event);
      } else {
        LOG.warn(
            "Quote changed event subscriber is not set but events '{}' "
                + "have arrived. Events will be skipped.",
            text);
      }
    } else if (TRADE_MADE_DATA_TYPE.equals(dataType)) {
      TradeMadeEvent event = jsonMapper.convertValue(parsedData, TradeMadeEvent.class);
      TradeMadeEventSubscriber threadSafeSubscriber = tradeMadeEventSubscriber;
      if (threadSafeSubscriber != null) {
        threadSafeSubscriber.onEvent(event);
      } else {
        LOG.warn(
            "Trade made event subscriber is not set but events '{}' "
                + "have arrived. Events will be skipped.",
            text);
      }
    } else if (ERROR_DATA_TYPE.equals(dataType)) {
      MarketDataErrorEvent event = jsonMapper.convertValue(parsedData, MarketDataErrorEvent.class);
      MarketDataErrorEventSubscriber threadSafeSubscriber = errorEventSubscriber;
      if (threadSafeSubscriber != null) {
        threadSafeSubscriber.onEvent(event);
      } else {
        LOG.warn(
            "Error event subscriber is not set but events '{}' "
                + "have arrived. Events will be skipped.",
            text);
      }
    } else {
      LOG.warn("The data '{}' with unknown type has arrived. The data will be skipped.", text);
    }
  }

  public void setSubscriber(QuoteChangedEventSubscriber quoteChangedEventSubscriber) {
    this.quoteChangedEventSubscriber = quoteChangedEventSubscriber;
  }

  public void setSubscriber(MarketDataErrorEventSubscriber errorEventSubscriber) {
    this.errorEventSubscriber = errorEventSubscriber;
  }

  public void setSubscriber(TradeMadeEventSubscriber tradeMadeEventSubscriber) {
    this.tradeMadeEventSubscriber = tradeMadeEventSubscriber;
  }
}
