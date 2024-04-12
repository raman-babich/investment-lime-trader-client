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

package com.ramanbabich.investment.limetraderclient.marketdata.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author Raman Babich
 */
public record Quote(
    String symbol,
    BigDecimal ask,
    Integer askSize,
    BigDecimal bid,
    Integer bidSize,
    BigDecimal last,
    Integer lastSize,
    Integer volume,
    Instant date,
    BigDecimal high,
    BigDecimal low,
    BigDecimal open,
    BigDecimal close,
    BigDecimal week52High,
    BigDecimal week52Low,
    BigDecimal change,
    @JsonProperty("change_pc") BigDecimal changePercent,
    BigDecimal openInterest,
    BigDecimal impliedVolatility,
    BigDecimal theoreticalPrice,
    BigDecimal delta,
    BigDecimal gamma,
    BigDecimal theta,
    BigDecimal vega) {}
