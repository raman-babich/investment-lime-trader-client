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

package com.ramanbabich.investment.limetraderclient;

import com.ramanbabich.investment.limetraderclient.account.AccountBalanceChangedEventSubscriber;
import com.ramanbabich.investment.limetraderclient.account.AccountErrorEventSubscriber;
import com.ramanbabich.investment.limetraderclient.account.AccountOrderChangedEventSubscriber;
import com.ramanbabich.investment.limetraderclient.account.AccountPositionsChangedEventSubscriber;
import com.ramanbabich.investment.limetraderclient.account.AccountTradeChangedEventSubscriber;
import com.ramanbabich.investment.limetraderclient.account.model.AccountActiveOrder;
import com.ramanbabich.investment.limetraderclient.account.model.AccountBalance;
import com.ramanbabich.investment.limetraderclient.account.model.AccountPosition;
import com.ramanbabich.investment.limetraderclient.account.model.AccountPositionQuery;
import com.ramanbabich.investment.limetraderclient.account.model.AccountTradeList;
import com.ramanbabich.investment.limetraderclient.account.model.AccountTradeQuery;
import com.ramanbabich.investment.limetraderclient.account.model.AccountTransactionList;
import com.ramanbabich.investment.limetraderclient.account.model.AccountTransactionQuery;
import com.ramanbabich.investment.limetraderclient.auth.model.Credentials;
import com.ramanbabich.investment.limetraderclient.common.model.OrderSide;
import com.ramanbabich.investment.limetraderclient.common.model.OrderTimeInForce;
import com.ramanbabich.investment.limetraderclient.common.model.OrderType;
import com.ramanbabich.investment.limetraderclient.common.model.QuoteHistoryPeriod;
import com.ramanbabich.investment.limetraderclient.marketdata.MarketDataErrorEventSubscriber;
import com.ramanbabich.investment.limetraderclient.marketdata.QuoteChangedEventSubscriber;
import com.ramanbabich.investment.limetraderclient.marketdata.TradeMadeEventSubscriber;
import com.ramanbabich.investment.limetraderclient.marketdata.model.Candle;
import com.ramanbabich.investment.limetraderclient.marketdata.model.Quote;
import com.ramanbabich.investment.limetraderclient.marketdata.model.QuoteHistoryQuery;
import com.ramanbabich.investment.limetraderclient.marketdata.model.TradingSchedule;
import com.ramanbabich.investment.limetraderclient.order.model.Order;
import com.ramanbabich.investment.limetraderclient.order.model.OrderCancellationInfo;
import com.ramanbabich.investment.limetraderclient.order.model.OrderCancellationResult;
import com.ramanbabich.investment.limetraderclient.order.model.OrderPlacementInfo;
import com.ramanbabich.investment.limetraderclient.order.model.OrderPlacementResult;
import com.ramanbabich.investment.limetraderclient.order.model.OrderValidationInfo;
import com.ramanbabich.investment.limetraderclient.order.model.OrderValidationResult;
import com.ramanbabich.investment.limetraderclient.pricing.model.OrderFeeCharge;
import com.ramanbabich.investment.limetraderclient.pricing.model.OrderFeeChargeQuery;
import com.ramanbabich.investment.limetraderclient.security.model.Option;
import com.ramanbabich.investment.limetraderclient.security.model.OptionExpirationQuery;
import com.ramanbabich.investment.limetraderclient.security.model.OptionQuery;
import com.ramanbabich.investment.limetraderclient.security.model.SecurityList;
import com.ramanbabich.investment.limetraderclient.security.model.SecurityQuery;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * @author Raman Babich
 */
class ClientOverviewApplication {

  private final LimeTraderClient client;

  public ClientOverviewApplication(Credentials credentials) {
    this.client = new LimeTraderClient(credentials);
  }

  public static void main(String[] args) throws Exception {
    ClientOverviewApplication application = new ClientOverviewApplication(new Credentials(
        "<your-client-id>",
        "<your-client-secret>",
        "<your-username>",
        "<your-password>"));
    application.reviewAccountApi();
    application.reviewOrderApi();
    application.reviewPricingApi();
    application.reviewSecurityApi();
    application.reviewMarketDataApi();
    application.close();
  }

  private void reviewAccountApi() throws Exception {
    System.out.println("-".repeat(100) + " ACCOUNT API");
    List<AccountBalance> accountBalances = client.getAccountBalances();
    System.out.println(accountBalances);
    String accountNumber = accountBalances.get(0).accountNumber();
    AccountPositionQuery accountPositionQuery = new AccountPositionQuery(accountNumber, null);
    List<AccountPosition> accountPositions = client.getAccountPositions(accountPositionQuery);
    System.out.println(accountPositions);
    AccountTradeQuery accountTradeQuery =
        new AccountTradeQuery(accountNumber, LocalDate.now(), null, null);
    AccountTradeList accountTrades = client.getAccountTrades(accountTradeQuery);
    System.out.println(accountTrades);
    List<AccountActiveOrder> accountActiveOrders = client.getAccountActiveOrders(accountNumber);
    accountActiveOrders.forEach(System.out::println);
    AccountTransactionQuery accountTransactionQuery =
        new AccountTransactionQuery(accountNumber, null, null, null, null);
    AccountTransactionList accountTransactions =
        client.getAccountTransactions(accountTransactionQuery);
    System.out.println(accountTransactions);
    System.out.println("-".repeat(50));
    client.setSubscriber((AccountErrorEventSubscriber) System.out::println);
    client.setSubscriber((AccountBalanceChangedEventSubscriber) System.out::println);
    client.setSubscriber((AccountPositionsChangedEventSubscriber) System.out::println);
    client.setSubscriber((AccountOrderChangedEventSubscriber) System.out::println);
    client.setSubscriber((AccountTradeChangedEventSubscriber) System.out::println);
    client.subscribeForAccountBalanceChangedEvents(accountNumber);
    client.subscribeForAccountPositionsChangedEvents(accountNumber);
    client.subscribeForAccountOrderChangedEvents(accountNumber);
    client.subscribeForAccountTradeChangedEvents(accountNumber);
    Thread.sleep(5 * 1000);
    client.unsubscribeFromAccountBalanceChangedEvents(accountNumber);
    client.unsubscribeFromAccountPositionsChangedEvents(accountNumber);
    client.unsubscribeFromAccountOrderChangedEvents(accountNumber);
    client.unsubscribeFromAccountTradeChangedEvents(accountNumber);
  }

  private void reviewOrderApi() {
    System.out.println("-".repeat(100) + " ORDER API");
    List<AccountBalance> accountBalances = client.getAccountBalances();
    System.out.println(accountBalances);
    String accountNumber = accountBalances.get(0).accountNumber();
    OrderPlacementInfo orderPlacementInfo = new OrderPlacementInfo(
        accountNumber,
        "AMD",
        1,
        BigDecimal.ONE,
        null,
        OrderTimeInForce.DAY,
        OrderType.LIMIT,
        OrderSide.BUY,
        "order placement overview",
        null);
    OrderValidationInfo orderValidationInfo = new OrderValidationInfo(
        orderPlacementInfo.accountNumber(),
        orderPlacementInfo.symbol(),
        orderPlacementInfo.quantity(),
        orderPlacementInfo.price(),
        orderPlacementInfo.stopPrice(),
        orderPlacementInfo.timeInForce(),
        orderPlacementInfo.orderType(),
        orderPlacementInfo.side(),
        orderPlacementInfo.comment(),
        orderPlacementInfo.exchange());
    OrderValidationResult orderValidationResult = client.validateOrder(orderValidationInfo);
    System.out.println(orderValidationResult);
    OrderPlacementResult orderPlacementResult = client.placeOrder(orderPlacementInfo);
    System.out.println(orderPlacementResult);
    List<AccountActiveOrder> accountActiveOrders = client.getAccountActiveOrders(accountNumber);
    accountActiveOrders.forEach(System.out::println);
    Order order = client.getOrder(accountActiveOrders.get(0).id());
    System.out.println(order);
    OrderCancellationInfo orderCancellationInfo =
        new OrderCancellationInfo("order cancellation overview");
    OrderCancellationResult orderCancellationResult =
        client.cancelOrder(order.id(), orderCancellationInfo);
    System.out.println(orderCancellationResult);
    order = client.getOrder(accountActiveOrders.get(0).id());
    System.out.println(order);
  }

  private void reviewPricingApi() {
    System.out.println("-".repeat(100) + " PRICING API");
    List<AccountBalance> accountBalances = client.getAccountBalances();
    System.out.println(accountBalances);
    String accountNumber = accountBalances.get(0).accountNumber();
    OrderFeeChargeQuery orderFeeChargeQuery = new OrderFeeChargeQuery(
        accountNumber,
        "AMD",
        1,
        OrderSide.BUY,
        BigDecimal.ONE);
    List<OrderFeeCharge> orderFeeCharges = client.getOrderFeeCharges(orderFeeChargeQuery);
    System.out.println(orderFeeCharges);
  }

  private void reviewSecurityApi() {
    System.out.println("-".repeat(100) + " SECURITY API");
    SecurityList securities = client.getSecurities(new SecurityQuery("AM", 5));
    System.out.println(securities);
    List<String> optionSeries = client.getOptionSeries("AMD");
    optionSeries.forEach(System.out::println);
    List<LocalDate> optionExpirations = client.getOptionExpirations(
        new OptionExpirationQuery("AMD", optionSeries.get(0)));
    optionExpirations.forEach(System.out::println);
    List<Option> options = client.getOptions(new OptionQuery(
        "AMD",
        LocalDate.of(2025, 12, 19),
        optionSeries.get(0)));
    options.forEach(System.out::println);
  }

  private void reviewMarketDataApi() throws Exception{
    System.out.println("-".repeat(100) + " MARKET DATA API");
    Quote quote = client.getQuote("AMD");
    System.out.println(quote);
    List<Quote> quotes = client.getQuotes(List.of("AMD", "NVDA"));
    quotes.forEach(System.out::println);
    List<Candle> candles = client.getQuoteHistory(new QuoteHistoryQuery(
        "AMD",
        QuoteHistoryPeriod.HOUR,
        Instant.now().minus(Duration.ofDays(1)),
        Instant.now()));
    candles.forEach(System.out::println);
    TradingSchedule tradingSchedule = client.getTradingSchedule();
    System.out.println(tradingSchedule);
    System.out.println("-".repeat(50));
    client.setSubscriber((MarketDataErrorEventSubscriber) System.out::println);
    client.setSubscriber((QuoteChangedEventSubscriber) System.out::println);
    client.setSubscriber((TradeMadeEventSubscriber) System.out::println);
    client.subscribeForMarketDataEvents(Set.of("AMD", "NVDA"));
    Thread.sleep(5 * 1000);
    client.unsubscribeFromMarketDataEvents();
  }

  private void close() {
    client.close();
  }

}
