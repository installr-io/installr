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
package io.installr.installrd.deployment.deployer.docker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.semver4j.Semver;

import de.gesellix.docker.client.DockerClient;
import de.gesellix.docker.client.DockerClientImpl;
import de.gesellix.docker.remote.api.Network;
import io.installr.installrd.deployment.DeploymentWarning;
import io.installr.lib.deployer.DeploymentSpec;
import lombok.SneakyThrows;

public class DockerNetworkDeployerTest {

    final DockerClient dockerClient = new DockerClientImpl();

    @SneakyThrows
    byte[] readDescriptor( String name ) {
        final Path root = Path.of(
            Thread.currentThread().getContextClassLoader().getResource( "." ).toURI()
        );
        return Files.readAllBytes( root.resolve( "deployer/docker" ).resolve( name ) );
    }

    DeploymentSpec genSpec( String name ) {
        final DeploymentSpec spec = new DeploymentSpec();
        spec.setName( Base64.getEncoder().encodeToString( name.getBytes() ) );
        spec.setVersion( Semver.parse( "0.1.0" ) );
        spec.setData( readDescriptor( name + ".json" ) );
        spec.setMediaType( "application/vnd.io.installr.docker-network" );
        spec.setHash( "empty" );
        spec.setEnv( Map.of( "namespace", "Namespace1" ) );
        return spec;
    }

    boolean existsNetwork( String name ) {
        final List<Network> networks = dockerClient.networks( String.format( "{ \"name\": [\"%s\"] }", name ) )
            .getContent();
        return networks != null && !networks.isEmpty();
    }

    @Test
    void deploy_should_create_an_non_existing_network() {
        final DockerNetworkDeployer deployer = new DockerNetworkDeployer( new DockerDeployerConfig() );
        final DeploymentSpec spec = genSpec( "test-network-1" );
        try {
            assertFalse( existsNetwork( "Namespace1-test-network-1" ), "Network 'test-network-1' does not exist." );
            assertDoesNotThrow( () -> deployer.deploy( spec ) );
            assertTrue( existsNetwork( "Namespace1-test-network-1" ), "Network 'test-network1' exists." );
        }
        finally {
            deployer.undeploy( spec );
        }
    }

    @Test
    void deploy_should_skip_an_existing_network() {
        final DockerNetworkDeployer deployer = new DockerNetworkDeployer( new DockerDeployerConfig() );
        final DeploymentSpec spec = genSpec( "test-network-1" );
        try {
            assertFalse( existsNetwork( "Namespace1-test-network-1" ), "Network 'test-network-1' does not exist." );
            assertDoesNotThrow( () -> deployer.deploy( spec ) );
            assertTrue( existsNetwork( "Namespace1-test-network-1" ), "Network 'test-network1' exists." );
            final DeploymentWarning ex = assertThrows(
                DeploymentWarning.class,
                () -> deployer.deploy( spec )
            );
            assertEquals( "Network 'Namespace1-test-network-1' already exists.", ex.getMessage() );
        }
        finally {
            deployer.undeploy( spec );
        }
    }

}
