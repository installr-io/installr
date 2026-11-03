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
package io.installr.installrd.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MapUtilTest {

    @Test
    void mask_should_hide_values_of_sensitive_keys() {
        assertEquals( "****", MapUtil.mask( "liquibase.password", "secret" ) );
        assertEquals( "****", MapUtil.mask( "registry_api-key", "abc" ) );
        assertEquals( "****", MapUtil.mask( "Auth.Token", "abc" ) );
    }

    @Test
    void mask_should_keep_other_values() {
        assertEquals( "ns", MapUtil.mask( "namespace", "ns" ) );
        assertEquals( "", MapUtil.mask( "password", "" ) );
        assertEquals( null, MapUtil.mask( "password", null ) );
    }

}
