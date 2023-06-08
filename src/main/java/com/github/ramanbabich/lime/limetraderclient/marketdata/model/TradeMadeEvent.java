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

package com.github.ramanbabich.lime.limetraderclient.marketdata.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author Raman Babich
 */
public record TradeMadeEvent(
    @JsonProperty("s") String symbol,
    @JsonProperty("ls") Integer lastSize,
    @JsonProperty("lm") String lastMarket,
    @JsonProperty("l") BigDecimal last,
    @JsonFormat(
        shape = JsonFormat.Shape.NUMBER,
        without = JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
    @JsonProperty("d") Instant date) {

}
