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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.semver4j.Semver;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.util.ThreadUtil;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentSpec;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
@ExtendWith( MockitoExtension.class )
public class DockerStackDeployerTest {

    @Mock
    InstallrdConfiguration installrdConfig;

    @SneakyThrows
    byte[] readDescriptor( String name ) {
        final Path root = Path.of(
            Thread.currentThread().getContextClassLoader().getResource( "." ).toURI()
        );
        return Files.readAllBytes( root.resolve( "deployer/docker" ).resolve( name ) );
    }

    DeploymentSpec genSpec( String name ) {
        final DeploymentSpec spec = new DeploymentSpec();
        spec.setName( name );
        spec.setVersion( Semver.parse( "0.1.0" ) );
        spec.setData( readDescriptor( name + ".yaml" ) );
        spec.setMediaType( "application/vnd.io.installr.docker-stack" );
        spec.setHash( "empty" );
        spec.setEnv( Map.of( "namespace", "Namespace1" ) );
        return spec;
    }

    DockerStackDeployer deployer() {
        return deployer( new DockerDeployerConfig() );
    }

    @SneakyThrows
    DockerStackDeployer deployer( DockerDeployerConfig config ) {
        lenient().when( installrdConfig.location() ).thenReturn( Path.of( "/tmp" ) );
        final DockerStackDeployer deployer = new DockerStackDeployer();
        deployer.init( config );
        final Field field = DockerStackDeployer.class.getDeclaredField( "installrdConfig" );
        field.setAccessible( true );
        field.set( deployer, installrdConfig );
        return deployer;
    }

    void randomSleep() {
        // To prevent 'invalid pool request: Pool overlaps with other one on this address space' we randomly wait a
        // bit before we start deploying a new stack
        ThreadUtil.sleep( new Random().nextLong( 500, 2000 ) );
    }

    @Test
    void deploy_should_throw_exception_when_image_was_not_found() {
        final DockerStackDeployer deployer = deployer();
        final DeploymentSpec spec = genSpec( "test-1" );
        try {
            randomSleep();
            final DeploymentException ex = assertThrows(
                DeploymentException.class,
                () -> deployer.deploy( spec )
            );
            log.info( ex.getMessage() );
            // Newer versions of docker respond with something like "Error response from daemon: manifest unknown".
            if ( ex.getMessage().contains( "manifest unknown" ) ) {
                assertTrue( ex.getMessage().contains( "manifest unknown" ), "manifest unknown" );
            }
            else {
                assertEquals( "No such image: ghcr.io/installr-io/uci:not-found", ex.getMessage() );
            }
        }
        finally {
            deployer.undeploy( spec );
        }
    }

    @Test
    void deploy_should_throw_exception_when_container_start_fails() {
        final DockerStackDeployer deployer = deployer();
        final DeploymentSpec spec = genSpec( "test-2" );
        try {
            randomSleep();
            final DeploymentException ex = assertThrows(
                DeploymentException.class,
                () -> deployer.deploy( spec )
            );
            assertTrue( ex.getMessage().startsWith( "starting container failed:" ) );
        }
        finally {
            deployer.undeploy( spec );
        }
    }

    @Test
    void deploy_should_throw_exception_when_not_all_services_are_running() {
        final DockerDeployerConfig config = new DockerDeployerConfig();
        config.setPollingTimeout( Duration.ofSeconds( 5 ) );
        final DockerStackDeployer deployer = deployer( config );
        final DeploymentSpec spec = genSpec( "test-3" );
        try {
            randomSleep();
            final DeploymentException ex = assertThrows(
                DeploymentException.class,
                () -> deployer.deploy( spec )
            );
            assertEquals(
                "Could not deploy 'Namespace1-test-3'. See 'docker service logs' for details.",
                ex.getMessage()
            );
        }
        finally {
            deployer.undeploy( spec );
        }
    }

    @Test
    void deploy_should_be_successful_when_all_tasks_running() {
        final DockerStackDeployer deployer = deployer();
        final DeploymentSpec spec = genSpec( "test-4" );
        try {
            randomSleep();
            assertDoesNotThrow( () -> deployer.deploy( spec ) );
        }
        finally {
            deployer.undeploy( spec );
        }
    }

    @Test
    void translateException_should_return_meaningful_message_when_socket_unreachable() {
        final DockerDeployerConfig config = new DockerDeployerConfig();
        config.setHost( "unix:///nonexistent/docker.sock" );
        final DockerStackDeployer deployer = deployer( config );

        final RuntimeException socketError = new RuntimeException( new SocketException( "No such file or directory" ) );
        final DeploymentException result = deployer.translateException( socketError );

        assertTrue(
            result.getMessage().contains( "Cannot connect to Docker" ),
            "Expected meaningful message, got: " + result.getMessage()
        );
    }

}
