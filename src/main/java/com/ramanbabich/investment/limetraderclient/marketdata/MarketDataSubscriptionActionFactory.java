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

import com.ramanbabich.investment.limetraderclient.marketdata.model.MarketDataSubscriptionAction;
import java.util.Set;

/**
 * @author Raman Babich
 */
public class MarketDataSubscriptionActionFactory {

  private static final String SUBSCRIBE_ACTION = "subscribe";
  private static final String UNSUBSCRIBE_ACTION = "unsubscribe";

  public MarketDataSubscriptionAction getAction(Set<String> symbols, boolean subscribe) {
    if (subscribe) {
      return new MarketDataSubscriptionAction(SUBSCRIBE_ACTION, symbols);
    }
    return new MarketDataSubscriptionAction(UNSUBSCRIBE_ACTION, symbols);
  }

}