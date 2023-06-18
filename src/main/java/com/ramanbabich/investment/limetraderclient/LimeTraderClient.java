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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.ramanbabich.investment.limetraderclient.account.AccountBalanceChangedEventSubscriber;
import com.ramanbabich.investment.limetraderclient.account.AccountErrorEventSubscriber;
import com.ramanbabich.investment.limetraderclient.account.AccountOrderChangedEventSubscriber;
import com.ramanbabich.investment.limetraderclient.account.AccountPositionsChangedEventSubscriber;
import com.ramanbabich.investment.limetraderclient.account.AccountSubscriptionActionFactory;
import com.ramanbabich.investment.limetraderclient.account.AccountTextSubscriber;
import com.ramanbabich.investment.limetraderclient.account.AccountTradeChangedEventSubscriber;
import com.ramanbabich.investment.limetraderclient.account.model.AccountActiveOrder;
import com.ramanbabich.investment.limetraderclient.account.model.AccountBalance;
import com.ramanbabich.investment.limetraderclient.account.model.AccountPosition;
import com.ramanbabich.investment.limetraderclient.account.model.AccountPositionQuery;
import com.ramanbabich.investment.limetraderclient.account.model.AccountSubscriptionAction;
import com.ramanbabich.investment.limetraderclient.account.model.AccountTradeList;
import com.ramanbabich.investment.limetraderclient.account.model.AccountTradeQuery;
import com.ramanbabich.investment.limetraderclient.account.model.AccountTransactionList;
import com.ramanbabich.investment.limetraderclient.account.model.AccountTransactionQuery;
import com.ramanbabich.investment.limetraderclient.auth.model.Authentication;
import com.ramanbabich.investment.limetraderclient.auth.model.Credentials;
import com.ramanbabich.investment.limetraderclient.common.model.ClientConfig;
import com.ramanbabich.investment.limetraderclient.common.model.IllegalLimeTraderClientStateException;
import com.ramanbabich.investment.limetraderclient.common.model.InvalidCredentialsException;
import com.ramanbabich.investment.limetraderclient.common.model.InvalidInputException;
import com.ramanbabich.investment.limetraderclient.common.model.OrderType;
import com.ramanbabich.investment.limetraderclient.common.model.UnexpectedFailureException;
import com.ramanbabich.investment.limetraderclient.common.websocket.IllegalResilientWebSocketStateException;
import com.ramanbabich.investment.limetraderclient.common.websocket.ResilientWebSocket;
import com.ramanbabich.investment.limetraderclient.common.websocket.StateChangeFailedException;
import com.ramanbabich.investment.limetraderclient.marketdata.MarketDataErrorEventSubscriber;
import com.ramanbabich.investment.limetraderclient.marketdata.MarketDataSubscriptionActionFactory;
import com.ramanbabich.investment.limetraderclient.marketdata.MarketDataTextSubscriber;
import com.ramanbabich.investment.limetraderclient.marketdata.QuoteChangedEventSubscriber;
import com.ramanbabich.investment.limetraderclient.marketdata.TradeMadeEventSubscriber;
import com.ramanbabich.investment.limetraderclient.marketdata.model.Candle;
import com.ramanbabich.investment.limetraderclient.marketdata.model.MarketDataSubscriptionAction;
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
import java.io.IOException;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author Raman Babich
 */
public class LimeTraderClient implements AutoCloseable {

  private static final String DEFAULT_BASE_HTTP_AUTH_URL = "https://auth.lime.co";
  private static final String DEFAULT_BASE_HTTP_API_URL = "https://api.lime.co";
  private static final String DEFAULT_BASE_WS_API_URL = "wss://api.lime.co";
  private static final String AUTHORIZATION_HEADER = "authorization";
  private static final String AUTHORIZATION_PREFIX = "Bearer ";

  private static final ClientConfig DEFAULT_CONFIG =
      new ClientConfig(null, null, null, null);

  private static final String ACCOUNT_BALANCE_CHANGED_EVENT_STATE_PROPERTY_PATTERN =
      "balanceChanged.%s";
  private static final String ACCOUNT_POSITIONS_CHANGED_EVENT_STATE_PROPERTY_PATTERN =
      "positionsChanged.%s";
  private static final String ACCOUNT_ORDER_CHANGED_EVENT_STATE_PROPERTY_PATTERN =
      "orderChanged.%s";
  private static final String ACCOUNT_TRADE_CHANGED_EVENT_STATE_PROPERTY_PATTERN =
      "tradeChanged.%s";
  private static final String MARKET_DATA_EVENT_STATE_PROPERTY = "marketDataSymbols";

  private final HttpClient httpClient;
  private final JsonMapper jsonMapper;
  private final InstantSource instantSource;
  private final Credentials credentials;
  private final Lock authLock;
  private Authentication auth = null;
  private Instant authRefreshTimestamp = null;
  private final AccountSubscriptionActionFactory accountSubscriptionActionFactory;
  private final ResilientWebSocket accountWebSocket;
  private final MarketDataSubscriptionActionFactory marketDataSubscriptionActionFactory;
  private final ResilientWebSocket marketDataWebSocket;
  private volatile boolean closed = false;

  private final String baseHttpAuthUrl;
  private final String tokenUrl;
  private final String baseHttpApiUrl;
  private final String accountsUrl;
  private final String accountPositionsUrlPattern;
  private final String accountTradesUrlPattern;
  private final String accountActiveOrderUrlPattern;
  private final String accountTransactionsUrlPattern;
  private final String ordersUrl;
  private final String orderUrlPattern;
  private final String placeOrderUrl;
  private final String validateOrderUrl;
  private final String cancelOrderUrlPattern;
  private final String pricingUrl;
  private final String pricingFeesUrl;
  private final String marketDataUrl;
  private final String quoteUrl;
  private final String quotesUrl;
  private final String quoteHistoryUrl;
  private final String tradingScheduleUrl;
  private final String securitiesUrl;
  private final String optionsUrlPattern;
  private final String optionSeriesUrlPattern;
  private final String optionExpirationUrlPattern;

  private final String baseWsApiUrl;
  private final String accountWsUrl;
  private final String marketDataWsUrl;

  public LimeTraderClient(Credentials credentials) {
    this(DEFAULT_CONFIG, credentials);
  }

  public LimeTraderClient(ClientConfig clientConfig, Credentials credentials) {
    Objects.requireNonNull(clientConfig, "Client config should be specified");
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

    this.httpClient = Objects.requireNonNullElse(
        clientConfig.httpClient(), HttpClient.newHttpClient());
    this.jsonMapper = JsonMapper.builder()
        .addModule(new ParameterNamesModule())
        .addModule(new Jdk8Module())
        .addModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .propertyNamingStrategy(new SnakeCaseStrategy())
        .build();
    this.instantSource = Clock.systemDefaultZone();
    this.credentials = Objects.requireNonNull(credentials, "Credentials should be specified");
    this.authLock = new ReentrantLock();
    this.accountSubscriptionActionFactory = new AccountSubscriptionActionFactory();
    this.accountWebSocket = new ResilientWebSocket(
        "account-web-socket",
        URI.create(accountWsUrl),
        () -> {
          refreshAuthIfNeeded();
          return httpClient.newWebSocketBuilder()
              .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken());
        },
        new AccountTextSubscriber(this.jsonMapper));
    this.marketDataSubscriptionActionFactory = new MarketDataSubscriptionActionFactory();
    this.marketDataWebSocket = new ResilientWebSocket(
        "market-data-web-socket",
        URI.create(marketDataWsUrl),
        () -> {
          refreshAuthIfNeeded();
          return httpClient.newWebSocketBuilder()
              .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken());
        },
        new MarketDataTextSubscriber(this.jsonMapper));
  }

  private static String createUrlEncodedForm(Map<String, String> form) {
    return form.entrySet()
        .stream()
        .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                + "="
                + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
  }

  private void authenticate() {
    try {
      doAuthenticate();
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(
          "Can't retrieve an authentication because of unexpected error.", ex);
    }
  }

  private void doAuthenticate() throws IOException, InterruptedException {
    Map<String, String> credentialsForm = Map.of(
        "grant_type", "password",
        "client_id", credentials.clientId(),
        "client_secret", credentials.clientSecret(),
        "username", credentials.username(),
        "password", credentials.password());
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(tokenUrl))
        .header("accept", "application/json")
        .header("content-type", "application/x-www-form-urlencoded")
        .POST(BodyPublishers.ofString(createUrlEncodedForm(credentialsForm)))
        .build();
    Instant now = instantSource.instant();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      auth = jsonMapper.readValue(response.body(), Authentication.class);
      authRefreshTimestamp =
          now.plus((long) (auth.expiresIn() * 0.8), ChronoUnit.SECONDS);
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidCredentialsException(String.format(
          "Can't retrieve an authentication because of '%s' error.", response.body()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't retrieve an authentication because of '%s' error with status '%d'.",
          response.body(), response.statusCode()));
    }
  }

  private void refreshAuthIfNeeded() {
    Instant now = instantSource.instant();
    if (authRefreshTimestamp == null || authRefreshTimestamp.isBefore(now)) {
      authLock.lock();
      try {
        if (authRefreshTimestamp == null || authRefreshTimestamp.isBefore(now)) {
          authenticate();
        }
      } finally {
        authLock.unlock();
      }
    }
  }

  public List<AccountBalance> getAccountBalances() {
    try {
      return doGetAccountBalances();
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(
          "Can't get account balances because of unexpected error.", ex);
    }
  }

  private List<AccountBalance> doGetAccountBalances() throws IOException, InterruptedException {
    verifyNotClosed();
    refreshAuthIfNeeded();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(accountsUrl))
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(
          response.body(),
          TypeFactory.defaultInstance().constructCollectionType(List.class, AccountBalance.class));
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get account balances because of '%s' input error with status '%d'.",
          response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get account balances because of '%s' error with status '%d'.",
          response.body(), response.statusCode()));
    }
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

  public List<AccountPosition> getAccountPositions(AccountPositionQuery accountPositionQuery) {
    try {
      return doGetAccountPositions(accountPositionQuery);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't get account positions for '%s' because of unexpected error.",
          accountPositionQuery),
          ex);
    }
  }

  private List<AccountPosition> doGetAccountPositions(AccountPositionQuery accountPositionQuery)
      throws IOException, InterruptedException {
    Objects.requireNonNull(accountPositionQuery, "Account position query should be specified.");
    Objects.requireNonNull(accountPositionQuery.accountNumber(),
        "Account number should be specified.");
    verifyNotClosed();
    refreshAuthIfNeeded();
    Map<String, String> requestParams = new TreeMap<>();
    if (accountPositionQuery.date() != null) {
      requestParams.put("date", DateTimeFormatter.ISO_DATE.format(accountPositionQuery.date()));
    }
    URI uri = buildUri(String.format(
            accountPositionsUrlPattern,
            URLEncoder.encode(accountPositionQuery.accountNumber(), StandardCharsets.UTF_8)),
        requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(
          response.body(),
          TypeFactory.defaultInstance().constructCollectionType(List.class, AccountPosition.class));
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get account positions for '%s' because of '%s' input error with status '%d'.",
          accountPositionQuery, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get account positions for '%s' because of '%s' error with status '%d'.",
          accountPositionQuery, response.body(), response.statusCode()));
    }
  }

  public AccountTradeList getAccountTrades(AccountTradeQuery accountTradeQuery) {
    try {
      return doGetAccountTrades(accountTradeQuery);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't get account trades for '%s' because of unexpected error.", accountTradeQuery),
          ex);
    }
  }

  private AccountTradeList doGetAccountTrades(AccountTradeQuery accountTradeQuery)
      throws IOException, InterruptedException {
    Objects.requireNonNull(accountTradeQuery, "Account trade query should be specified.");
    Objects.requireNonNull(accountTradeQuery.accountNumber(),
        "Account number should be specified.");
    Objects.requireNonNull(accountTradeQuery.date(), "Date should be specified.");
    verifyNotClosed();
    refreshAuthIfNeeded();
    Map<String, String> requestParams = new TreeMap<>();
    if (accountTradeQuery.limit() != null) {
      requestParams.put("limit", accountTradeQuery.limit().toString());
    }
    if (accountTradeQuery.skip() != null) {
      requestParams.put("skip", accountTradeQuery.skip().toString());
    }
    URI uri = buildUri(String.format(
            accountTradesUrlPattern,
            URLEncoder.encode(accountTradeQuery.accountNumber(), StandardCharsets.UTF_8),
            URLEncoder.encode(
                DateTimeFormatter.ISO_DATE.format(accountTradeQuery.date()),
                StandardCharsets.UTF_8)),
        requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), AccountTradeList.class);
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get account trades for '%s' because of '%s' input error with status '%d'.",
          accountTradeQuery, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get account trades for '%s' because of '%s' error with status '%d'.",
          accountTradeQuery, response.body(), response.statusCode()));
    }
  }

  public List<AccountActiveOrder> getAccountActiveOrders(String accountNumber) {
    try {
      return doGetAccountActiveOrders(accountNumber);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't get account '%s' active orders because of unexpected error.", accountNumber),
          ex);
    }
  }

  private List<AccountActiveOrder> doGetAccountActiveOrders(String accountNumber)
      throws IOException, InterruptedException {
    Objects.requireNonNull(accountNumber, "Account number should be specified.");
    verifyNotClosed();
    refreshAuthIfNeeded();
    URI uri = URI.create(String.format(
        accountActiveOrderUrlPattern,
        URLEncoder.encode(accountNumber, StandardCharsets.UTF_8)));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), TypeFactory.defaultInstance()
          .constructCollectionType(List.class, AccountActiveOrder.class));
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get account '%s' active orders because of '%s' input error with status '%d'.",
          accountNumber, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get account '%s' active orders because of '%s' error with status '%d'.",
          accountNumber, response.body(), response.statusCode()));
    }
  }

  public AccountTransactionList getAccountTransactions(
      AccountTransactionQuery accountTransactionQuery) {
    try {
      return doGetAccountTransactions(accountTransactionQuery);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't get account transactions for '%s' because of unexpected error.",
          accountTransactionQuery),
          ex);
    }
  }

  private AccountTransactionList doGetAccountTransactions(
      AccountTransactionQuery accountTransactionQuery)
      throws IOException, InterruptedException {
    Objects.requireNonNull(accountTransactionQuery,
        "Account transaction query should be specified.");
    Objects.requireNonNull(accountTransactionQuery.accountNumber(),
        "Account number should be specified.");
    verifyNotClosed();
    refreshAuthIfNeeded();
    Map<String, String> requestParams = new TreeMap<>();
    if (accountTransactionQuery.startDate() != null) {
      requestParams.put("start_date",
          DateTimeFormatter.ISO_DATE.format(accountTransactionQuery.startDate()));
    }
    if (accountTransactionQuery.endDate() != null) {
      requestParams.put("end_date",
          DateTimeFormatter.ISO_DATE.format(accountTransactionQuery.endDate()));
    }
    if (accountTransactionQuery.limit() != null) {
      requestParams.put("limit", accountTransactionQuery.limit().toString());
    }
    if (accountTransactionQuery.skip() != null) {
      requestParams.put("skip", accountTransactionQuery.skip().toString());
    }
    URI uri = buildUri(String.format(
            accountTransactionsUrlPattern,
            URLEncoder.encode(accountTransactionQuery.accountNumber(), StandardCharsets.UTF_8)),
        requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), AccountTransactionList.class);
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get account transactions for '%s' because of '%s' input error with status '%d'.",
          accountTransactionQuery, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get account transactions for '%s' because of '%s' error with status '%d'.",
          accountTransactionQuery, response.body(), response.statusCode()));
    }
  }

  public OrderPlacementResult placeOrder(OrderPlacementInfo orderPlacementInfo) {
    try {
      return doPlaceOrder(orderPlacementInfo);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't place the order '%s' because of unexpected error.", orderPlacementInfo),
          ex);
    }
  }

  private OrderPlacementResult doPlaceOrder(OrderPlacementInfo orderPlacementInfo)
      throws IOException, InterruptedException {
    validateOrderPlacementInfo(orderPlacementInfo);
    verifyNotClosed();
    refreshAuthIfNeeded();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(placeOrderUrl))
        .header("accept", "application/json")
        .header("content-type", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .POST(BodyPublishers.ofString(jsonMapper.writeValueAsString(orderPlacementInfo)))
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), OrderPlacementResult.class);
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't place the order '%s' because of '%s' input error with status '%d'.",
          orderPlacementInfo, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't place the order '%s' because of '%s' error with status '%d'.",
          orderPlacementInfo, response.body(), response.statusCode()));
    }
  }

  private void validateOrderPlacementInfo(OrderPlacementInfo orderPlacementInfo) {
    Objects.requireNonNull(orderPlacementInfo, "Order placement info should be specified.");
    Objects.requireNonNull(orderPlacementInfo.accountNumber(),
        "Account number should be specified.");
    Objects.requireNonNull(orderPlacementInfo.symbol(), "Symbol should be specified.");
    Objects.requireNonNull(orderPlacementInfo.quantity(), "Quantity should be specified.");
    String orderType = orderPlacementInfo.orderType();
    if (orderType != null) {
      if (orderType.equals(OrderType.LIMIT) || orderType.equals(OrderType.STOP_LIMIT)) {
        Objects.requireNonNull(orderPlacementInfo.price(), "Price should be specified.");
      }
      if (orderType.equals(OrderType.STOP) || orderType.equals(OrderType.STOP_LIMIT)) {
        Objects.requireNonNull(orderPlacementInfo.stopPrice(), "Stop price should be specified.");
      }
    }
  }

  public OrderValidationResult validateOrder(OrderValidationInfo orderValidationInfo) {
    try {
      return doValidateOrder(orderValidationInfo);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't validate the order '%s' because of unexpected error.", orderValidationInfo),
          ex);
    }
  }

  private OrderValidationResult doValidateOrder(OrderValidationInfo orderValidationInfo)
      throws IOException, InterruptedException {
    validateOrderValidationInfo(orderValidationInfo);
    verifyNotClosed();
    refreshAuthIfNeeded();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(validateOrderUrl))
        .header("accept", "application/json")
        .header("content-type", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .POST(BodyPublishers.ofString(jsonMapper.writeValueAsString(orderValidationInfo)))
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), OrderValidationResult.class);
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't validate the order '%s' because of '%s' input error with status '%d'.",
          orderValidationInfo, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't validate the order '%s' because of '%s' error with status '%d'.",
          orderValidationInfo, response.body(), response.statusCode()));
    }
  }

  private void validateOrderValidationInfo(OrderValidationInfo orderValidationInfo) {
    Objects.requireNonNull(orderValidationInfo, "Order placement info should be specified.");
    Objects.requireNonNull(orderValidationInfo.accountNumber(),
        "Account number should be specified.");
    Objects.requireNonNull(orderValidationInfo.symbol(), "Symbol should be specified.");
    Objects.requireNonNull(orderValidationInfo.quantity(), "Quantity should be specified.");
    String orderType = orderValidationInfo.orderType();
    if (orderType != null) {
      if (orderType.equals(OrderType.LIMIT) || orderType.equals(OrderType.STOP_LIMIT)) {
        Objects.requireNonNull(orderValidationInfo.price(), "Price should be specified.");
      }
      if (orderType.equals(OrderType.STOP) || orderType.equals(OrderType.STOP_LIMIT)) {
        Objects.requireNonNull(orderValidationInfo.stopPrice(), "Stop price should be specified.");
      }
    }
  }

  public Order getOrder(String id) {
    try {
      return doGetOrder(id);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't get the order '%s' because of unexpected error.", id),
          ex);
    }
  }

  private Order doGetOrder(String id) throws IOException, InterruptedException {
    Objects.requireNonNull(id, "Id should be specified.");
    verifyNotClosed();
    refreshAuthIfNeeded();
    URI uri = URI.create(String.format(
        orderUrlPattern,
        URLEncoder.encode(id, StandardCharsets.UTF_8)));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), Order.class);
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get the order '%s' because of '%s' input error with status '%d'.",
          id, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get the order '%s' because of '%s' error with status '%d'.",
          id, response.body(), response.statusCode()));
    }
  }

  public OrderCancellationResult cancelOrder(String id,
      OrderCancellationInfo orderCancellationInfo) {
    try {
      return doCancelOrder(id, orderCancellationInfo);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't cancel the order '%s' because of unexpected error.", orderCancellationInfo),
          ex);
    }
  }

  private OrderCancellationResult doCancelOrder(String id,
      OrderCancellationInfo orderCancellationInfo)
      throws IOException, InterruptedException {
    Objects.requireNonNull(orderCancellationInfo, "Order cancellation info should be specified.");
    Objects.requireNonNull(id, "Id should be specified.");
    verifyNotClosed();
    refreshAuthIfNeeded();
    URI uri = URI.create(String.format(
        cancelOrderUrlPattern,
        URLEncoder.encode(id, StandardCharsets.UTF_8)));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header("content-type", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .POST(BodyPublishers.ofString(jsonMapper.writeValueAsString(orderCancellationInfo)))
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), OrderCancellationResult.class);
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't cancel the order '%s' because of '%s' input error with status '%d'.",
          id, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't cancel the order '%s' because of '%s' error with status '%d'.",
          id, response.body(), response.statusCode()));
    }
  }

  public List<OrderFeeCharge> getOrderFeeCharges(OrderFeeChargeQuery orderFeeChargeQuery) {
    try {
      return doGetOrderFeeCharges(orderFeeChargeQuery);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't get order fee charges for '%s' because of unexpected error.", orderFeeChargeQuery),
          ex);
    }
  }

  private List<OrderFeeCharge> doGetOrderFeeCharges(OrderFeeChargeQuery orderFeeChargeQuery)
      throws IOException, InterruptedException {
    validateOrderFeeChargeQuery(orderFeeChargeQuery);
    verifyNotClosed();
    refreshAuthIfNeeded();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(pricingFeesUrl))
        .header("accept", "application/json")
        .header("content-type", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .POST(BodyPublishers.ofString(jsonMapper.writeValueAsString(orderFeeChargeQuery)))
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), TypeFactory.defaultInstance()
          .constructCollectionType(List.class, OrderFeeCharge.class));
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get order fee charges for '%s' because of '%s' input error with status '%d'.",
          orderFeeChargeQuery, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get order fee charges for '%s' because of '%s' error with status '%d'.",
          orderFeeChargeQuery, response.body(), response.statusCode()));
    }
  }

  private void validateOrderFeeChargeQuery(OrderFeeChargeQuery orderFeeChargeQuery) {
    Objects.requireNonNull(orderFeeChargeQuery, "Order fee charge query should be specified.");
    Objects.requireNonNull(orderFeeChargeQuery.accountNumber(),
        "Account number should be specified.");
    Objects.requireNonNull(orderFeeChargeQuery.symbol(), "Symbol should be specified.");
    Objects.requireNonNull(orderFeeChargeQuery.quantity(), "Quantity should be specified.");
    Objects.requireNonNull(orderFeeChargeQuery.side(), "Side should be specified.");
  }

  public Quote getQuote(String symbol) {
    try {
      return doGetQuote(symbol);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't get quote '%s' because of unexpected error.", symbol),
          ex);
    }
  }

  private Quote doGetQuote(String symbol) throws IOException, InterruptedException {
    Objects.requireNonNull(symbol, "Symbol should be specified.");
    verifyNotClosed();
    refreshAuthIfNeeded();
    URI uri = buildUri(quoteUrl, Map.of("symbol", symbol));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), Quote.class);
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get quote '%s' because of '%s' input error with status '%d'.",
          symbol, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get quote '%s' because of '%s' error with status '%d'.",
          symbol, response.body(), response.statusCode()));
    }
  }

  public List<Quote> getQuotes(List<String> symbols) {
    try {
      return doGetQuotes(symbols);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't get quotes '%s' because of unexpected error.", String.join(",", symbols)),
          ex);
    }
  }

  private List<Quote> doGetQuotes(List<String> symbols) throws IOException, InterruptedException {
    for (String symbol : symbols) {
      Objects.requireNonNull(symbol, "Symbol should be specified.");
    }
    verifyNotClosed();
    refreshAuthIfNeeded();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(quotesUrl))
        .header("accept", "application/json")
        .header("content-type", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .POST(BodyPublishers.ofString(jsonMapper.writeValueAsString(symbols)))
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), TypeFactory.defaultInstance()
          .constructCollectionType(List.class, Quote.class));
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get quotes '%s' because of '%s' input error with status '%d'.",
          String.join(",", symbols), response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get quotes '%s' because of '%s' error with status '%d'.",
          String.join(",", symbols), response.body(), response.statusCode()));
    }
  }

  public List<Candle> getQuoteHistory(QuoteHistoryQuery quoteHistoryQuery) {
    try {
      return doGetQuoteHistory(quoteHistoryQuery);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't get quote history for '%s' because of unexpected error.", quoteHistoryQuery),
          ex);
    }
  }

  private List<Candle> doGetQuoteHistory(QuoteHistoryQuery quoteHistoryQuery)
      throws IOException, InterruptedException {
    validateQuoteHistoryQuery(quoteHistoryQuery);
    verifyNotClosed();
    refreshAuthIfNeeded();
    Map<String, String> requestParams = new TreeMap<>();
    requestParams.put("symbol", quoteHistoryQuery.symbol());
    requestParams.put("period", quoteHistoryQuery.period());
    requestParams.put("from", Long.toString(quoteHistoryQuery.from().getEpochSecond()));
    requestParams.put("to", Long.toString(quoteHistoryQuery.to().getEpochSecond()));
    URI uri = buildUri(quoteHistoryUrl, requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), TypeFactory.defaultInstance()
          .constructCollectionType(List.class, Candle.class));
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get quote history for '%s' because of '%s' input error with status '%d'.",
          quoteHistoryQuery, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get quote history for '%s' because of '%s' error with status '%d'.",
          quoteHistoryQuery, response.body(), response.statusCode()));
    }
  }

  private void validateQuoteHistoryQuery(QuoteHistoryQuery quoteHistoryQuery) {
    Objects.requireNonNull(quoteHistoryQuery, "Quote history query should be specified.");
    Objects.requireNonNull(quoteHistoryQuery.symbol(), "Symbol should be specified.");
    Objects.requireNonNull(quoteHistoryQuery.period(), "Period should be specified.");
    Objects.requireNonNull(quoteHistoryQuery.from(), "From should be specified.");
    Objects.requireNonNull(quoteHistoryQuery.to(), "To should be specified.");
  }

  public TradingSchedule getTradingSchedule() {
    try {
      return doGetTradingSchedule();
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(
          "Can't get trading schedule because of unexpected error.", ex);
    }
  }

  private TradingSchedule doGetTradingSchedule() throws IOException, InterruptedException {
    verifyNotClosed();
    refreshAuthIfNeeded();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(tradingScheduleUrl))
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), TradingSchedule.class);
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get trading schedule because of '%s' input error with status '%d'.",
          response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get trading schedule because of '%s' error with status '%d'.",
          response.body(), response.statusCode()));
    }
  }

  public SecurityList getSecurities(SecurityQuery securityQuery) {
    try {
      return doGetSecurities(securityQuery);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't get securities for '%s' because of unexpected error.", securityQuery),
          ex);
    }
  }

  private SecurityList doGetSecurities(SecurityQuery securityQuery)
      throws IOException, InterruptedException {
    Objects.requireNonNull(securityQuery, "Security query should be specified.");
    Objects.requireNonNull(securityQuery.query(), "Query should be specified.");
    verifyNotClosed();
    refreshAuthIfNeeded();
    Map<String, String> requestParams = new TreeMap<>();
    if (securityQuery.query() != null) {
      requestParams.put("query", securityQuery.query());
    }
    if (securityQuery.limit() != null) {
      requestParams.put("limit", securityQuery.limit().toString());
    }
    URI uri = buildUri(securitiesUrl, requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), SecurityList.class);
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get securities for '%s' because of '%s' input error with status '%d'.",
          securityQuery, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get securities for '%s' because of '%s' error with status '%d'.",
          securityQuery, response.body(), response.statusCode()));
    }
  }

  public List<String> getOptionSeries(String symbol) {
    try {
      return doGetOptionSeries(symbol);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't get option series for '%s' because of unexpected error.", symbol),
          ex);
    }
  }

  private List<String> doGetOptionSeries(String symbol) throws IOException, InterruptedException {
    Objects.requireNonNull(symbol, "Symbol should be specified.");
    verifyNotClosed();
    refreshAuthIfNeeded();
    URI uri = URI.create(String.format(
        optionSeriesUrlPattern,
        URLEncoder.encode(symbol, StandardCharsets.UTF_8)));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), TypeFactory.defaultInstance()
          .constructCollectionType(List.class, String.class));
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get option series for '%s' because of '%s' input error with status '%d'.",
          symbol, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get option series for '%s' because of '%s' error with status '%d'.",
          symbol, response.body(), response.statusCode()));
    }
  }

  public List<LocalDate> getOptionExpirations(OptionExpirationQuery optionExpirationQuery) {
    try {
      return doGetOptionExpirations(optionExpirationQuery);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't get option expirations for '%s' because of unexpected error.",
          optionExpirationQuery),
          ex);
    }
  }

  private List<LocalDate> doGetOptionExpirations(OptionExpirationQuery optionExpirationQuery)
      throws IOException, InterruptedException {
    Objects.requireNonNull(optionExpirationQuery, "Option expiration query should be specified.");
    Objects.requireNonNull(optionExpirationQuery.symbol(), "Symbol should be specified.");
    verifyNotClosed();
    refreshAuthIfNeeded();
    Map<String, String> requestParams = new TreeMap<>();
    if (optionExpirationQuery.series() != null) {
      requestParams.put("series", optionExpirationQuery.series());
    }
    URI uri = buildUri(String.format(
            optionExpirationUrlPattern,
            URLEncoder.encode(optionExpirationQuery.symbol(), StandardCharsets.UTF_8)),
        requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), TypeFactory.defaultInstance()
          .constructCollectionType(List.class, LocalDate.class));
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get option expirations for '%s' because of '%s' input error with status '%d'.",
          optionExpirationQuery, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get option expirations for '%s' because of '%s' error with status '%d'.",
          optionExpirationQuery, response.body(), response.statusCode()));
    }
  }

  public List<Option> getOptions(OptionQuery optionQuery) {
    try {
      return doGetOptions(optionQuery);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new UnexpectedFailureException(String.format(
          "Can't get options for '%s' because of unexpected error.", optionQuery),
          ex);
    }
  }

  private List<Option> doGetOptions(OptionQuery optionQuery)
      throws IOException, InterruptedException {
    Objects.requireNonNull(optionQuery, "Option query should be specified.");
    Objects.requireNonNull(optionQuery.symbol(), "Symbol should be specified.");
    Objects.requireNonNull(optionQuery.expiration(), "Expiration should be specified.");
    verifyNotClosed();
    refreshAuthIfNeeded();
    Map<String, String> requestParams = new TreeMap<>();
    if (optionQuery.expiration() != null) {
      requestParams.put("expiration", DateTimeFormatter.ISO_DATE.format(optionQuery.expiration()));
    }
    if (optionQuery.series() != null) {
      requestParams.put("series", optionQuery.series());
    }
    URI uri = buildUri(String.format(
            optionsUrlPattern,
            URLEncoder.encode(optionQuery.symbol(), StandardCharsets.UTF_8)),
        requestParams);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("accept", "application/json")
        .header(AUTHORIZATION_HEADER, AUTHORIZATION_PREFIX + auth.accessToken())
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() / 100 == 2) {
      return jsonMapper.readValue(response.body(), TypeFactory.defaultInstance()
          .constructCollectionType(List.class, Option.class));
    } else if (response.statusCode() / 100 == 4) {
      throw new InvalidInputException(String.format(
          "Can't get options for '%s' because of '%s' input error with status '%d'.",
          optionQuery, response.body(), response.statusCode()));
    } else {
      throw new UnexpectedFailureException(String.format(
          "Can't get options for '%s' because of '%s' error with status '%d'.",
          optionQuery, response.body(), response.statusCode()));
    }
  }

  public void setSubscriber(AccountBalanceChangedEventSubscriber subscriber) {
    verifyNotClosed();
    AccountTextSubscriber textSubscriber =
        accountWebSocket.getTextSubscriber(AccountTextSubscriber.class);
    textSubscriber.setSubscriber(subscriber);
  }

  public void setSubscriber(AccountPositionsChangedEventSubscriber subscriber) {
    verifyNotClosed();
    AccountTextSubscriber textSubscriber =
        accountWebSocket.getTextSubscriber(AccountTextSubscriber.class);
    textSubscriber.setSubscriber(subscriber);
  }

  public void setSubscriber(AccountOrderChangedEventSubscriber subscriber) {
    verifyNotClosed();
    AccountTextSubscriber textSubscriber =
        accountWebSocket.getTextSubscriber(AccountTextSubscriber.class);
    textSubscriber.setSubscriber(subscriber);
  }

  public void setSubscriber(AccountTradeChangedEventSubscriber subscriber) {
    verifyNotClosed();
    AccountTextSubscriber textSubscriber =
        accountWebSocket.getTextSubscriber(AccountTextSubscriber.class);
    textSubscriber.setSubscriber(subscriber);
  }

  public void setSubscriber(AccountErrorEventSubscriber subscriber) {
    verifyNotClosed();
    AccountTextSubscriber textSubscriber =
        accountWebSocket.getTextSubscriber(AccountTextSubscriber.class);
    textSubscriber.setSubscriber(subscriber);
  }

  public void setSubscriber(TradeMadeEventSubscriber subscriber) {
    verifyNotClosed();
    MarketDataTextSubscriber textSubscriber =
        marketDataWebSocket.getTextSubscriber(MarketDataTextSubscriber.class);
    textSubscriber.setSubscriber(subscriber);
  }

  public void setSubscriber(QuoteChangedEventSubscriber subscriber) {
    verifyNotClosed();
    MarketDataTextSubscriber textSubscriber =
        marketDataWebSocket.getTextSubscriber(MarketDataTextSubscriber.class);
    textSubscriber.setSubscriber(subscriber);
  }

  public void setSubscriber(MarketDataErrorEventSubscriber subscriber) {
    verifyNotClosed();
    MarketDataTextSubscriber textSubscriber =
        marketDataWebSocket.getTextSubscriber(MarketDataTextSubscriber.class);
    textSubscriber.setSubscriber(subscriber);
  }

  public void subscribeForAccountBalanceChangedEvents(String accountNumber) {
    Objects.requireNonNull(accountNumber, "Account number should be specified.");
    verifyNotClosed();
    try {
      AccountSubscriptionAction subscribeAction =
          accountSubscriptionActionFactory.getAccountBalanceChangedEventAction(accountNumber, true);
      accountWebSocket.insertToStateIfAbsent(
          String.format(ACCOUNT_BALANCE_CHANGED_EVENT_STATE_PROPERTY_PATTERN, accountNumber),
          jsonMapper.writeValueAsString(subscribeAction));
    } catch (StateChangeFailedException | JsonProcessingException ex) {
      throw new UnexpectedFailureException(String.format(
          "Subscription for account '%s' balance changed events failed.", accountNumber),
          ex);
    } catch (IllegalResilientWebSocketStateException ex) {
      throw new IllegalLimeTraderClientStateException("Lime trader client is closed.", ex);
    }
  }

  public void unsubscribeFromAccountBalanceChangedEvents(String accountNumber) {
    Objects.requireNonNull(accountNumber, "Account number should be specified.");
    verifyNotClosed();
    try {
      AccountSubscriptionAction unsubscribeAction =
          accountSubscriptionActionFactory.getAccountBalanceChangedEventAction(
              accountNumber, false);
      accountWebSocket.removeFromStateWith(
          String.format(ACCOUNT_BALANCE_CHANGED_EVENT_STATE_PROPERTY_PATTERN, accountNumber),
          jsonMapper.writeValueAsString(unsubscribeAction));
    } catch (StateChangeFailedException | JsonProcessingException ex) {
      throw new UnexpectedFailureException(String.format(
          "Unsubscription from account '%s' balance changed events failed.", accountNumber),
          ex);
    } catch (IllegalResilientWebSocketStateException ex) {
      throw new IllegalLimeTraderClientStateException("Lime trader client is closed.", ex);
    }
  }

  public void subscribeForAccountPositionsChangedEvents(String accountNumber) {
    Objects.requireNonNull(accountNumber, "Account number should be specified.");
    verifyNotClosed();
    try {
      AccountSubscriptionAction subscribeAction =
          accountSubscriptionActionFactory.getAccountPositionsChangedEventAction(
              accountNumber, true);
      accountWebSocket.insertToStateIfAbsent(
          String.format(ACCOUNT_POSITIONS_CHANGED_EVENT_STATE_PROPERTY_PATTERN, accountNumber),
          jsonMapper.writeValueAsString(subscribeAction));
    } catch (StateChangeFailedException | JsonProcessingException ex) {
      throw new UnexpectedFailureException(String.format(
          "Subscription for account '%s' positions changed events failed.", accountNumber),
          ex);
    } catch (IllegalResilientWebSocketStateException ex) {
      throw new IllegalLimeTraderClientStateException("Lime trader client is closed.", ex);
    }
  }

  public void unsubscribeFromAccountPositionsChangedEvents(String accountNumber) {
    Objects.requireNonNull(accountNumber, "Account number should be specified.");
    verifyNotClosed();
    try {
      AccountSubscriptionAction unsubscribeAction =
          accountSubscriptionActionFactory.getAccountPositionsChangedEventAction(
              accountNumber, false);
      accountWebSocket.removeFromStateWith(
          String.format(ACCOUNT_POSITIONS_CHANGED_EVENT_STATE_PROPERTY_PATTERN, accountNumber),
          jsonMapper.writeValueAsString(unsubscribeAction));
    } catch (StateChangeFailedException | JsonProcessingException ex) {
      throw new UnexpectedFailureException(String.format(
          "Unsubscription from account '%s' positions changed events failed.", accountNumber),
          ex);
    } catch (IllegalResilientWebSocketStateException ex) {
      throw new IllegalLimeTraderClientStateException("Lime trader client is closed.", ex);
    }
  }

  public void subscribeForAccountOrderChangedEvents(String accountNumber) {
    Objects.requireNonNull(accountNumber, "Account number should be specified.");
    verifyNotClosed();
    try {
      AccountSubscriptionAction subscribeAction =
          accountSubscriptionActionFactory.getAccountOrderChangedEventAction(accountNumber, true);
      accountWebSocket.insertToStateIfAbsent(
          String.format(ACCOUNT_ORDER_CHANGED_EVENT_STATE_PROPERTY_PATTERN, accountNumber),
          jsonMapper.writeValueAsString(subscribeAction));
    } catch (StateChangeFailedException | JsonProcessingException ex) {
      throw new UnexpectedFailureException(String.format(
          "Subscription for account '%s' order changed events failed.", accountNumber),
          ex);
    } catch (IllegalResilientWebSocketStateException ex) {
      throw new IllegalLimeTraderClientStateException("Lime trader client is closed.", ex);
    }
  }

  public void unsubscribeFromAccountOrderChangedEvents(String accountNumber) {
    Objects.requireNonNull(accountNumber, "Account number should be specified.");
    verifyNotClosed();
    try {
      AccountSubscriptionAction unsubscribeAction =
          accountSubscriptionActionFactory.getAccountOrderChangedEventAction(
              accountNumber, false);
      accountWebSocket.removeFromStateWith(
          String.format(ACCOUNT_ORDER_CHANGED_EVENT_STATE_PROPERTY_PATTERN, accountNumber),
          jsonMapper.writeValueAsString(unsubscribeAction));
    } catch (StateChangeFailedException | JsonProcessingException ex) {
      throw new UnexpectedFailureException(String.format(
          "Unsubscription from account '%s' order changed events failed.", accountNumber),
          ex);
    } catch (IllegalResilientWebSocketStateException ex) {
      throw new IllegalLimeTraderClientStateException("Lime trader client is closed.", ex);
    }
  }

  public void subscribeForAccountTradeChangedEvents(String accountNumber) {
    Objects.requireNonNull(accountNumber, "Account number should be specified.");
    verifyNotClosed();
    try {
      AccountSubscriptionAction subscribeAction =
          accountSubscriptionActionFactory.getAccountTradeChangedEventAction(accountNumber, true);
      accountWebSocket.insertToStateIfAbsent(
          String.format(ACCOUNT_TRADE_CHANGED_EVENT_STATE_PROPERTY_PATTERN, accountNumber),
          jsonMapper.writeValueAsString(subscribeAction));
    } catch (StateChangeFailedException | JsonProcessingException ex) {
      throw new UnexpectedFailureException(String.format(
          "Subscription for account '%s' trade changed events failed.", accountNumber),
          ex);
    } catch (IllegalResilientWebSocketStateException ex) {
      throw new IllegalLimeTraderClientStateException("Lime trader client is closed.", ex);
    }
  }

  public void unsubscribeFromAccountTradeChangedEvents(String accountNumber) {
    Objects.requireNonNull(accountNumber, "Account number should be specified.");
    verifyNotClosed();
    try {
      AccountSubscriptionAction unsubscribeAction =
          accountSubscriptionActionFactory.getAccountTradeChangedEventAction(
              accountNumber, false);
      accountWebSocket.removeFromStateWith(
          String.format(ACCOUNT_TRADE_CHANGED_EVENT_STATE_PROPERTY_PATTERN, accountNumber),
          jsonMapper.writeValueAsString(unsubscribeAction));
    } catch (StateChangeFailedException | JsonProcessingException ex) {
      throw new UnexpectedFailureException(String.format(
          "Unsubscription from account '%s' trade changed events failed.", accountNumber),
          ex);
    } catch (IllegalResilientWebSocketStateException ex) {
      throw new IllegalLimeTraderClientStateException("Lime trader client is closed.", ex);
    }
  }

  public void subscribeForMarketDataEvents(Set<String> symbols) {
    Objects.requireNonNull(symbols, "Symbols should be specified.");
    if (symbols.isEmpty()) {
      throw new IllegalArgumentException("Symbols should be specified.");
    }
    verifyNotClosed();
    try {
      MarketDataSubscriptionAction subscribeAction =
          marketDataSubscriptionActionFactory.getAction(symbols, true);
      marketDataWebSocket.updateState(
          MARKET_DATA_EVENT_STATE_PROPERTY,
          jsonMapper.writeValueAsString(subscribeAction),
          oldData -> buildDataForStateTransitionDuringMarketDataSubscription(oldData, symbols));
    } catch (StateChangeFailedException | JsonProcessingException ex) {
      throw new UnexpectedFailureException(String.format(
          "Subscription for market data for '%s' symbols failed.",
          String.join(",", symbols)),
          ex);
    } catch (IllegalResilientWebSocketStateException ex) {
      throw new IllegalLimeTraderClientStateException("Lime trader client is closed.", ex);
    }
  }

  private String buildDataForStateTransitionDuringMarketDataSubscription(String oldData,
      Set<String> newSymbols) {
    if (oldData == null) {
      return null;
    }
    try {
      Set<String> oldSymbols = jsonMapper.readValue(oldData, MarketDataSubscriptionAction.class)
          .symbols();
      Set<String> symbolsToUnsubscribe = new HashSet<>(oldSymbols);
      symbolsToUnsubscribe.removeAll(newSymbols);
      if (symbolsToUnsubscribe.isEmpty()) {
        return null;
      }
      MarketDataSubscriptionAction unsubscribeAction =
          marketDataSubscriptionActionFactory.getAction(symbolsToUnsubscribe, false);
      return jsonMapper.writeValueAsString(unsubscribeAction);
    } catch (JsonProcessingException ex) {
      throw new UnexpectedFailureException(String.format(
          "If you see this message then possibly there is a bug in the lime trader client during "
              + "market data subscription. Please contact dev team. Details are: old data '%s', "
              + "new symbols '%s'.", oldData, String.join(",", newSymbols)));
    }
  }

  public void unsubscribeFromMarketDataEvents() {
    verifyNotClosed();
    try {
      marketDataWebSocket.updateState(
          MARKET_DATA_EVENT_STATE_PROPERTY,
          null,
          this::buildDataForStateTransitionDuringMarketDataUnsubscription);
    } catch (StateChangeFailedException ex) {
      throw new UnexpectedFailureException("Unsubscription from market data failed.", ex);
    } catch (IllegalResilientWebSocketStateException ex) {
      throw new IllegalLimeTraderClientStateException("Lime trader client is closed.", ex);
    }
  }

  private String buildDataForStateTransitionDuringMarketDataUnsubscription(String oldData) {
    if (oldData == null) {
      return null;
    }
    try {
      Set<String> symbolsToUnsubscribe =
          jsonMapper.readValue(oldData, MarketDataSubscriptionAction.class)
              .symbols();
      MarketDataSubscriptionAction unsubscribeAction =
          marketDataSubscriptionActionFactory.getAction(symbolsToUnsubscribe, false);
      return jsonMapper.writeValueAsString(unsubscribeAction);
    } catch (JsonProcessingException ex) {
      throw new UnexpectedFailureException(String.format(
          "If you see this message then possibly there is a bug in the lime trader client during "
              + "market data unsubscription. Please contact dev team. Details are: old data '%s'.",
          oldData));
    }
  }

  @Override
  public void close() {
    closed = true;
    accountWebSocket.close();
    marketDataWebSocket.close();
  }

  private void verifyNotClosed() {
    if (closed) {
      throw new IllegalLimeTraderClientStateException("Lime trader client is closed.");
    }
  }
}
