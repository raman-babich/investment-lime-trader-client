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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author Raman Babich
 */
public record QuoteChangedEvent(
    @JsonProperty("s") String symbol,
    @JsonProperty("a") BigDecimal ask,
    @JsonProperty("as") Integer askSize,
    @JsonProperty("b") BigDecimal bid,
    @JsonProperty("bs") Integer bidSize,
    @JsonProperty("l") BigDecimal last,
    @JsonProperty("ls") Integer lastSize,
    @JsonProperty("v") Integer volume,
    @JsonFormat(
            shape = JsonFormat.Shape.NUMBER,
            without = JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
        @JsonProperty("d")
        Instant date,
    @JsonProperty("h") BigDecimal high,
    BigDecimal low,
    @JsonProperty("o") BigDecimal open,
    @JsonProperty("c") BigDecimal close,
    @JsonProperty("ch") BigDecimal change,
    @JsonProperty("chpc") BigDecimal changePercent,
    @JsonProperty("iv") BigDecimal impliedVolatility,
    @JsonProperty("tp") BigDecimal theoreticalPrice,
    BigDecimal delta,
    BigDecimal gamma,
    BigDecimal theta,
    BigDecimal vega) {}
