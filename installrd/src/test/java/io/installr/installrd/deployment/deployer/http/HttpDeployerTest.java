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
package io.installr.installrd.deployment.deployer.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Duration;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.installr.installrd.deployment.DeploymentWarning;
import io.installr.lib.deployer.AbstractConfigurableDeployer;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentResponse;
import io.installr.lib.deployer.DeploymentSpec;
import io.installr.lib.deployer.DeploymentStatus;
import jakarta.ws.rs.NotFoundException;

@ExtendWith( MockitoExtension.class )
class HttpDeployerTest {

    @Mock
    private HttpDeployerClient client;

    @InjectMocks
    private HttpDeployer deployer;

    private DeploymentSpec spec;

    @BeforeEach
    void setUp() throws Exception {
        HttpDeployerConfig config = new HttpDeployerConfig();
        config.setPollingInterval( Duration.ofMillis( 100 ) );
        config.setPollingTimeout( Duration.ofMillis( 500 ) );

        Field configField = AbstractConfigurableDeployer.class.getDeclaredField( "config" );
        configField.setAccessible( true );
        configField.set( deployer, config );

        spec = new DeploymentSpec();
        spec.setName( "test-app" );
    }

    @Test
    void deploy_sync_success_should_not_throw() {
        when( client.deploy( any() ) ).thenReturn( syncResponse( DeploymentStatus.Success ) );
        assertDoesNotThrow( () -> deployer.deploy( spec ) );
    }

    @Test
    void deploy_sync_warning_should_throw_deployment_warning() {
        when( client.deploy( any() ) ).thenReturn( syncResponse( DeploymentStatus.Warning ) );
        assertThrows( DeploymentWarning.class, () -> deployer.deploy( spec ) );
    }

    @Test
    void deploy_sync_failed_should_throw_deployment_exception() {
        when( client.deploy( any() ) ).thenReturn( syncResponse( DeploymentStatus.Failed ) );
        assertThrows( DeploymentException.class, () -> deployer.deploy( spec ) );
    }

    @Test
    void deploy_sync_queued_should_throw_illegal_state() {
        when( client.deploy( any() ) ).thenReturn( syncResponse( DeploymentStatus.Queued ) );
        assertThrows( IllegalStateException.class, () -> deployer.deploy( spec ) );
    }

    @Test
    void deploy_sync_in_progress_should_throw_illegal_state() {
        when( client.deploy( any() ) ).thenReturn( syncResponse( DeploymentStatus.InProgress ) );
        assertThrows( IllegalStateException.class, () -> deployer.deploy( spec ) );
    }

    @Test
    void deploy_async_should_poll_until_success() {
        when( client.deploy( any() ) ).thenReturn( asyncResponse() );
        when( client.status( any() ) )
            .thenReturn( statusResponse( DeploymentStatus.InProgress ) )
            .thenReturn( statusResponse( DeploymentStatus.Success ) );
        assertDoesNotThrow( () -> deployer.deploy( spec ) );
    }

    @Test
    void deploy_async_should_throw_on_warning() {
        when( client.deploy( any() ) ).thenReturn( asyncResponse() );
        when( client.status( any() ) ).thenReturn( statusResponse( DeploymentStatus.Warning ) );
        assertThrows( DeploymentWarning.class, () -> deployer.deploy( spec ) );
    }

    @Test
    void deploy_async_should_throw_on_failed() {
        when( client.deploy( any() ) ).thenReturn( asyncResponse() );
        when( client.status( any() ) ).thenReturn( statusResponse( DeploymentStatus.Failed ) );
        assertThrows( DeploymentException.class, () -> deployer.deploy( spec ) );
    }

    @Test
    void deploy_async_should_throw_on_timeout() {
        when( client.deploy( any() ) ).thenReturn( asyncResponse() );
        when( client.status( any() ) ).thenReturn( statusResponse( DeploymentStatus.InProgress ) );
        assertThrows( DeploymentException.class, () -> deployer.deploy( spec ) );
    }

    @Test
    void deploy_should_retry_and_succeed_after_not_found() {
        when( client.deploy( any() ) )
            .thenThrow( new NotFoundException() )
            .thenReturn( syncResponse( DeploymentStatus.Success ) );
        assertDoesNotThrow( () -> deployer.deploy( spec ) );
    }

    @Test
    void deploy_should_throw_when_service_stays_not_found() {
        when( client.deploy( any() ) ).thenThrow( new NotFoundException() );
        assertThrows( DeploymentException.class, () -> deployer.deploy( spec ) );
    }

    @Test
    void deploy_should_translate_exception_to_deployment_exception() {
        when( client.deploy( any() ) ).thenThrow( new RuntimeException( "connection refused" ) );
        DeploymentException ex = assertThrows( DeploymentException.class, () -> deployer.deploy( spec ) );
        assertEquals( "connection refused", ex.getMessage() );
    }

    @Test
    void async_status_poll_should_translate_exception_to_deployment_exception() {
        when( client.deploy( any() ) ).thenReturn( asyncResponse() );
        when( client.status( any() ) ).thenThrow( new RuntimeException( "timeout" ) );
        DeploymentException ex = assertThrows( DeploymentException.class, () -> deployer.deploy( spec ) );
        assertEquals( "timeout", ex.getMessage() );
    }

    @Test
    void undeploy_sync_success_should_not_throw() {
        when( client.undeploy( any() ) ).thenReturn( syncResponse( DeploymentStatus.Success ) );
        assertDoesNotThrow( () -> deployer.undeploy( spec ) );
    }

    private static RestResponse<DeploymentResponse> syncResponse( DeploymentStatus status ) {
        return RestResponse.ok(
            DeploymentResponse.builder().status( status ).message( "test" ).build()
        );
    }

    private static RestResponse<DeploymentResponse> asyncResponse() {
        return RestResponse.accepted(
            DeploymentResponse.builder().status( DeploymentStatus.Queued ).build()
        );
    }

    private static RestResponse<DeploymentResponse> statusResponse( DeploymentStatus status ) {
        return RestResponse.ok(
            DeploymentResponse.builder().status( status ).message( "test" ).build()
        );
    }

}
