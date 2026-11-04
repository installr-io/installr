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
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logmanager.MDC;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.DeployerConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.NamespaceDefinition;
import io.installr.installrd.config.NamespaceConfiguration;
import io.installr.installrd.config.SecurityConfiguration;
import io.installr.installrd.logging.ConsoleLog;
import io.installr.installrd.plugin.PluginLoader;
import io.installr.installrd.security.Operation.Names;
import io.installr.lib.installrd.controller.DaemonController;
import io.installr.lib.installrd.model.VersionInfo;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Startup;
import io.quarkus.security.PermissionsAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path( "/" )
public class Daemon implements DaemonController {

    @Inject
    InstallrdConfiguration config;

    @Inject
    PluginLoader pluginLoader;

    @ConfigProperty( name = "quarkus.http.ssl-port" )
    int httpsPort;

    @ConfigProperty( name = "installrd.log.timezone" )
    String logTimezone;

    @ConfigProperty( name = "installrd.log.sifting-logger.log-dir" )
    String logLocation;

    @Inject
    ConsoleLog consoleLog;

    @Startup
    void startup() {
        // set 'installrd' as default "namespace", to prevent empty brackets in the logs
        MDC.put( "namespace", "installrd" );

        consoleLog.infof( "Installrd %s running on port %d (HTTPS, mTLS)\n", Installrd.version(), httpsPort );

        consoleLog.info( "Installrd:" );
        consoleLog.infof( "- Revision: %s", Installrd.revision() );
        consoleLog.infof( "- Timestamp: %s", Installrd.buildTimestamp().atZone( ZoneId.systemDefault() ) );
        consoleLog.infof( "- Timezone: %s", ZoneId.systemDefault() );

        consoleLog.info( "Config:" );
        consoleLog.infof( "- Location: %s", config.location() );

        consoleLog.info( "Security:" );
        if ( !config.accessPolicy().hasAdmins() ) {
            consoleLog.warn( "- No admins configured ('security.admins'); only role bindings grant access" );
        }
        config.security()
            .flatMap( SecurityConfiguration::admins )
            .ifPresent( admins -> admins.forEach( admin -> consoleLog.infof( "- Admin: %s", admin ) ) );
        config.security()
            .flatMap( SecurityConfiguration::roles )
            .ifPresent(
                roles -> roles.forEach(
                    role -> consoleLog.infof( "- Role: %s %s", role.name(), role.operations() )
                )
            );

        consoleLog.info( "Log:" );
        consoleLog.infof( "- Location: %s", logLocation );
        consoleLog.infof( "- Timezone: %s", logTimezone );

        consoleLog.info( "Namespaces:" );
        final List<NamespaceDefinition> namespaces = config.namespaceDefinitions();
        if ( namespaces.isEmpty() ) {
            consoleLog.info( "- No namespaces configured" );
        }
        else {
            namespaces.forEach( nd -> consoleLog.infof( "- %s", nd.name() ) );
        }

        consoleLog.info( "Repositories:" );
        config.repositories()
            .ifPresentOrElse(
                list -> list.forEach( repo -> consoleLog.infof( "- %s: %s", repo.id(), repo.url() ) ),
                () -> consoleLog.info( "- No repositories configured" )
            );

        consoleLog.info( "Deployer:" );
        if ( config.deployerConfigurations().isEmpty() ) {
            consoleLog.info( "- No deployer configured" );
        }
        logDeployerConfigurations( config.deployerConfigurations() );
        config.namespaceConfigurations()
            .entrySet()
            .stream()
            .map( Map.Entry::getValue )
            .map( NamespaceConfiguration::deployerConfigurations )
            .forEach( this::logDeployerConfigurations );
        pluginLoader.load();
    }

    private void logDeployerConfigurations( Map<String, DeployerConfiguration> deployerConfigurations ) {
        deployerConfigurations
            .forEach(
                ( k, v ) -> consoleLog.infof( "- %s: %s", k, v.type() )
            );
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
    @PermissionsAllowed( Names.DAEMON_STOP )
    public void stop() {
        Quarkus.asyncExit();
    }

}
