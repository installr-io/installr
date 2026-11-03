/*
 * Installr.io
 *
 * Copyright (c) 2025-present Dorian Vallant
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.installr.installrd.util.interpolation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class EnvironmentInterpolatorTest {

    private final EnvironmentInterpolator interpolator = new EnvironmentInterpolator();

    @Test
    void interpolate_should_replace_variables_and_defaults() {
        assertEquals(
            "name: ns-app, port: 8080",
            interpolator.interpolate( "name: ${namespace}-app, port: ${port:-8080}", Map.of( "namespace", "ns" ) )
        );
    }

    @Test
    void interpolate_should_insert_dollar_and_backslash_literally() {
        assertEquals(
            "password: pa$$w0rd\\1",
            interpolator.interpolate( "password: ${password}", Map.of( "password", "pa$$w0rd\\1" ) )
        );
    }

}
