/*
 * Copyright (c) 2017, 2021 Oracle and/or its affiliates.
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

package io.helidon.config.tests.mappers1;

import java.util.Locale;
import java.util.logging.Logger;

import io.helidon.config.Config;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Copy of {@link MapperServicesEnabledTest} that overrides Config Mappers loaded on class/module-path.
 */
public class MapperServicesEnabledOverrideTest extends AbstractMapperServicesTest {

    @Override
    protected Config.Builder configBuilder() {
        return super.configBuilder()
                .addMapper(Logger.class, new MyLoggerConfigMapper())
                .addMapper(Locale.class, new MyLocaleConfigMapper());
    }

    @Test
    public void testLogger() {
        assertThat(getLogger().getName(), is("TEST.io.helidon.config.tests.mappers1.MapperServicesEnabledOverrideTest"));
    }

    @Test
    public void testLocale() {
        assertThat(getLocale(), is(new Locale("TEST:cs", "TEST:CZ", "TEST:Praha")));
    }

}
