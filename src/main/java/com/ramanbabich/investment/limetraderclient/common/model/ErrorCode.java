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
public final class ErrorCode {

  public static final String NOT_FOUND = "not_found";
  public static final String VALIDATION_ERROR = "validation_error";
  public static final String API_ERROR = "api_error";

  private ErrorCode() {}
}
