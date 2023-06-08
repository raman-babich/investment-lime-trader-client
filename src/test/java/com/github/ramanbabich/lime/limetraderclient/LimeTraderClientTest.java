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

package com.github.ramanbabich.lime.limetraderclient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.json.JsonMapper.Builder;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.ramanbabich.lime.limetraderclient.account.AccountBalanceChangedEventSubscriber;
import com.github.ramanbabich.lime.limetraderclient.account.AccountErrorEventSubscriber;
import com.github.ramanbabich.lime.limetraderclient.account.AccountOrderChangedEventSubscriber;
import com.github.ramanbabich.lime.limetraderclient.account.AccountPositionsChangedEventSubscriber;
import com.github.ramanbabich.lime.limetraderclient.account.AccountSubscriptionActionFactory;
import com.github.ramanbabich.lime.limetraderclient.account.AccountTextSubscriber;
import com.github.ramanbabich.lime.limetraderclient.account.AccountTradeChangedEventSubscriber;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountActiveOrder;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountBalance;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountPosition;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountPositionQuery;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountSubscriptionAction;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountTradeList;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountTradeList.Trade;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountTradeQuery;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountTransactionList;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountTransactionList.Transaction;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountTransactionList.Transaction.Asset;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountTransactionList.Transaction.Cash;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountTransactionList.Transaction.Fee;
import com.github.ramanbabich.lime.limetraderclient.account.model.AccountTransactionQuery;
import com.github.ramanbabich.lime.limetraderclient.auth.model.Authentication;
import com.github.ramanbabich.lime.limetraderclient.auth.model.Credentials;
import com.github.ramanbabich.lime.limetraderclient.common.model.ClientConfig;
import com.github.ramanbabich.lime.limetraderclient.common.model.IllegalLimeTraderClientStateException;
import com.github.ramanbabich.lime.limetraderclient.common.websocket.ResilientWebSocket;
import com.github.ramanbabich.lime.limetraderclient.marketdata.MarketDataErrorEventSubscriber;
import com.github.ramanbabich.lime.limetraderclient.marketdata.MarketDataSubscriptionActionFactory;
import com.github.ramanbabich.lime.limetraderclient.marketdata.MarketDataTextSubscriber;
import com.github.ramanbabich.lime.limetraderclient.marketdata.QuoteChangedEventSubscriber;
import com.github.ramanbabich.lime.limetraderclient.marketdata.TradeMadeEventSubscriber;
import com.github.ramanbabich.lime.limetraderclient.marketdata.model.Candle;
import com.github.ramanbabich.lime.limetraderclient.marketdata.model.MarketDataSubscriptionAction;
import com.github.ramanbabich.lime.limetraderclient.marketdata.model.Quote;
import com.github.ramanbabich.lime.limetraderclient.marketdata.model.QuoteHistoryQuery;
import com.github.ramanbabich.lime.limetraderclient.marketdata.model.TradingSchedule;
import com.github.ramanbabich.lime.limetraderclient.order.model.Order;
import com.github.ramanbabich.lime.limetraderclient.order.model.OrderCancellationInfo;
import com.github.ramanbabich.lime.limetraderclient.order.model.OrderCancellationResult;
import com.github.ramanbabich.lime.limetraderclient.order.model.OrderPlacementInfo;
import com.github.ramanbabich.lime.limetraderclient.order.model.OrderPlacementResult;
import com.github.ramanbabich.lime.limetraderclient.order.model.OrderValidationInfo;
import com.github.ramanbabich.lime.limetraderclient.order.model.OrderValidationResult;
import com.github.ramanbabich.lime.limetraderclient.pricing.model.OrderFeeCharge;
import com.github.ramanbabich.lime.limetraderclient.pricing.model.OrderFeeChargeQuery;
import com.github.ramanbabich.lime.limetraderclient.security.model.Option;
import com.github.ramanbabich.lime.limetraderclient.security.model.OptionExpirationQuery;
import com.github.ramanbabich.lime.limetraderclient.security.model.OptionQuery;
import com.github.ramanbabich.lime.limetraderclient.security.model.SecurityList;
import com.github.ramanbabich.lime.limetraderclient.security.model.SecurityList.Security;
import com.github.ramanbabich.lime.limetraderclient.security.model.SecurityQuery;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Raman Babich
 */
class LimeTraderClientTest {

  private static final String DEFAULT_BASE_HTTP_AUTH_URL = "https://auth.lime.co";
  private static final String DEFAULT_BASE_HTTP_API_URL = "https://api.lime.co";
  private static final String DEFAULT_BASE_WS_API_URL = "wss://api.lime.co";
  private static final String AUTHORIZATION_HEADER = "authorization";
  private static final String AUTHORIZATION_PREFIX = "Bearer ";

  private static final String ACCOUNT_BALANCE_CHANGED_EVENT_STATE_PROPERTY_PATTERN =
      "balanceChanged.%s";
  private static final String ACCOUNT_POSITIONS_CHANGED_EVENT_STATE_PROPERTY_PATTERN =
      "positionsChanged.%s";
  private static final String ACCOUNT_ORDER_CHANGED_EVENT_STATE_PROPERTY_PATTERN =
      "orderChanged.%s";
  private static final String ACCOUNT_TRADE_CHANGED_EVENT_STATE_PROPERTY_PATTERN =
      "tradeChanged.%s";
  private static final String MARKET_DATA_EVENT_STATE_PROPERTY = "marketDataSymbols";

  private static final Instant DEFAULT_NOW = Instant.ofEpochMilli(0);

  private String baseHttpAuthUrl;
  private String tokenUrl;
  private String baseHttpApiUrl;
  private String accountsUrl;
  private String accountPositionsUrlPattern;
  private String accountTradesUrlPattern;
  private String accountActiveOrderUrlPattern;
  private String accountTransactionsUrlPattern;
  private String ordersUrl;
  private String orderUrlPattern;
  private String placeOrderUrl;
  private String validateOrderUrl;
  private String cancelOrderUrlPattern;
  private String pricingUrl;
  private String pricingFeesUrl;
  private String marketDataUrl;
  private String quoteUrl;
  private String quotesUrl;
  private String quoteHistoryUrl;
  private String tradingScheduleUrl;
  private String securitiesUrl;
  private String optionsUrlPattern;
  private String optionSeriesUrlPattern;
  private String optionExpirationUrlPattern;

  private String baseWsApiUrl;
  private String accountWsUrl;
  private String marketDataWsUrl;

  private final Credentials credentials =
      new Credentials("clientId", "clientSecret", "username", "password");

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetAccountBalances() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(accountsUrl))
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
            .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    List<AccountBalance> expected = List.of(buildAccountBalanceSample());
    Mockito.when(clientWithMocks.jsonMapper.readValue(
        responseBody,
        TypeFactory.defaultInstance()
            .constructCollectionType(List.class, AccountBalance.class)))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getAccountBalances());
  }

  private AccountBalance buildAccountBalanceSample() {
    return new AccountBalance(
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
  @SuppressWarnings("unchecked")
  void shouldRefreshAuth() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(accountsUrl))
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    Mockito.when(clientWithMocks.jsonMapper.readValue(
        responseBody,
        TypeFactory.defaultInstance()
            .constructCollectionType(List.class, AccountBalance.class)))
        .thenReturn(List.of());

    clientWithMocks.client.getAccountBalances();
    Mockito.when(clientWithMocks.instantSource.instant())
        .thenReturn(DEFAULT_NOW.plus(auth.expiresIn() + 1, ChronoUnit.SECONDS));
    clientWithMocks.client.getAccountBalances();

    verifyAuthenticateTimes(2, clientWithMocks);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetAccountPositions() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    AccountPositionQuery query = new AccountPositionQuery("accountNumber", LocalDate.now());
    Map<String, String> requestParams = new TreeMap<>();
    requestParams.put("date", DateTimeFormatter.ISO_DATE.format(query.date()));
    URI uri = buildUri(String.format(
            accountPositionsUrlPattern,
            URLEncoder.encode(query.accountNumber(), StandardCharsets.UTF_8)),
        requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
            .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    List<AccountPosition> expected = List.of(
        new AccountPosition(
            "accountNumber",
            1,
            BigDecimal.ONE,
            BigDecimal.ONE,
            "securityType"));
    Mockito.when(clientWithMocks.jsonMapper.readValue(
        responseBody,
        TypeFactory.defaultInstance()
            .constructCollectionType(List.class, AccountPosition.class)))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getAccountPositions(query));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetAccountTrades() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    AccountTradeQuery query = new AccountTradeQuery(
        "accountNumber",
        LocalDate.now(),
        1,
        0);
    Map<String, String> requestParams = new TreeMap<>();
    requestParams.put("limit", query.limit().toString());
    requestParams.put("skip", query.skip().toString());
    URI uri = buildUri(String.format(
            accountTradesUrlPattern,
            URLEncoder.encode(query.accountNumber(), StandardCharsets.UTF_8),
            URLEncoder.encode(
                DateTimeFormatter.ISO_DATE.format(query.date()),
                StandardCharsets.UTF_8)),
        requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    AccountTradeList expected = new AccountTradeList(
        List.of(
            new Trade(
                "symbol",
                Instant.ofEpochMilli(0),
                1,
                BigDecimal.ONE,
                BigDecimal.ONE,
                "side")),
        1);
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            AccountTradeList.class))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getAccountTrades(query));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetAccountActiveOrders() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    String accountNumber = "accountNumber";
    URI uri = URI.create(String.format(
        accountActiveOrderUrlPattern,
        URLEncoder.encode(accountNumber, StandardCharsets.UTF_8)));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    List<AccountActiveOrder> expected = List.of(buildAccountActiveOrderSample());
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            TypeFactory.defaultInstance()
                .constructCollectionType(List.class, AccountActiveOrder.class)))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getAccountActiveOrders(accountNumber));
  }

  private AccountActiveOrder buildAccountActiveOrderSample() {
    return new AccountActiveOrder(
        "id",
        "accountNumber",
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
        "orderStatus");
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetAccountTransactions() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    AccountTransactionQuery query = new AccountTransactionQuery(
        "accountNumber",
        LocalDate.now().minus(Period.ofDays(1)),
        LocalDate.now(),
        1,
        0);
    Map<String, String> requestParams = new TreeMap<>();
    requestParams.put("start_date", DateTimeFormatter.ISO_DATE.format(query.startDate()));
    requestParams.put("end_date", DateTimeFormatter.ISO_DATE.format(query.endDate()));
    requestParams.put("limit", query.limit().toString());
    requestParams.put("skip", query.skip().toString());
    URI uri = buildUri(String.format(
            accountTransactionsUrlPattern,
            URLEncoder.encode(query.accountNumber(), StandardCharsets.UTF_8)),
        requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    AccountTransactionList expected = new AccountTransactionList(
        List.of(buildAccountTransactionSample()), 1);
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            AccountTransactionList.class))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getAccountTransactions(query));
  }

  private Transaction buildAccountTransactionSample() {
    return new Transaction(
        "id",
        "type",
        "description",
        LocalDate.now(),
        new Asset(
            "symbol",
            "symbolDescription",
            1,
            BigDecimal.ONE),
        new Cash(
            BigDecimal.ONE,
            BigDecimal.ONE),
        List.of(
            new Fee("name", BigDecimal.ONE)),
        "status");
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldPlaceOrder() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    OrderPlacementInfo info = new OrderPlacementInfo(
        "accountNumber",
        "symbol",
        1,
        BigDecimal.ONE,
        BigDecimal.ONE,
        "timeInForce",
        "orderType",
        "side",
        "comment",
        "exchange");
    String requestBody = "requestBody";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(info)).thenReturn(requestBody);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(placeOrderUrl))
        .header("accept", "application/json")
        .header("content-type", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .POST(BodyPublishers.ofString(requestBody))
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    OrderPlacementResult expected = new OrderPlacementResult(true, "data");
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            OrderPlacementResult.class))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.placeOrder(info));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldValidateOrder() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    OrderValidationInfo info = new OrderValidationInfo(
        "accountNumber",
        "symbol",
        1,
        BigDecimal.ONE,
        BigDecimal.ONE,
        "timeInForce",
        "orderType",
        "side",
        "comment",
        "exchange");
    String requestBody = "requestBody";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(info)).thenReturn(requestBody);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(validateOrderUrl))
        .header("accept", "application/json")
        .header("content-type", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .POST(BodyPublishers.ofString(requestBody))
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    OrderValidationResult expected =
        new OrderValidationResult(true, "validationMessage");
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            OrderValidationResult.class))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.validateOrder(info));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetOrder() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    String orderId = "orderId";
    URI uri = URI.create(String.format(
        orderUrlPattern,
        URLEncoder.encode(orderId, StandardCharsets.UTF_8)));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    Order expected = buildOrderSample();
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            Order.class))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getOrder(orderId));
  }

  private Order buildOrderSample() {
    return new Order(
        "id",
        "accountNumber",
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
        "orderStatus");
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldCancelOrder() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    String orderId = "orderId";
    OrderCancellationInfo info = new OrderCancellationInfo("message");
    String requestBody = "requestBody";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(info)).thenReturn(requestBody);
    URI uri = URI.create(String.format(
        cancelOrderUrlPattern,
        URLEncoder.encode(orderId, StandardCharsets.UTF_8)));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header("content-type", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .POST(BodyPublishers.ofString(requestBody))
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    OrderCancellationResult expected = new OrderCancellationResult(true, "data");
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            OrderCancellationResult.class))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.cancelOrder(orderId, info));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetOrderFeeCharges() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    OrderFeeChargeQuery query = new OrderFeeChargeQuery(
        "accountNumber",
        "symbol",
        1,
        "side",
        BigDecimal.ONE);
    String requestBody = "requestBody";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(query)).thenReturn(requestBody);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(pricingFeesUrl))
        .header("accept", "application/json")
        .header("content-type", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .POST(BodyPublishers.ofString(requestBody))
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    List<OrderFeeCharge> expected = List.of(new OrderFeeCharge(BigDecimal.ONE, "type"));
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            TypeFactory.defaultInstance()
                .constructCollectionType(List.class, OrderFeeCharge.class)))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getOrderFeeCharges(query));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetQuote() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    String symbol = "symbol";
    URI uri = buildUri(quoteUrl, Map.of("symbol", symbol));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    Quote expected = buildQuoteSample();
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            Quote.class))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getQuote(symbol));
  }

  private Quote buildQuoteSample() {
    return new Quote(
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
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetQuotes() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    List<String> symbols = List.of("symbol");
    String requestBody = "requestBody";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(symbols)).thenReturn(requestBody);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(quotesUrl))
        .header("accept", "application/json")
        .header("content-type", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .POST(BodyPublishers.ofString(requestBody))
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    List<Quote> expected = List.of(buildQuoteSample());
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            TypeFactory.defaultInstance()
                .constructCollectionType(List.class, Quote.class)))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getQuotes(symbols));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetQuoteHistory() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    QuoteHistoryQuery query = new QuoteHistoryQuery(
        "symbol",
        "period",
        Instant.ofEpochMilli(0).minus(Period.ofDays(1)),
        Instant.ofEpochMilli(0));
    Map<String, String> requestParams = new TreeMap<>();
    requestParams.put("symbol", query.symbol());
    requestParams.put("period", query.period());
    requestParams.put("from", Long.toString(query.from().getEpochSecond()));
    requestParams.put("to", Long.toString(query.to().getEpochSecond()));
    URI uri = buildUri(quoteHistoryUrl, requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    List<Candle> expected = List.of(new Candle(
        Instant.ofEpochMilli(0),
        "period",
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        1));
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            TypeFactory.defaultInstance()
                .constructCollectionType(List.class, Candle.class)))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getQuoteHistory(query));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetTradingSchedule() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(tradingScheduleUrl))
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    TradingSchedule expected = new TradingSchedule("session");
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            TradingSchedule.class))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getTradingSchedule());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetSecurities() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    SecurityQuery query = new SecurityQuery("query", 1);
    Map<String, String> requestParams = new TreeMap<>();
    requestParams.put("query", query.query());
    requestParams.put("limit", query.limit().toString());
    URI uri = buildUri(securitiesUrl, requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    SecurityList expected = new SecurityList(List.of(
        new Security("symbol", "description")),
        1);
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            SecurityList.class))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getSecurities(query));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetOptionSeries() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    String symbol = "symbol";
    URI uri = URI.create(String.format(
        optionSeriesUrlPattern,
        URLEncoder.encode(symbol, StandardCharsets.UTF_8)));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    List<String> expected = List.of("series");
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            TypeFactory.defaultInstance()
                .constructCollectionType(List.class, String.class)))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getOptionSeries(symbol));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetOptionExpirations() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    OptionExpirationQuery query = new OptionExpirationQuery("symbol", "series");
    Map<String, String> requestParams = new TreeMap<>();
    requestParams.put("series", query.series());
    URI uri = buildUri(String.format(
            optionExpirationUrlPattern,
            URLEncoder.encode(query.symbol(), StandardCharsets.UTF_8)),
        requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    List<LocalDate> expected = List.of(LocalDate.now());
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            TypeFactory.defaultInstance()
                .constructCollectionType(List.class, LocalDate.class)))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getOptionExpirations(query));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGetOptions() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Authentication auth = mockSuccessfulAuth(clientWithMocks);
    OptionQuery query = new OptionQuery("symbol", LocalDate.now(), "series");
    Map<String, String> requestParams = new TreeMap<>();
    requestParams.put("expiration", DateTimeFormatter.ISO_DATE.format(query.expiration()));
    requestParams.put("series", query.series());
    URI uri = buildUri(String.format(
            optionsUrlPattern,
            URLEncoder.encode(query.symbol(), StandardCharsets.UTF_8)),
        requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    List<Option> expected = List.of(new Option("symbol", "type", BigDecimal.ONE));
    Mockito.when(clientWithMocks.jsonMapper.readValue(
            responseBody,
            TypeFactory.defaultInstance()
                .constructCollectionType(List.class, Option.class)))
        .thenReturn(expected);

    Assertions.assertEquals(expected, clientWithMocks.client.getOptions(query));
  }

  @Test
  void shouldSetAccountBalanceChangedEventSubscriber() {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    AccountBalanceChangedEventSubscriber eventSubscriber =
        Mockito.mock(AccountBalanceChangedEventSubscriber.class);
    AccountTextSubscriber textSubscriber = Mockito.mock(AccountTextSubscriber.class);
    Mockito.when(clientWithMocks.accountWebSocket.getTextSubscriber(AccountTextSubscriber.class))
            .thenReturn(textSubscriber);

    clientWithMocks.client.setSubscriber(eventSubscriber);

    Mockito.verify(textSubscriber).setSubscriber(eventSubscriber);
  }

  @Test
  void shouldSetAccountPositionsChangedEventSubscriber() {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    AccountPositionsChangedEventSubscriber eventSubscriber =
        Mockito.mock(AccountPositionsChangedEventSubscriber.class);
    AccountTextSubscriber textSubscriber = Mockito.mock(AccountTextSubscriber.class);
    Mockito.when(clientWithMocks.accountWebSocket.getTextSubscriber(AccountTextSubscriber.class))
        .thenReturn(textSubscriber);

    clientWithMocks.client.setSubscriber(eventSubscriber);

    Mockito.verify(textSubscriber).setSubscriber(eventSubscriber);
  }

  @Test
  void shouldSetAccountOrderChangedEventSubscriber() {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    AccountOrderChangedEventSubscriber eventSubscriber =
        Mockito.mock(AccountOrderChangedEventSubscriber.class);
    AccountTextSubscriber textSubscriber = Mockito.mock(AccountTextSubscriber.class);
    Mockito.when(clientWithMocks.accountWebSocket.getTextSubscriber(AccountTextSubscriber.class))
        .thenReturn(textSubscriber);

    clientWithMocks.client.setSubscriber(eventSubscriber);

    Mockito.verify(textSubscriber).setSubscriber(eventSubscriber);
  }

  @Test
  void shouldSetAccountTradeChangedEventSubscriber() {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    AccountTradeChangedEventSubscriber eventSubscriber =
        Mockito.mock(AccountTradeChangedEventSubscriber.class);
    AccountTextSubscriber textSubscriber = Mockito.mock(AccountTextSubscriber.class);
    Mockito.when(clientWithMocks.accountWebSocket.getTextSubscriber(AccountTextSubscriber.class))
        .thenReturn(textSubscriber);

    clientWithMocks.client.setSubscriber(eventSubscriber);

    Mockito.verify(textSubscriber).setSubscriber(eventSubscriber);
  }

  @Test
  void shouldSetAccountErrorEventSubscriber() {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    AccountErrorEventSubscriber eventSubscriber =
        Mockito.mock(AccountErrorEventSubscriber.class);
    AccountTextSubscriber textSubscriber = Mockito.mock(AccountTextSubscriber.class);
    Mockito.when(clientWithMocks.accountWebSocket.getTextSubscriber(AccountTextSubscriber.class))
        .thenReturn(textSubscriber);

    clientWithMocks.client.setSubscriber(eventSubscriber);

    Mockito.verify(textSubscriber).setSubscriber(eventSubscriber);
  }

  @Test
  void shouldSetTradeMadeEventSubscriber() {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    TradeMadeEventSubscriber eventSubscriber =
        Mockito.mock(TradeMadeEventSubscriber.class);
    MarketDataTextSubscriber textSubscriber = Mockito.mock(MarketDataTextSubscriber.class);
    Mockito.when(clientWithMocks.marketDataWebSocket.getTextSubscriber(MarketDataTextSubscriber.class))
        .thenReturn(textSubscriber);

    clientWithMocks.client.setSubscriber(eventSubscriber);

    Mockito.verify(textSubscriber).setSubscriber(eventSubscriber);
  }

  @Test
  void shouldSetQuoteChangedEventSubscriber() {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    QuoteChangedEventSubscriber eventSubscriber =
        Mockito.mock(QuoteChangedEventSubscriber.class);
    MarketDataTextSubscriber textSubscriber = Mockito.mock(MarketDataTextSubscriber.class);
    Mockito.when(clientWithMocks.marketDataWebSocket.getTextSubscriber(MarketDataTextSubscriber.class))
        .thenReturn(textSubscriber);

    clientWithMocks.client.setSubscriber(eventSubscriber);

    Mockito.verify(textSubscriber).setSubscriber(eventSubscriber);
  }

  @Test
  void shouldSetMarketDataErrorEventSubscriber() {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    MarketDataErrorEventSubscriber eventSubscriber =
        Mockito.mock(MarketDataErrorEventSubscriber.class);
    MarketDataTextSubscriber textSubscriber = Mockito.mock(MarketDataTextSubscriber.class);
    Mockito.when(clientWithMocks.marketDataWebSocket.getTextSubscriber(MarketDataTextSubscriber.class))
        .thenReturn(textSubscriber);

    clientWithMocks.client.setSubscriber(eventSubscriber);

    Mockito.verify(textSubscriber).setSubscriber(eventSubscriber);
  }

  @Test
  void shouldSubscribeForAccountBalanceChangedEvents() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    String accountNumber = "accountNumber";
    AccountSubscriptionAction action =
        new AccountSubscriptionAction("action", accountNumber);
    Mockito.when(clientWithMocks.accountSubscriptionActionFactory
        .getAccountBalanceChangedEventAction(accountNumber, true))
            .thenReturn(action);
    String actionString = "action";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(action))
            .thenReturn(actionString);

    clientWithMocks.client.subscribeForAccountBalanceChangedEvents(accountNumber);

    Mockito.verify(clientWithMocks.accountWebSocket)
        .insertToStateIfAbsent(String.format(
                ACCOUNT_BALANCE_CHANGED_EVENT_STATE_PROPERTY_PATTERN,
                accountNumber),
            actionString);
  }

  @Test
  void shouldUnsubscribeFromAccountBalanceChangedEvents() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    String accountNumber = "accountNumber";
    AccountSubscriptionAction action =
        new AccountSubscriptionAction("action", accountNumber);
    Mockito.when(clientWithMocks.accountSubscriptionActionFactory
            .getAccountBalanceChangedEventAction(accountNumber, false))
        .thenReturn(action);
    String actionString = "action";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(action))
        .thenReturn(actionString);

    clientWithMocks.client.unsubscribeFromAccountBalanceChangedEvents(accountNumber);

    Mockito.verify(clientWithMocks.accountWebSocket)
        .removeFromStateWith(String.format(
                ACCOUNT_BALANCE_CHANGED_EVENT_STATE_PROPERTY_PATTERN,
                accountNumber),
            actionString);
  }

  @Test
  void shouldSubscribeForAccountPositionsChangedEvents() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    String accountNumber = "accountNumber";
    AccountSubscriptionAction action =
        new AccountSubscriptionAction("action", accountNumber);
    Mockito.when(clientWithMocks.accountSubscriptionActionFactory
            .getAccountPositionsChangedEventAction(accountNumber, true))
        .thenReturn(action);
    String actionString = "action";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(action))
        .thenReturn(actionString);

    clientWithMocks.client.subscribeForAccountPositionsChangedEvents(accountNumber);

    Mockito.verify(clientWithMocks.accountWebSocket)
        .insertToStateIfAbsent(String.format(
                ACCOUNT_POSITIONS_CHANGED_EVENT_STATE_PROPERTY_PATTERN,
                accountNumber),
            actionString);
  }

  @Test
  void shouldUnsubscribeFromAccountPositionsChangedEvents() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    String accountNumber = "accountNumber";
    AccountSubscriptionAction action =
        new AccountSubscriptionAction("action", accountNumber);
    Mockito.when(clientWithMocks.accountSubscriptionActionFactory
            .getAccountPositionsChangedEventAction(accountNumber, false))
        .thenReturn(action);
    String actionString = "action";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(action))
        .thenReturn(actionString);

    clientWithMocks.client.unsubscribeFromAccountPositionsChangedEvents(accountNumber);

    Mockito.verify(clientWithMocks.accountWebSocket)
        .removeFromStateWith(String.format(
                ACCOUNT_POSITIONS_CHANGED_EVENT_STATE_PROPERTY_PATTERN,
                accountNumber),
            actionString);
  }

  @Test
  void shouldSubscribeForAccountOrderChangedEvents() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    String accountNumber = "accountNumber";
    AccountSubscriptionAction action =
        new AccountSubscriptionAction("action", accountNumber);
    Mockito.when(clientWithMocks.accountSubscriptionActionFactory
            .getAccountOrderChangedEventAction(accountNumber, true))
        .thenReturn(action);
    String actionString = "action";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(action))
        .thenReturn(actionString);

    clientWithMocks.client.subscribeForAccountOrderChangedEvents(accountNumber);

    Mockito.verify(clientWithMocks.accountWebSocket)
        .insertToStateIfAbsent(String.format(
                ACCOUNT_ORDER_CHANGED_EVENT_STATE_PROPERTY_PATTERN,
                accountNumber),
            actionString);
  }

  @Test
  void shouldUnsubscribeFromAccountOrderChangedEvents() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    String accountNumber = "accountNumber";
    AccountSubscriptionAction action =
        new AccountSubscriptionAction("action", accountNumber);
    Mockito.when(clientWithMocks.accountSubscriptionActionFactory
            .getAccountOrderChangedEventAction(accountNumber, false))
        .thenReturn(action);
    String actionString = "action";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(action))
        .thenReturn(actionString);

    clientWithMocks.client.unsubscribeFromAccountOrderChangedEvents(accountNumber);

    Mockito.verify(clientWithMocks.accountWebSocket)
        .removeFromStateWith(String.format(
                ACCOUNT_ORDER_CHANGED_EVENT_STATE_PROPERTY_PATTERN,
                accountNumber),
            actionString);
  }

  @Test
  void shouldSubscribeForAccountTradeChangedEvents() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    String accountNumber = "accountNumber";
    AccountSubscriptionAction action =
        new AccountSubscriptionAction("action", accountNumber);
    Mockito.when(clientWithMocks.accountSubscriptionActionFactory
            .getAccountTradeChangedEventAction(accountNumber, true))
        .thenReturn(action);
    String actionString = "action";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(action))
        .thenReturn(actionString);

    clientWithMocks.client.subscribeForAccountTradeChangedEvents(accountNumber);

    Mockito.verify(clientWithMocks.accountWebSocket)
        .insertToStateIfAbsent(String.format(
                ACCOUNT_TRADE_CHANGED_EVENT_STATE_PROPERTY_PATTERN,
                accountNumber),
            actionString);
  }

  @Test
  void shouldUnsubscribeFromAccountTradeChangedEvents() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    String accountNumber = "accountNumber";
    AccountSubscriptionAction action =
        new AccountSubscriptionAction("action", accountNumber);
    Mockito.when(clientWithMocks.accountSubscriptionActionFactory
            .getAccountTradeChangedEventAction(accountNumber, false))
        .thenReturn(action);
    String actionString = "action";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(action))
        .thenReturn(actionString);

    clientWithMocks.client.unsubscribeFromAccountTradeChangedEvents(accountNumber);

    Mockito.verify(clientWithMocks.accountWebSocket)
        .removeFromStateWith(String.format(
                ACCOUNT_TRADE_CHANGED_EVENT_STATE_PROPERTY_PATTERN,
                accountNumber),
            actionString);
  }

  @Test
  @SuppressWarnings("rawtypes,unchecked")
  void shouldSubscribeForMarketDataEvents() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();
    Set<String> symbols = Set.of("a", "b", "c", "d");
    MarketDataSubscriptionAction action =
        new MarketDataSubscriptionAction("action", symbols);
    Mockito.when(clientWithMocks.marketDataSubscriptionActionFactory
            .getAction(symbols, true))
        .thenReturn(action);
    String actionString = "action";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(action))
        .thenReturn(actionString);

    clientWithMocks.client.subscribeForMarketDataEvents(symbols);

    ArgumentCaptor<Function> stateTransitionDataFunctionCaptor =
        ArgumentCaptor.forClass(Function.class);
    Mockito.verify(clientWithMocks.marketDataWebSocket())
        .updateState(
            Mockito.eq(MARKET_DATA_EVENT_STATE_PROPERTY),
            Mockito.eq(actionString),
            stateTransitionDataFunctionCaptor.capture());
    Function<String, String> stateTransitionDataFunction =
        stateTransitionDataFunctionCaptor.getValue();

    Assertions.assertNull(stateTransitionDataFunction.apply(null));

    String oldData = "oldData";
    MarketDataSubscriptionAction oldAction =
        new MarketDataSubscriptionAction("action", Set.of("a", "b"));
    Mockito.when(clientWithMocks.jsonMapper.readValue(oldData, MarketDataSubscriptionAction.class))
            .thenReturn(oldAction);
    Assertions.assertNull(stateTransitionDataFunction.apply(oldData));

    oldAction = new MarketDataSubscriptionAction("action", Set.of("a", "b", "e", "f"));
    Mockito.when(clientWithMocks.jsonMapper.readValue(oldData, MarketDataSubscriptionAction.class))
        .thenReturn(oldAction);
    MarketDataSubscriptionAction actionForStateTransition =
        new MarketDataSubscriptionAction("action", Set.of());
    Mockito.when(clientWithMocks.marketDataSubscriptionActionFactory.getAction(Set.of("e", "f"), false))
            .thenReturn(actionForStateTransition);
    String actionForStateTransitionString = "actionForStateTransition";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(actionForStateTransition))
            .thenReturn(actionForStateTransitionString);
    Assertions.assertEquals(
        actionForStateTransitionString,
        stateTransitionDataFunction.apply(oldData));
  }

  @Test
  @SuppressWarnings("rawtypes,unchecked")
  void shouldUnsubscribeFromMarketDataEvents() throws Exception {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();

    clientWithMocks.client.unsubscribeFromMarketDataEvents();

    ArgumentCaptor<Function> stateTransitionDataFunctionCaptor =
        ArgumentCaptor.forClass(Function.class);
    Mockito.verify(clientWithMocks.marketDataWebSocket())
        .updateState(
            Mockito.eq(MARKET_DATA_EVENT_STATE_PROPERTY),
            Mockito.isNull(),
            stateTransitionDataFunctionCaptor.capture());
    Function<String, String> stateTransitionDataFunction =
        stateTransitionDataFunctionCaptor.getValue();

    Assertions.assertNull(stateTransitionDataFunction.apply(null));

    String oldData = "oldData";
    MarketDataSubscriptionAction oldAction =
        new MarketDataSubscriptionAction("action", Set.of("a", "b"));
    Mockito.when(clientWithMocks.jsonMapper.readValue(oldData, MarketDataSubscriptionAction.class))
        .thenReturn(oldAction);
    MarketDataSubscriptionAction actionForStateTransition =
        new MarketDataSubscriptionAction("action", Set.of());
    Mockito.when(clientWithMocks.marketDataSubscriptionActionFactory
            .getAction(oldAction.symbols(), false))
        .thenReturn(actionForStateTransition);
    String actionForStateTransitionString = "actionForStateTransition";
    Mockito.when(clientWithMocks.jsonMapper.writeValueAsString(actionForStateTransition))
        .thenReturn(actionForStateTransitionString);
    Assertions.assertEquals(
        actionForStateTransitionString,
        stateTransitionDataFunction.apply(oldData));
  }

  @Test
  void shouldClose() {
    ClientWithMocks clientWithMocks = buildClientWithMocksAndInitUrls();

    clientWithMocks.client.close();
    Assertions.assertThrows(
        IllegalLimeTraderClientStateException.class,
        clientWithMocks.client::getAccountBalances);

    Mockito.verify(clientWithMocks.accountWebSocket).close();
    Mockito.verify(clientWithMocks.marketDataWebSocket).close();
  }

  @SuppressWarnings("unchecked")
  private Authentication mockSuccessfulAuth(ClientWithMocks clientWithMocks) throws Exception {
    Mockito.when(clientWithMocks.instantSource.instant()).thenReturn(DEFAULT_NOW);
    HttpRequest request = buildAuthHttpRequest();
    HttpResponse<String> response = (HttpResponse<String>) Mockito.mock(HttpResponse.class);
    Mockito.when(clientWithMocks.httpClient.send(request, BodyHandlers.ofString()))
        .thenReturn(response);
    Mockito.when(response.statusCode()).thenReturn(200);
    String responseBody = "responseBody";
    Mockito.when(response.body()).thenReturn(responseBody);
    Authentication auth = new Authentication("scope", "tokenType", "accessToken", 86400);
    Mockito.when(clientWithMocks.jsonMapper.readValue(responseBody, Authentication.class))
        .thenReturn(auth);
    return auth;
  }

  private void verifyAuthenticateTimes(Integer numberOfInvocations, ClientWithMocks clientWithMocks)
      throws Exception {
    Mockito.verify(clientWithMocks.httpClient, Mockito.times(numberOfInvocations))
        .send(buildAuthHttpRequest(), BodyHandlers.ofString());
  }

  private HttpRequest buildAuthHttpRequest() {
    Map<String, String> credentialsForm = Map.of(
        "grant_type", "password",
        "client_id", credentials.clientId(),
        "client_secret", credentials.clientSecret(),
        "username", credentials.username(),
        "password", credentials.password());
    return HttpRequest.newBuilder()
        .uri(URI.create(tokenUrl))
        .header("accept", "application/json")
        .header("content-type", "application/x-www-form-urlencoded")
        .POST(BodyPublishers.ofString(createUrlEncodedForm(credentialsForm)))
        .build();
  }

  private static String createUrlEncodedForm(Map<String, String> form) {
    return form.entrySet()
        .stream()
        .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
            + "="
            + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
  }

  private static URI buildUri(String base, Map<String, String> requestParams) {
    if (requestParams.isEmpty() || requestParams.values().stream().noneMatch(Objects::nonNull)) {
      return URI.create(base);
    }
    StringBuilder builder = new StringBuilder();
    builder.append(base).append("?");
    for (Entry<String, String> entry : requestParams.entrySet()) {
      if (entry.getValue() != null) {
        builder
            .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
            .append("=")
            .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
            .append("&");
      }
    }
    return URI.create(builder.toString());
  }

  private ClientWithMocks buildClientWithMocksAndInitUrls() {
    try (
        MockedStatic<JsonMapper> jsonMapperMockedStatic = Mockito.mockStatic(JsonMapper.class);
        MockedStatic<Clock> clockMockedStatic = Mockito.mockStatic(Clock.class);
        MockedConstruction<AccountSubscriptionActionFactory> accountSubscriptionActionFactoryMockedConstruction =
            Mockito.mockConstruction(AccountSubscriptionActionFactory.class);
        MockedConstruction<MarketDataSubscriptionActionFactory> marketDataSubscriptionActionFactoryMockedConstruction =
            Mockito.mockConstruction(MarketDataSubscriptionActionFactory.class);
        MockedConstruction<ResilientWebSocket> resilientWebSocketMockedConstruction =
            Mockito.mockConstruction(ResilientWebSocket.class)) {
      HttpClient httpClient = Mockito.mock(HttpClient.class);
      ClientConfig clientConfig =
          new ClientConfig(httpClient, null, null, null);
      Builder jsonMapperBuilder = Mockito.mock(Builder.class);
      JsonMapper jsonMapper = Mockito.mock(JsonMapper.class);
      jsonMapperMockedStatic.when(JsonMapper::builder)
          .thenReturn(jsonMapperBuilder);
      Mockito.when(jsonMapperBuilder.addModule(Mockito.any()))
          .thenReturn(jsonMapperBuilder);
      Mockito.when(jsonMapperBuilder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))
          .thenReturn(jsonMapperBuilder);
      Mockito.when(jsonMapperBuilder.propertyNamingStrategy(Mockito.any()))
          .thenReturn(jsonMapperBuilder);
      Mockito.when(jsonMapperBuilder.build())
          .thenReturn(jsonMapper);
      InstantSource instantSource = Mockito.mock(Clock.class);
      clockMockedStatic.when(Clock::systemDefaultZone).thenReturn(instantSource);
      LimeTraderClient client = new LimeTraderClient(clientConfig, credentials);
      initUrls(clientConfig);
      return new ClientWithMocks(
          client,
          httpClient,
          jsonMapper,
          instantSource,
          accountSubscriptionActionFactoryMockedConstruction.constructed().get(0),
          resilientWebSocketMockedConstruction.constructed().get(0),
          marketDataSubscriptionActionFactoryMockedConstruction.constructed().get(0),
          resilientWebSocketMockedConstruction.constructed().get(1));
    }
  }

  private void initUrls(ClientConfig clientConfig) {
    this.baseHttpAuthUrl = Objects.requireNonNullElse(
        clientConfig.baseHttpAuthUrl(), DEFAULT_BASE_HTTP_AUTH_URL);
    this.tokenUrl = this.baseHttpAuthUrl + "/connect/token";
    this.baseHttpApiUrl = Objects.requireNonNullElse(
        clientConfig.baseHttpApiUrl(), DEFAULT_BASE_HTTP_API_URL);
    this.accountsUrl = this.baseHttpApiUrl + "/accounts";
    this.accountPositionsUrlPattern = this.accountsUrl + "/%s/positions";
    this.accountTradesUrlPattern = this.accountsUrl + "/%s/trades/%s";
    this.accountActiveOrderUrlPattern = this.accountsUrl + "/%s/activeorders";
    this.accountTransactionsUrlPattern = this.accountsUrl + "/%s/transactions";
    this.ordersUrl = this.baseHttpApiUrl + "/orders";
    this.orderUrlPattern = this.ordersUrl + "/%s";
    this.placeOrderUrl = this.ordersUrl + "/place";
    this.validateOrderUrl = this.ordersUrl + "/validate";
    this.cancelOrderUrlPattern = this.ordersUrl + "/%s/cancel";
    this.pricingUrl = this.baseHttpApiUrl + "/pricing";
    this.pricingFeesUrl = this.pricingUrl + "/fees";
    this.marketDataUrl = this.baseHttpApiUrl + "/marketdata";
    this.quoteUrl = this.marketDataUrl + "/quote";
    this.quotesUrl = this.marketDataUrl + "/quotes";
    this.quoteHistoryUrl = this.marketDataUrl + "/history";
    this.tradingScheduleUrl = this.marketDataUrl + "/schedule";
    this.securitiesUrl = this.baseHttpApiUrl + "/securities";
    this.optionsUrlPattern = this.securitiesUrl + "/%s/options";
    this.optionSeriesUrlPattern = this.optionsUrlPattern + "/series";
    this.optionExpirationUrlPattern = this.optionsUrlPattern + "/expirations";
    this.baseWsApiUrl = Objects.requireNonNullElse(
        clientConfig.baseWsApiUrl(), DEFAULT_BASE_WS_API_URL);
    this.accountWsUrl = this.baseWsApiUrl + "/accounts";
    this.marketDataWsUrl = this.baseWsApiUrl + "/marketdata";
  }

  private record ClientWithMocks(
      LimeTraderClient client,
      HttpClient httpClient,
      JsonMapper jsonMapper,
      InstantSource instantSource,
      AccountSubscriptionActionFactory accountSubscriptionActionFactory,
      ResilientWebSocket accountWebSocket,
      MarketDataSubscriptionActionFactory marketDataSubscriptionActionFactory,
      ResilientWebSocket marketDataWebSocket) {

  }

}
