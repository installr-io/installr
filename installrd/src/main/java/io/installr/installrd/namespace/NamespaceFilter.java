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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jboss.logmanager.MDC;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.LocationType;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
class NamespaceFilter {

    @Inject
    InstallrdConfiguration config;

    @Inject
    Namespace namespace;

    @ServerRequestFilter
    void filter( ContainerRequestContext rc ) {
        final List<String> params = rc.getUriInfo().getPathParameters().get( "namespace" );
        if ( params != null && !params.isEmpty() ) {
            final String name = params.get( 0 );
            final Path location = config.locationFor( LocationType.Namespace ).resolve( name ).toAbsolutePath();
            if ( !Files.exists( location ) ) {
                throw new NamespaceNotFoundException( name );
            }
            namespace.init( name, location );
            MDC.put( "namespace", name );
            log.tracef( "Namespace '%s' initialized; location: %s", name, location );
        }
    }

}
