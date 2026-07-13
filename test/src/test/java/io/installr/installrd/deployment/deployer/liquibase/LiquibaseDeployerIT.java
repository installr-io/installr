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
package io.installr.installrd.deployment.deployer.liquibase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import io.installr.installrd.InstallrClient;
import io.installr.lib.installrd.event.Event;
import io.installr.lib.installrd.event.Events.PackageInstallationFinished;
import io.installr.lib.installrd.event.Events.PackageInstallationStarted;
import io.installr.lib.installrd.event.Status;
import io.installr.lib.installrd.resource.InstallPackageResource;
import io.installr.lib.pkg.PackageId;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class LiquibaseDeployerIT {

    private static final String NAMESPACE = "Namespace1";
    private static final PackageId PACKAGE_ID = PackageId.valueOf( "liquibase-test-package:0.1.0" );
    private static final String DB_URL = "jdbc:h2:mem:liquibase_it;DB_CLOSE_DELAY=-1";

    @Inject
    @RestClient
    InstallrClient installrClient;

    @Test
    void install_should_apply_all_three_liquibase_changelogs() throws SQLException {
        final Map<Class<?>, List<Event>> events = install();

        final PackageInstallationStarted started = (PackageInstallationStarted)events
            .get( PackageInstallationStarted.class ).getFirst();
        assertEquals( 3, started.totalItems() );

        final PackageInstallationFinished finished = (PackageInstallationFinished)events
            .get( PackageInstallationFinished.class ).getFirst();
        assertEquals( Status.Success, finished.status() );

        assertTrue( tableExists( "PERSON" ) );
        assertTrue( tableExists( "ADDRESS" ) );
        assertTrue( tableExists( "CUSTOMER_ORDER" ) );
    }

    private Map<Class<?>, List<Event>> install() {
        final InstallPackageResource resource = new InstallPackageResource();
        resource.setPackageIds( List.of( PACKAGE_ID ) );
        return installrClient
            .install( NAMESPACE, resource )
            .subscribe()
            .asStream()
            .collect( Collectors.groupingBy( Event::getClass ) );
    }

    private boolean tableExists( String tableName ) throws SQLException {
        try (
            Connection conn = DriverManager.getConnection( DB_URL, "sa", "" );
            ResultSet rs = conn.getMetaData().getTables( null, null, tableName, null )
        ) {
            return rs.next();
        }
    }

}
