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
package io.installr.installrd.namespace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.installr.installrd.config.ConfigReloadedEvent;
import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.LocationType;
import io.installr.installrd.config.InstallrdConfiguration.NamespaceDefinition;
import io.installr.lib.installrd.model.NamespaceInfo;
import jakarta.enterprise.event.Event;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith( MockitoExtension.class )
class NamespaceServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    InstallrdConfiguration config;

    @Mock
    Event<ConfigReloadedEvent> reloadEvent;

    @InjectMocks
    NamespaceService namespaceService;

    // --- create ---

    @Test
    void create_should_create_namespace_directory() {
        when( config.locationFor( LocationType.Namespace ) ).thenReturn( tempDir );

        namespaceService.create( "alpha" );

        assertTrue( Files.isDirectory( tempDir.resolve( "alpha" ) ) );
    }

    @Test
    void create_should_create_config_yaml() {
        when( config.locationFor( LocationType.Namespace ) ).thenReturn( tempDir );

        namespaceService.create( "alpha" );

        assertTrue( Files.exists( tempDir.resolve( "alpha" ).resolve( "config.yaml" ) ) );
    }

    @Test
    void create_should_fire_reload_event() {
        when( config.locationFor( LocationType.Namespace ) ).thenReturn( tempDir );

        namespaceService.create( "alpha" );

        verify( reloadEvent ).fire( any( ConfigReloadedEvent.class ) );
    }

    @Test
    void create_should_return_namespace_info() {
        when( config.locationFor( LocationType.Namespace ) ).thenReturn( tempDir );

        final NamespaceInfo result = namespaceService.create( "alpha" );

        assertEquals( new NamespaceInfo( "alpha" ), result );
    }

    @Test
    void create_should_throw_when_namespace_already_exists() throws IOException {
        when( config.locationFor( LocationType.Namespace ) ).thenReturn( tempDir );
        Files.createDirectories( tempDir.resolve( "alpha" ) );

        assertThrows( NamespaceAlreadyExistsException.class, () -> namespaceService.create( "alpha" ) );
    }

    // --- delete ---

    @Test
    void delete_should_remove_namespace_directory() throws IOException {
        when( config.locationFor( LocationType.Namespace ) ).thenReturn( tempDir );
        Files.createDirectories( tempDir.resolve( "beta" ) );
        Files.writeString( tempDir.resolve( "beta" ).resolve( "config.yaml" ), "" );

        namespaceService.delete( "beta" );

        assertFalse( Files.exists( tempDir.resolve( "beta" ) ) );
    }

    @Test
    void delete_should_remove_directory_with_contents() throws IOException {
        when( config.locationFor( LocationType.Namespace ) ).thenReturn( tempDir );
        final Path nsDir = tempDir.resolve( "beta" );
        Files.createDirectories( nsDir );
        Files.writeString( nsDir.resolve( "config.yaml" ), "deployer: {}" );
        Files.writeString( nsDir.resolve( "data.txt" ), "some data" );

        namespaceService.delete( "beta" );

        assertFalse( Files.exists( nsDir ) );
    }

    @Test
    void delete_should_fire_reload_event() throws IOException {
        when( config.locationFor( LocationType.Namespace ) ).thenReturn( tempDir );
        Files.createDirectories( tempDir.resolve( "beta" ) );

        namespaceService.delete( "beta" );

        verify( reloadEvent ).fire( any( ConfigReloadedEvent.class ) );
    }

    // --- get ---

    @Test
    void get_should_return_namespace_info() {
        final NamespaceInfo result = namespaceService.get( "alpha" );

        assertEquals( new NamespaceInfo( "alpha" ), result );
    }

    // --- list ---

    @Test
    void list_should_return_namespace_info_for_each_definition() {
        when( config.namespaceDefinitions() ).thenReturn(
            List.of( (NamespaceDefinition)() -> "alpha", () -> "beta" )
        );

        final List<NamespaceInfo> result = namespaceService.list();

        assertEquals( List.of( new NamespaceInfo( "alpha" ), new NamespaceInfo( "beta" ) ), result );
    }

    @Test
    void list_should_return_empty_list_when_no_namespaces_defined() {
        when( config.namespaceDefinitions() ).thenReturn( List.of() );

        final List<NamespaceInfo> result = namespaceService.list();

        assertTrue( result.isEmpty() );
    }

}
