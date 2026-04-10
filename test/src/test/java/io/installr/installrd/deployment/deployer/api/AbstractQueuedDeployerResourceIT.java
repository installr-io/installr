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
package io.installr.installrd.deployment.deployer.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.installr.installrd.util.ThreadUtil;
import io.installr.lib.deployer.DeploymentResponse;
import io.installr.lib.deployer.DeploymentStatus;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AbstractQueuedDeployerResourceIT {

    static final String NAMESPACE = "Namespace1";
    static final String DEFAULT_CONTENT_TYPE = "application/vnd.io.installr.binary";
    static final String UNSUPPORTED_CONTENT_TYPE = "application/vnd.io.installr.unsupported";

    @Test
    void deploy_should_correctly_enqueue() {
        deploy( "testfile.json" );
        checkStatus( "testfile.json", 200, DeploymentStatus.InProgress );
        // put another deployment into queue
        deploy( "another-file.json" );
        checkStatus( "another-file.json", 200, DeploymentStatus.Queued );
        // now wait until both deployments are finished
        waitUntilFinished( "testfile.json", DeploymentStatus.Success );
        waitUntilFinished( "another-file.json", DeploymentStatus.Success );
    }

    @Test
    void deploy_should_remove_retired_jobs() {
        deploy( "retired.json" );
        checkStatus( "retired.json", 200, DeploymentStatus.InProgress );
        // wait "deployment time" period
        ThreadUtil.sleep( AbstractQueuedDeployerResourceMock.DEPLOY_TIME );
        checkStatus( "retired.json", 200, DeploymentStatus.Success );
        // now wait another "deployment time" period (retention = deployment time * 2)
        ThreadUtil.sleep( AbstractQueuedDeployerResourceMock.DEPLOY_TIME );
        // and put another deployment into queue (=> this cleans the retired jobs)
        deploy( "another-file.json" );
        // check status of retired.json
        checkStatus( "retired.json", 400, DeploymentStatus.Failed );
        // now wait for another-file.json to succeed
        waitUntilFinished( "another-file.json", DeploymentStatus.Success );
    }

    void deploy( String name ) {
        // @formatter:off
        given()
            .pathParam( "name", name )
            .multiPart( "version", "0.1.0" )
            .multiPart( "data", new byte[] { 1 } )
            .multiPart( "hash", 0 )
            .multiPart( "media-type", DEFAULT_CONTENT_TYPE )
            .multiPart( "encoding", "UTF-8" )
            .multiPart( "env", "{}" )
        .when()
            .put( "/test/Deployer.deploy/{name}" )
        .then()
            .log().all()
            .assertThat()
                .statusCode( 202 )
        ;
        // @formatter:on
    }

    void checkStatus( String name, int expectedCode, DeploymentStatus expectedStatus ) {
        // @formatter:off
        given()
            .pathParam( "name", name )
        .when()
            .get( "/test/Deployer.status/{name}" )
        .then()
            .log().all()
            .assertThat()
                .statusCode( expectedCode )
                .body( "status", equalTo( expectedStatus.name() ) )
        ;
        // @formatter:on
    }

    void waitUntilFinished( String name, DeploymentStatus expectedStatus ) {
        // @formatter:off
        DeploymentResponse response;
        do {
            ThreadUtil.sleep( 500 );
            response =
                given()
                    .pathParam( "name", name )
                .when()
                    .get( "/test/Deployer.status/{name}" )
                .then()
                    .log()
                        .ifError()
                    .assertThat()
                        .statusCode( 200 )
                        .extract()
                            .as( DeploymentResponse.class )
            ;
        }
        while ( !response.getStatus().isFinished() );
        assertEquals( expectedStatus, response.getStatus() );
        // @formatter:on
    }

}
