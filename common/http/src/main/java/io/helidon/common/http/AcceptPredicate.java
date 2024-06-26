/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.common.http;

import java.util.function.Predicate;

/**
 * API to model HTTP content negotiation using {@code Accept-*} request headers. (RFC 7231 and RFC 2295)
 * <p>
 * It extends {@link Predicate} for smooth integration with standard functional APIs.
 *
 * @param <T> The type of the <i>Accept-*</i> header value.
 */
public interface AcceptPredicate<T> extends Predicate<T> {

    /**
     * The media type quality factor ({@value QUALITY_FACTOR_PARAMETER}) parameter name.
     */
    String QUALITY_FACTOR_PARAMETER = "q";

    /**
     * The wildcard value {@value #WILDCARD_VALUE} used by standard in several headers.
     */
    String WILDCARD_VALUE = "*";

    /**
     * Gets quality factor parameter ({@value QUALITY_FACTOR_PARAMETER}) as a double value. If missing, then returns {@code 1.0}
     *
     * @return Quality factor parameter.
     */
    double qualityFactor();

}
