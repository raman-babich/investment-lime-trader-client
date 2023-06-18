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

package com.ramanbabich.investment.limetraderclient.account.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Raman Babich
 */
public record AccountTransactionList(
    List<Transaction> transactions,
    Integer count) {

  public record Transaction(
      String id,
      String type,
      String description,
      LocalDate date,
      Asset asset,
      Cash cash,
      List<Fee> fees,
      String status) {

    public record Asset(
        String symbol,
        String symbolDescription,
        Integer quantity,
        BigDecimal price) {

    }

    public record Cash(
        BigDecimal grossAmount,
        BigDecimal netAmount) {

    }

    public record Fee(
        String name,
        BigDecimal amount) {

    }

  }

}
