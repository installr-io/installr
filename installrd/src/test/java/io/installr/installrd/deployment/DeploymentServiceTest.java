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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.DeployerConfiguration;
import io.installr.installrd.plugin.PluginLoader;
import io.installr.lib.deployer.DeploymentSpec;

@ExtendWith( MockitoExtension.class )
class DeploymentServiceTest {

    private static final String NOOP_MEDIA_TYPE = "application/vnd.io.installr.noop";
    private static final String BINARY_MEDIA_TYPE = "application/vnd.io.installr.binary";

    @Mock
    InstallrdConfiguration config;

    @Mock
    PluginLoader pluginLoader;

    @InjectMocks
    DeploymentService service;

    @Test
    void deploy_should_route_to_noop_deployer() {
        when( config.deployerConfigurations() ).thenReturn(
            Map.of(
                NOOP_MEDIA_TYPE,
                deployerConfig( "NoOpDeployer", Map.of() )
            )
        );
        service.init();

        assertDoesNotThrow( () -> service.deploy( spec( NOOP_MEDIA_TYPE ) ) );
    }

    @Test
    void undeploy_should_route_to_noop_deployer() {
        when( config.deployerConfigurations() ).thenReturn(
            Map.of(
                NOOP_MEDIA_TYPE,
                deployerConfig( "NoOpDeployer", Map.of() )
            )
        );
        service.init();

        assertDoesNotThrow( () -> service.undeploy( spec( NOOP_MEDIA_TYPE ) ) );
    }

    @Test
    void deploy_should_route_to_local_file_deployer( @TempDir Path tempDir ) {
        when( config.deployerConfigurations() ).thenReturn(
            Map.of(
                BINARY_MEDIA_TYPE,
                deployerConfig( "LocalFileDeployer", Map.of( "directory", tempDir.toString() ) )
            )
        );
        service.init();

        final DeploymentSpec spec = spec( BINARY_MEDIA_TYPE );
        spec.setData( "some content".getBytes() );
        assertDoesNotThrow( () -> service.deploy( spec ) );
        assertTrue( Files.exists( tempDir.resolve( "test-artifact" ) ) );
    }

    @Test
    void deploy_should_throw_when_media_type_not_supported() {
        when( config.deployerConfigurations() ).thenReturn( Map.of() );
        service.init();

        assertThrows(
            MediaTypeNotSupportedException.class,
            () -> service.deploy( spec( "application/vnd.io.installr.unknown" ) )
        );
    }

    @Test
    void init_should_throw_when_deployer_type_is_unknown_and_no_plugins_present() {
        when( config.deployerConfigurations() ).thenReturn(
            Map.of(
                NOOP_MEDIA_TYPE,
                deployerConfig( "UnknownDeployer", Map.of() )
            )
        );
        when( pluginLoader.getDeployers() ).thenReturn( Optional.empty() );

        assertThrows( IllegalStateException.class, () -> service.init() );
    }

    @Test
    void deploy_should_support_wildcard_media_type_matching() {
        when( config.deployerConfigurations() ).thenReturn(
            Map.of(
                "application/vnd.io.installr.*",
                deployerConfig( "NoOpDeployer", Map.of() )
            )
        );
        service.init();

        assertDoesNotThrow( () -> service.deploy( spec( "application/vnd.io.installr.binary" ) ) );
    }

    private DeploymentSpec spec( String mediaType ) {
        final DeploymentSpec spec = new DeploymentSpec();
        spec.setName( "test-artifact" );
        spec.setMediaType( mediaType );
        return spec;
    }

    private DeployerConfiguration deployerConfig( String type, Map<String, String> cfg ) {
        return new DeployerConfiguration() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public Map<String, String> config() {
                return cfg;
            }
        };
    }

}
