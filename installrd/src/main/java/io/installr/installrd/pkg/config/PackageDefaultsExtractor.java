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
package io.installr.installrd.pkg.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.LocationType;
import io.installr.installrd.namespace.Namespace;
import io.installr.lib.pkg.Package;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class PackageDefaultsExtractor {

    @Inject
    InstallrdConfiguration config;

    public void extract( final Namespace namespace, final Package pkg ) {
        try {
            final byte[] defaults = pkg.readPackageDefaults();
            if ( defaults != null ) {
                final Path targetLocation = config.locationFor( LocationType.Namespace ).toAbsolutePath()
                    .resolve( Path.of( namespace.name(), pkg.getPackageInfo().getPackageId().name() ) );
                if ( !Files.exists( targetLocation ) ) {
                    Files.createDirectories( targetLocation );
                }
                final Path file = targetLocation.resolve( Package.PACKAGE_DEFAULTS_YAML );
                Files.write( file, defaults );
                log.debugf( "'%s' wrote to '%s'.", Package.PACKAGE_DEFAULTS_YAML, targetLocation );
            }
        }
        catch ( IOException ex ) {
            ex.printStackTrace();
            throw new IllegalStateException( ex );
        }
    }

}
