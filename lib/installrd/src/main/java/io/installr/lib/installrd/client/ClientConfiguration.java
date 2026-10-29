/*
 * Installr.io
 *
 * Copyright (c) 2025-present Dorian Vallant
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.installr.lib.installrd.client;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * Configuration file of an installrd client, containing named connections:
 *
 * <pre>
 * default-config: local
 * configs:
 *   local:
 *     url: https://localhost:9316
 *     ca: |
 *       -----BEGIN CERTIFICATE-----
 *       ...
 *     cert: |
 *       -----BEGIN CERTIFICATE-----
 *       ...
 *     key: |
 *       -----BEGIN PRIVATE KEY-----
 *       ...
 * </pre>
 *
 * Since the file contains private keys, it must not be accessible by group or others.
 */
public record ClientConfiguration(
    @JsonProperty( "default-config" ) String defaultConfig,
    @JsonProperty( "configs" ) Map<String, ConnectionConfiguration> configs
) {

    private static final Set<PosixFilePermission> FORBIDDEN_PERMISSIONS = EnumSet.of(
        PosixFilePermission.GROUP_READ,
        PosixFilePermission.GROUP_WRITE,
        PosixFilePermission.GROUP_EXECUTE,
        PosixFilePermission.OTHERS_READ,
        PosixFilePermission.OTHERS_WRITE,
        PosixFilePermission.OTHERS_EXECUTE
    );

    private static final YAMLMapper MAPPER = new YAMLMapper();

    public ClientConfiguration {
        configs = configs == null ? Map.of() : configs;
    }

    public static ClientConfiguration load( Path file ) {
        if ( !Files.isRegularFile( file ) ) {
            throw new InvalidClientConfigurationException( "Configuration file %s not found.", file );
        }
        checkPermissions( file );
        final ClientConfiguration configuration;
        try {
            configuration = MAPPER.readValue( file.toFile(), ClientConfiguration.class );
        }
        catch ( IOException ex ) {
            throw new InvalidClientConfigurationException( ex, "Could not read %s: %s", file, ex.getMessage() );
        }
        if ( configuration == null ) {
            throw new InvalidClientConfigurationException( "Configuration file %s is empty.", file );
        }
        return configuration;
    }

    /**
     * Returns the validated connection with the given name, or the {@code default-config} if {@code name} is
     * {@code null}.
     */
    public InstallrdConnection select( String name ) {
        final String selected = name != null ? name : defaultConfig;
        if ( selected == null || selected.isBlank() ) {
            throw new InvalidClientConfigurationException( "No config name given and no 'default-config' set." );
        }
        final ConnectionConfiguration configuration = configs.get( selected );
        if ( configuration == null ) {
            throw new InvalidClientConfigurationException(
                "Unknown config '%s' (available: %s).",
                selected,
                configs.isEmpty() ? "none" : String.join( ", ", configs.keySet() )
            );
        }
        return configuration.toConnection( selected );
    }

    private static void checkPermissions( Path file ) {
        if ( !FileSystems.getDefault().supportedFileAttributeViews().contains( "posix" ) ) {
            return;
        }
        final Set<PosixFilePermission> permissions;
        try {
            permissions = Files.getPosixFilePermissions( file );
        }
        catch ( IOException ex ) {
            throw new InvalidClientConfigurationException( ex, "Could not read %s: %s", file, ex.getMessage() );
        }
        if ( permissions.stream().anyMatch( FORBIDDEN_PERMISSIONS::contains ) ) {
            throw new InvalidClientConfigurationException(
                "%s contains private keys and must only be accessible by its owner; run 'chmod 600 %s'.",
                file,
                file
            );
        }
    }

}
