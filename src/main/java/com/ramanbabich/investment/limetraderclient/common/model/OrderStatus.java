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

package com.ramanbabich.investment.limetraderclient.common.model;

/**
 * @author Raman Babich
 */
public final class OrderStatus {

  public static final String NEW = "new";
  public static final String PENDING_NEW = "pending_new";
  public static final String PARTIALLY_FILLED = "partially_filled";
  public static final String FILLED = "filled";
  public static final String PENDING_CANCEL = "pending_cancel";
  public static final String CANCELED = "canceled";
  public static final String PENDING_REPLACE = "pending_replace";
  public static final String REPLACED = "replaced";
  public static final String REJECTED = "rejected";
  public static final String DONE_FOR_DAY = "done_for_day";
  public static final String EXPIRED = "expired";

  private OrderStatus() {
  }
}
