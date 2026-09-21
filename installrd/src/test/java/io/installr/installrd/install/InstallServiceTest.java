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
package io.installr.installrd.install;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class InstallServiceTest {

    private final InstallService installService = new InstallService();

    @Test
    void should_match_when_labels_are_null() {
        assertTrue( installService.matchesSelector( null, Map.of( "platform", "kubernetes" ) ) );
    }

    @Test
    void should_match_when_labels_are_empty() {
        assertTrue( installService.matchesSelector( Map.of(), Map.of( "platform", "kubernetes" ) ) );
    }

    @Test
    void should_match_when_selector_is_null() {
        assertTrue( installService.matchesSelector( Map.of( "platform", "kubernetes" ), null ) );
    }

    @Test
    void should_match_when_selector_is_empty() {
        assertTrue( installService.matchesSelector( Map.of( "platform", "kubernetes" ), Map.of() ) );
    }

    @Test
    void should_match_when_selector_equals_labels() {
        assertTrue(
            installService.matchesSelector(
                Map.of( "platform", "kubernetes" ),
                Map.of( "platform", "kubernetes" )
            )
        );
    }

    @Test
    void should_match_when_selector_is_subset_of_labels() {
        assertTrue(
            installService.matchesSelector(
                Map.of( "platform", "kubernetes", "tier", "monitoring" ),
                Map.of( "platform", "kubernetes" )
            )
        );
    }

    @Test
    void should_not_match_when_label_value_differs() {
        assertFalse(
            installService.matchesSelector(
                Map.of( "platform", "docker-swarm" ),
                Map.of( "platform", "kubernetes" )
            )
        );
    }

    @Test
    void should_not_match_when_selector_key_not_in_labels() {
        assertFalse(
            installService.matchesSelector(
                Map.of( "tier", "monitoring" ),
                Map.of( "platform", "kubernetes" )
            )
        );
    }

    @Test
    void should_not_match_when_only_some_selector_entries_match() {
        assertFalse(
            installService.matchesSelector(
                Map.of( "platform", "kubernetes", "tier", "web" ),
                Map.of( "platform", "kubernetes", "tier", "monitoring" )
            )
        );
    }

}
