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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ClientConfigurationTest {

    private static final String CONFIG = """
        default-config: local
        configs:
          local:
            url: https://localhost:9316
            ca: |
              -----BEGIN CERTIFICATE-----
              ca
              -----END CERTIFICATE-----
            cert: |
              -----BEGIN CERTIFICATE-----
              cert
              -----END CERTIFICATE-----
            key: |
              -----BEGIN PRIVATE KEY-----
              key
              -----END PRIVATE KEY-----
          remote:
            url: https://installrd.example.org:9316
            cert: remote-cert
            key: remote-key
        """;

    @TempDir
    Path dir;

    @Test
    void selectShouldReturnDefaultConfig() throws IOException {
        final InstallrdConnection connection = ClientConfiguration.load( write( CONFIG ) ).select( null );
        assertEquals( "local", connection.name() );
        assertEquals( URI.create( "https://localhost:9316" ), connection.url() );
        assertTrue( connection.ca().contains( "ca" ) );
        assertTrue( connection.cert().startsWith( "-----BEGIN CERTIFICATE-----\ncert\n" ) );
        assertTrue( connection.key().startsWith( "-----BEGIN PRIVATE KEY-----\nkey\n" ) );
    }

    @Test
    void selectShouldReturnNamedConfig() throws IOException {
        final InstallrdConnection connection = ClientConfiguration.load( write( CONFIG ) ).select( "remote" );
        assertEquals( "remote", connection.name() );
        assertEquals( URI.create( "https://installrd.example.org:9316" ), connection.url() );
        assertNull( connection.ca() );
    }

    @Test
    void selectShouldFailForUnknownConfig() throws IOException {
        final ClientConfiguration configuration = ClientConfiguration.load( write( CONFIG ) );
        final InvalidClientConfigurationException ex = assertThrows(
            InvalidClientConfigurationException.class,
            () -> configuration.select( "prod" )
        );
        assertTrue( ex.getMessage().contains( "local, remote" ) );
    }

    @Test
    void selectShouldFailWithoutNameAndDefault() throws IOException {
        final ClientConfiguration configuration = ClientConfiguration.load(
            write( "configs:\n  local:\n    url: https://localhost:9316\n    cert: c\n    key: k\n" )
        );
        assertThrows( InvalidClientConfigurationException.class, () -> configuration.select( null ) );
    }

    @Test
    void selectShouldFailForHttpUrl() throws IOException {
        final ClientConfiguration configuration = ClientConfiguration.load(
            write( "configs:\n  local:\n    url: http://localhost:9316\n    cert: c\n    key: k\n" )
        );
        assertThrows( InvalidClientConfigurationException.class, () -> configuration.select( "local" ) );
    }

    @Test
    void selectShouldFailWithoutUrl() throws IOException {
        final ClientConfiguration configuration = ClientConfiguration.load(
            write( "configs:\n  local:\n    cert: c\n    key: k\n" )
        );
        assertThrows( InvalidClientConfigurationException.class, () -> configuration.select( "local" ) );
    }

    @Test
    void selectShouldFailWithoutCert() throws IOException {
        final ClientConfiguration configuration = ClientConfiguration.load(
            write( "configs:\n  local:\n    url: https://localhost:9316\n    key: k\n" )
        );
        assertThrows( InvalidClientConfigurationException.class, () -> configuration.select( "local" ) );
    }

    @Test
    void selectShouldFailWithoutKey() throws IOException {
        final ClientConfiguration configuration = ClientConfiguration.load(
            write( "configs:\n  local:\n    url: https://localhost:9316\n    cert: c\n" )
        );
        assertThrows( InvalidClientConfigurationException.class, () -> configuration.select( "local" ) );
    }

    @Test
    void loadShouldFailForMissingFile() {
        assertThrows(
            InvalidClientConfigurationException.class,
            () -> ClientConfiguration.load( dir.resolve( "missing.yaml" ) )
        );
    }

    @Test
    void loadShouldFailForEmptyFile() throws IOException {
        final Path file = write( "" );
        assertThrows( InvalidClientConfigurationException.class, () -> ClientConfiguration.load( file ) );
    }

    @Test
    void loadShouldFailForUnknownProperty() throws IOException {
        final Path file = write( "default-config: local\nconfgs: {}\n" );
        assertThrows( InvalidClientConfigurationException.class, () -> ClientConfiguration.load( file ) );
    }

    @Test
    void loadShouldFailIfReadableByOthers() throws IOException {
        assumeTrue( FileSystems.getDefault().supportedFileAttributeViews().contains( "posix" ) );
        final Path file = write( CONFIG );
        Files.setPosixFilePermissions( file, PosixFilePermissions.fromString( "rw-r--r--" ) );
        final InvalidClientConfigurationException ex = assertThrows(
            InvalidClientConfigurationException.class,
            () -> ClientConfiguration.load( file )
        );
        assertTrue( ex.getMessage().contains( "chmod 600" ) );
    }

    private Path write( String content ) throws IOException {
        final Path file = dir.resolve( "config.yaml" );
        Files.writeString( file, content );
        if ( FileSystems.getDefault().supportedFileAttributeViews().contains( "posix" ) ) {
            Files.setPosixFilePermissions( file, PosixFilePermissions.fromString( "rw-------" ) );
        }
        return file;
    }

}
