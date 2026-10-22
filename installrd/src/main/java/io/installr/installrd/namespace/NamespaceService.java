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
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import io.installr.installrd.config.ConfigReloadedEvent;
import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.LocationType;
import io.installr.lib.installrd.model.NamespaceInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class NamespaceService {

    /*
     * Namespace names are used as directory names, so they must not contain path separators or dots.
     */
    private static final Pattern NAME_PATTERN = Pattern.compile( "^[A-Za-z][A-Za-z0-9_-]{0,63}$" );

    @Inject
    InstallrdConfiguration config;

    @Inject
    Event<ConfigReloadedEvent> reloadEvent;

    @SneakyThrows
    public NamespaceInfo create( String name ) {
        validateName( name );
        final Path location = config.locationFor( LocationType.Namespace ).resolve( name ).toAbsolutePath();
        if ( Files.exists( location ) ) {
            throw new NamespaceAlreadyExistsException( name );
        }
        Files.createDirectories( location );
        Files.writeString( location.resolve( "config.yaml" ), "" );
        reloadEvent.fire( new ConfigReloadedEvent() );
        log.infof( "Namespace '%s' created at %s", name, location );
        return new NamespaceInfo( name );
    }

    @SneakyThrows
    public void delete( String name ) {
        validateName( name );
        final Path location = config.locationFor( LocationType.Namespace ).resolve( name ).toAbsolutePath();
        Files.walk( location )
            .sorted( Comparator.reverseOrder() )
            .forEach( this::deleteSilently );
        reloadEvent.fire( new ConfigReloadedEvent() );
        log.infof( "Namespace '%s' deleted", name );
    }

    public static boolean isValidName( String name ) {
        return name != null && NAME_PATTERN.matcher( name ).matches();
    }

    private static void validateName( String name ) {
        if ( !isValidName( name ) ) {
            throw new BadRequestException(
                String.format( "Invalid namespace name '%s'; allowed pattern: %s", name, NAME_PATTERN.pattern() )
            );
        }
    }

    @SneakyThrows
    private void deleteSilently( Path path ) {
        Files.delete( path );
    }

    public NamespaceInfo get( String name ) {
        return new NamespaceInfo( name );
    }

    public List<NamespaceInfo> list() {
        return config.namespaceDefinitions().stream()
            .map( nd -> new NamespaceInfo( nd.name() ) )
            .toList();
    }

}
