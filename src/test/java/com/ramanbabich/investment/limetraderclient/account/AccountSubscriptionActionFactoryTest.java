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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Raman Babich
 */
class AccountSubscriptionActionFactoryTest {

  private static final String SUBSCRIBE_ACCOUNT_BALANCE_ACTION = "subscribeBalance";
  private static final String SUBSCRIBE_ACCOUNT_POSITIONS_ACTION = "subscribePositions";
  private static final String SUBSCRIBE_ACCOUNT_ORDER_ACTION = "subscribeOrders";
  private static final String SUBSCRIBE_ACCOUNT_TRADE_ACTION = "subscribeTrades";
  private static final String UNSUBSCRIBE_ACCOUNT_BALANCE_ACTION = "unsubscribeBalance";
  private static final String UNSUBSCRIBE_ACCOUNT_POSITIONS_ACTION = "unsubscribePositions";
  private static final String UNSUBSCRIBE_ACCOUNT_ORDER_ACTION = "unsubscribeOrders";
  private static final String UNSUBSCRIBE_ACCOUNT_TRADE_ACTION = "unsubscribeTrades";

  private final AccountSubscriptionActionFactory factory = new AccountSubscriptionActionFactory();

  @Test
  void shouldGetAccountBalanceChangedEventAction() {
    AccountSubscriptionAction expected = new AccountSubscriptionAction(
        SUBSCRIBE_ACCOUNT_BALANCE_ACTION, "accountNumber");
    Assertions.assertEquals(
        expected, factory.getAccountBalanceChangedEventAction(expected.accountNumber(), true));
    expected = new AccountSubscriptionAction(
        UNSUBSCRIBE_ACCOUNT_BALANCE_ACTION, expected.accountNumber());
    Assertions.assertEquals(
        expected, factory.getAccountBalanceChangedEventAction(expected.accountNumber(), false));
  }

  @Test
  void shouldGetAccountPositionsChangedEventAction() {
    AccountSubscriptionAction expected = new AccountSubscriptionAction(
        SUBSCRIBE_ACCOUNT_POSITIONS_ACTION, "accountNumber");
    Assertions.assertEquals(
        expected, factory.getAccountPositionsChangedEventAction(expected.accountNumber(), true));
    expected = new AccountSubscriptionAction(
        UNSUBSCRIBE_ACCOUNT_POSITIONS_ACTION, expected.accountNumber());
    Assertions.assertEquals(
        expected, factory.getAccountPositionsChangedEventAction(expected.accountNumber(), false));
  }

  @Test
  void shouldGetAccountOrderChangedEventAction() {
    AccountSubscriptionAction expected = new AccountSubscriptionAction(
        SUBSCRIBE_ACCOUNT_ORDER_ACTION, "accountNumber");
    Assertions.assertEquals(
        expected, factory.getAccountOrderChangedEventAction(expected.accountNumber(), true));
    expected = new AccountSubscriptionAction(
        UNSUBSCRIBE_ACCOUNT_ORDER_ACTION, expected.accountNumber());
    Assertions.assertEquals(
        expected, factory.getAccountOrderChangedEventAction(expected.accountNumber(), false));
  }

  @Test
  void shouldGetAccountTradeChangedEventAction() {
    AccountSubscriptionAction expected = new AccountSubscriptionAction(
        SUBSCRIBE_ACCOUNT_TRADE_ACTION, "accountNumber");
    Assertions.assertEquals(
        expected, factory.getAccountTradeChangedEventAction(expected.accountNumber(), true));
    expected = new AccountSubscriptionAction(
        UNSUBSCRIBE_ACCOUNT_TRADE_ACTION, expected.accountNumber());
    Assertions.assertEquals(
        expected, factory.getAccountTradeChangedEventAction(expected.accountNumber(), false));
  }

}
