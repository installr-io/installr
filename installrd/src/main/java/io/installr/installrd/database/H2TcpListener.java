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
package io.installr.installrd.database;

import java.sql.SQLException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.h2.tools.Server;

import io.installr.installrd.config.InstallrdConfiguration;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class H2TcpListener {

    @Inject
    InstallrdConfiguration config;

    @ConfigProperty( name = "installrd.database-listener.enabled", defaultValue = "true" )
    boolean enabled;

    @ConfigProperty( name = "installrd.database-listener.port" )
    int port;

    private Server server;

    @Startup
    void start() {
        if ( !enabled ) {
            return;
        }
        try {
            server = Server.createTcpServer(
                "-tcpPort",
                String.valueOf( port ),
                "-tcpAllowOthers",
                "-baseDir",
                config.location().toAbsolutePath().toString()
            ).start();
            log.debugf( "Started H2 TCP listener on port %d", port );
        }
        catch ( SQLException e ) {
            log.errorf( "Failed to start H2 TCP listener on port %d: %s", port, e.getMessage() );
        }
    }

    @PreDestroy
    void stop() {
        if ( server != null && server.isRunning( false ) ) {
            server.stop();
        }
    }

}
