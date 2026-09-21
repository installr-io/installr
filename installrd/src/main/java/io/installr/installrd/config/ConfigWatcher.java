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
package io.installr.installrd.config;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.installr.installrd.config.InstallrdConfiguration.LocationType;
import io.installr.installrd.logging.ConsoleLog;
import io.quarkus.runtime.Shutdown;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import lombok.SneakyThrows;

@ApplicationScoped
public class ConfigWatcher {

    @Inject
    InstallrdConfiguration config;

    @Inject
    Event<ConfigReloadedEvent> reloadEvent;

    @Inject
    ConsoleLog consoleLog;

    private final Map<WatchKey, Path> keyDirMap = new ConcurrentHashMap<>();
    private WatchService watchService;
    private ExecutorService executor;

    @Startup
    @SneakyThrows
    void start() {
        final Path configLocation = config.location().toAbsolutePath();
        watchService = configLocation.getFileSystem().newWatchService();
        registerDir( configLocation );

        final Path namespacesDir = config.locationFor( LocationType.Namespace ).toAbsolutePath();
        if ( Files.exists( namespacesDir ) ) {
            registerDir( namespacesDir );
            Files.walk( namespacesDir, 1, FileVisitOption.FOLLOW_LINKS )
                .filter( Files::isDirectory )
                .filter( path -> !path.equals( namespacesDir ) )
                .forEach( this::registerDir );
        }

        executor = Executors.newSingleThreadExecutor(
            runnable -> {
                final Thread daemonThread = new Thread( runnable, "config-watcher" );
                daemonThread.setDaemon( true );
                return daemonThread;
            }
        );
        executor.submit( this::watch );
    }

    @Shutdown
    void stop() {
        executor.shutdownNow();
    }

    @SneakyThrows
    void registerDir( Path dir ) {
        if ( !Files.exists( dir ) || !Files.isDirectory( dir ) ) {
            return;
        }
        final WatchKey key = dir.register( watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE );
        keyDirMap.put( key, dir );
        consoleLog.debugf( "Watching: %s", dir );
    }

    private void watch() {
        try {
            while ( !Thread.currentThread().isInterrupted() ) {
                final WatchKey key = watchService.take();
                boolean relevant = false;

                for ( final WatchEvent<?> event : key.pollEvents() ) {
                    final Path dir = keyDirMap.get( key );
                    if ( dir == null ) {
                        continue;
                    }
                    @SuppressWarnings( "unchecked" )
                    final Path path = dir.resolve( ( (WatchEvent<Path>)event ).context() );

                    if ( event.kind() == ENTRY_CREATE && Files.isDirectory( path ) ) {
                        registerDir( path );
                    }

                    if ( path.getFileName().toString().equals( "config.yaml" ) || Files.isDirectory( path ) ) {
                        relevant = true;
                    }
                }
                key.reset();

                if ( relevant ) {
                    consoleLog.infof( "Configuration change detected, reloading..." );
                    reloadEvent.fire( new ConfigReloadedEvent() );
                }
            }
        }
        catch ( InterruptedException ex ) {
            Thread.currentThread().interrupt();
        }
        finally {
            try {
                watchService.close();
            }
            catch ( IOException ex ) {
                consoleLog.debugf( "Error closing WatchService: %s", ex.getMessage() );
            }
        }
    }

}
