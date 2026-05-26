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
package io.installr.installrd;

import java.time.ZoneId;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logmanager.MDC;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.NamespaceDefinition;
import io.installr.installrd.plugin.PluginLoader;
import io.installr.lib.installrd.controller.DaemonController;
import io.installr.lib.installrd.model.VersionInfo;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Startup;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import lombok.extern.jbosslog.JBossLog;

@Path( "/" )
@JBossLog( topic = "console-log" )
public class Daemon implements DaemonController {

    @Inject
    InstallrdConfiguration config;

    @Inject
    PluginLoader pluginLoader;

    @ConfigProperty( name = "quarkus.http.port" )
    int httpPort;

    @Startup
    void startup() {
        // set 'installrd' as default "namespace", to prevent empty brackets in the logs
        MDC.put( "namespace", "installrd" );

        log.infof( "Installrd %s running on port %d\n", Installrd.version(), httpPort );

        log.info( "Installrd:" );
        log.infof( "- Revision: %s", Installrd.revision() );
        log.infof( "- Timestamp: %s", Installrd.buildTimestamp().atZone( ZoneId.systemDefault() ) );
        log.infof( "- Timezone: %s", ZoneId.systemDefault() );

        log.info( "Config:" );
        log.infof( "- Location: %s", config.location() );

        log.info( "Namespaces:" );
        final List<NamespaceDefinition> namespaces = config.namespaceDefinitions();
        if ( namespaces.isEmpty() ) {
            log.info( "- No namespaces configured" );
        }
        else {
            namespaces.forEach( nd -> log.infof( "- %s", nd.name() ) );
        }

        log.info( "Repositories:" );
        config.repositories()
            .ifPresentOrElse(
                list -> list.forEach( repo -> log.infof( "- %s: %s", repo.id(), repo.url() ) ),
                () -> log.info( "- No repositories configured" )
            );

        log.info( "Deployer:" );
        if ( config.deployerConfigurations().isEmpty() ) {
            log.info( "- No deployer configured" );
        }
        else {
            config.deployerConfigurations()
                .forEach(
                    ( k, v ) -> log.infof( "- %s: %s", k, v.type() )
                );
        }
        // currently disabled
        // pluginLoader.load();
    }

    @Override
    public String version() {
        return String.format( "%s-%s", Installrd.version(), Installrd.revision() );
    }

    @Override
    public VersionInfo versionInfo() {
        return Installrd.versionInfo();
    }

    @POST
    @Path( "/stop" )
    public void stop() {
        Quarkus.asyncExit();
    }

}
