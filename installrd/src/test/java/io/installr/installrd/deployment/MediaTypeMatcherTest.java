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
package io.installr.installrd.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class MediaTypeMatcherTest {

    @Test
    void matcher_should_find_a_direct_match() {
        assertTrue(
            MediaTypeMatcher.matcher(
                Set.of(
                    "application/vnd.io.installr.noop",
                    "application/vnd.io.installr.binary",
                    "application/vnd.io.installr.foo",
                    "application/vnd.io.installr.*"
                ),
                "application/vnd.io.installr.binary"
            ).matches()
        );
    }

    @Test
    void matcher_should_find_a_wildcard_match() {
        assertEquals(
            "application/vnd.io.installr.*",
            MediaTypeMatcher.matcher(
                Set.of(
                    "application/vnd.io.installr.noop",
                    "application/vnd.io.installr.binary",
                    "application/vnd.io.installr.foo",
                    "application/vnd.io.installr.*"
                ),
                "application/vnd.io.installr.bar"
            ).mediaType()
        );
    }

    @Test
    void matcher_should_find_no_match() {
        assertFalse(
            MediaTypeMatcher.matcher(
                Set.of(
                    "application/vnd.io.installr.noop",
                    "application/vnd.io.installr.binary",
                    "application/vnd.io.installr.foo"
                    // , "application/vnd.io.installr.*"
                ),
                "application/vnd.io.installr.bar"
            ).matches()
        );
    }

    @Test
    void matcher_should_find_a_wildcard_match_2() {
        assertEquals(
            "application/vnd.io.installr.kc-*",
            MediaTypeMatcher.matcher(
                Set.of(
                    "application/vnd.io.installr.noop",
                    "application/vnd.io.installr.binary",
                    "application/vnd.io.installr.foo",
                    "application/vnd.io.installr.kc-*",
                    "application/vnd.io.installr.*"
                ),
                "application/vnd.io.installr.kc-client"
            ).mediaType()
        );
    }

}
