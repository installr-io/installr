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
package io.installr.installrd.pkg.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.LocationType;
import io.installr.lib.pkg.InvalidPackageIdException;
import io.installr.lib.pkg.PackageId;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class PackageConfigReaderIT {

    public static final String TEST_NAMESPACE = "Namespace1";
    private static final String TEST_PACKAGE = "test-package:0.1.0";

    @Inject
    PackageConfigReader packageConfigReader;

    @Inject
    InstallrdConfiguration config;

    Path namespaceLocation( String namespace ) {
        return config.locationFor( LocationType.Namespace )
            .resolve( namespace );
    }

    @Test
    void readDefaults_should_read_all_defaults() throws InvalidPackageIdException {
        Map<String, String> defaults = packageConfigReader.readDefaults(
            namespaceLocation( TEST_NAMESPACE ),
            Map.of(),
            PackageId.valueOf( TEST_PACKAGE )
        );
        assertEquals( "foo", defaults.get( "value" ) );
        assertEquals( "bar", defaults.get( "other" ) );
    }

    @Test
    void readDefaults_should_read_all_custom_values() throws InvalidPackageIdException {
        Map<String, String> customVals = packageConfigReader.readCustom(
            namespaceLocation( TEST_NAMESPACE ),
            Map.of(),
            PackageId.valueOf( TEST_PACKAGE )
        );
        assertEquals( "foo-overwritten", customVals.get( "value" ) );
        assertNull( customVals.get( "other" ) );
    }

    @Test
    void readDefaults_should_read_all_values() throws InvalidPackageIdException {
        Map<String, String> allVals = packageConfigReader.readAll(
            namespaceLocation( TEST_NAMESPACE ),
            Map.of(),
            PackageId.valueOf( TEST_PACKAGE )
        );
        assertEquals( "foo-overwritten", allVals.get( "value" ) );
        assertEquals( "bar", allVals.get( "other" ) );
    }

    @Test
    void read_should_transform_keys_to_snake_case() throws InvalidPackageIdException {
        Map<String, String> allVals = packageConfigReader.readAll(
            namespaceLocation( TEST_NAMESPACE ),
            Map.of(),
            PackageId.valueOf( TEST_PACKAGE )
        );
        assertEquals( "baz", allVals.get( "third_value" ) );
        assertEquals( "bar", allVals.get( "other" ) );
    }

    @Test
    void readDefaults_should_read_hierarchical_values() throws InvalidPackageIdException {
        Map<String, String> allVals = packageConfigReader.readAll(
            namespaceLocation( TEST_NAMESPACE ),
            Map.of(),
            PackageId.valueOf( TEST_PACKAGE )
        );
        assertEquals( "1", allVals.get( "hierarchical.value.one" ) );
        assertEquals( "2", allVals.get( "hierarchical.value.two" ) );
        assertEquals( "3", allVals.get( "hierarchical_other" ) );
        assertEquals( "1", allVals.get( "hierarchical_value_one" ) );
        assertEquals( "2", allVals.get( "hierarchical_value_two" ) );
        assertEquals( "3", allVals.get( "hierarchical_other" ) );
    }

    @Test
    void readDefaults_should_substitute_known_keys() throws InvalidPackageIdException {
        Map<String, String> allVals = packageConfigReader.readAll(
            namespaceLocation( TEST_NAMESPACE ),
            Map.of(),
            PackageId.valueOf( TEST_PACKAGE )
        );
        assertEquals( "foo-sub", allVals.get( "value-substitution" ) );
        assertEquals( "foo-sub", allVals.get( "value_substitution" ) );
    }

    @Test
    void read_should_overwrite_default_value_by_namespace_env() throws InvalidPackageIdException {
        Map<String, String> allVals = packageConfigReader.readAll(
            namespaceLocation( TEST_NAMESPACE ),
            Map.of( "globally-overwritten", "by-namespace-env" ),
            PackageId.valueOf( TEST_PACKAGE )
        );
        assertEquals( "by-namespace-env", allVals.get( "globally-overwritten" ) );
    }

    @Test
    void read_should_throw_when_value_is_REQUIRED_but_not_for_empty() throws InvalidPackageIdException {
        assertThrows(
            MissingConfigurationException.class,
            () -> packageConfigReader.readAll(
                namespaceLocation( TEST_NAMESPACE ),
                Map.of(),
                PackageId.valueOf( "test-package-with-required-values:0.1.0" )
            )
        );
    }

}
