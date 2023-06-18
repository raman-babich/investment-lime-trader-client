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

import com.ramanbabich.investment.limetraderclient.account.model.AccountSubscriptionAction;

/**
 * @author Raman Babich
 */
public class AccountSubscriptionActionFactory {

  private static final String SUBSCRIBE_ACCOUNT_BALANCE_ACTION = "subscribeBalance";
  private static final String SUBSCRIBE_ACCOUNT_POSITIONS_ACTION = "subscribePositions";
  private static final String SUBSCRIBE_ACCOUNT_ORDER_ACTION = "subscribeOrders";
  private static final String SUBSCRIBE_ACCOUNT_TRADE_ACTION = "subscribeTrades";
  private static final String UNSUBSCRIBE_ACCOUNT_BALANCE_ACTION = "unsubscribeBalance";
  private static final String UNSUBSCRIBE_ACCOUNT_POSITIONS_ACTION = "unsubscribePositions";
  private static final String UNSUBSCRIBE_ACCOUNT_ORDER_ACTION = "unsubscribeOrders";
  private static final String UNSUBSCRIBE_ACCOUNT_TRADE_ACTION = "unsubscribeTrades";


  public AccountSubscriptionAction getAccountBalanceChangedEventAction(
      String accountNumber, boolean subscribe) {
    if (subscribe) {
      return new AccountSubscriptionAction(SUBSCRIBE_ACCOUNT_BALANCE_ACTION, accountNumber);
    }
    return new AccountSubscriptionAction(UNSUBSCRIBE_ACCOUNT_BALANCE_ACTION, accountNumber);
  }

  public AccountSubscriptionAction getAccountPositionsChangedEventAction(
      String accountNumber, boolean subscribe) {
    if (subscribe) {
      return new AccountSubscriptionAction(SUBSCRIBE_ACCOUNT_POSITIONS_ACTION, accountNumber);
    }
    return new AccountSubscriptionAction(UNSUBSCRIBE_ACCOUNT_POSITIONS_ACTION, accountNumber);
  }

  public AccountSubscriptionAction getAccountOrderChangedEventAction(
      String accountNumber, boolean subscribe) {
    if (subscribe) {
      return new AccountSubscriptionAction(SUBSCRIBE_ACCOUNT_ORDER_ACTION, accountNumber);
    }
    return new AccountSubscriptionAction(UNSUBSCRIBE_ACCOUNT_ORDER_ACTION, accountNumber);
  }

  public AccountSubscriptionAction getAccountTradeChangedEventAction(
      String accountNumber, boolean subscribe) {
    if (subscribe) {
      return new AccountSubscriptionAction(SUBSCRIBE_ACCOUNT_TRADE_ACTION, accountNumber);
    }
    return new AccountSubscriptionAction(UNSUBSCRIBE_ACCOUNT_TRADE_ACTION, accountNumber);
  }

}
