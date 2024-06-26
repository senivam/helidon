/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

package io.helidon.common.reactive;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsNull.nullValue;

public class MultiTakeWhilePublisherTest {

    @Test
    public void error() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Multi.<Integer>error(new IOException())
                .takeWhile(v -> true)
                .subscribe(ts);

        assertThat(ts.getItems().isEmpty(), is(true));
        assertThat(ts.getLastError(), instanceOf(IOException.class));
        assertThat(ts.isComplete(), is(false));
    }

    @Test
    public void limited() {

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Multi.just(1, 2, 3, 4, 5)
                .takeWhile(v -> v < 4)
                .subscribe(ts);

        ts.requestMax();

        assertThat(ts.getItems(), contains(1, 2, 3));
        assertThat(ts.getLastError(), is(nullValue()));
        assertThat(ts.isComplete(), is(true));
    }

    @Test
    public void predicateCrash() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Multi.<Integer>singleton(1)
                .takeWhile(v -> { throw new IllegalArgumentException();})
                .subscribe(ts);

        ts.requestMax();

        assertThat(ts.getItems().isEmpty(), is(true));
        assertThat(ts.getLastError(), instanceOf(IllegalArgumentException.class));
        assertThat(ts.isComplete(), is(false));
    }
}
