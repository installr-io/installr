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
package io.installr.lib.pkg;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.yaml.snakeyaml.LoaderOptions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.installr.lib.util.FileUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Package {

    public static final String EXTENSION = ".ipf";
    public static final String PACKAGE_INFO_YAML = "package-info.yaml";
    public static final String PACKAGE_DEFAULTS_YAML = "defaults.yaml";
    private static final String DEFAULT_REPOSITORY_ID = "@local";

    private PackageInfo packageInfo;
    private URI uri;
    @JsonIgnore
    private Path localPath;
    @JsonIgnore
    private String repositoryId;

    private static final YAMLMapper yamlMapper;
    static {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit( 100 * 1024 * 1024 ); // 100 MB
        yamlMapper = new YAMLMapper(
            YAMLFactory.builder()
                .loaderOptions( loaderOptions )
                .build()
        );
    }

    public static Package loadFromFile( Path path ) throws IOException {
        if ( path.getFileName().toString().endsWith( EXTENSION ) ) {
            return loadArchive( Files.readAllBytes( path ), path );
        }
        if ( Files.isDirectory( path ) ) {
            return loadFromFile( path.resolve( PACKAGE_INFO_YAML ) );
        }
        return loadFile( Files.readAllBytes( path ), path );
    }

    private static Package loadArchive( final byte[] archiveData, Path path ) throws IOException {
        try (
            TarArchiveInputStream tis = new TarArchiveInputStream(
                new GzipCompressorInputStream(
                    new BufferedInputStream(
                        new ByteArrayInputStream( archiveData )
                    )
                )
            )
        ) {
            TarArchiveEntry entry;
            while ( ( entry = tis.getNextEntry() ) != null ) {
                if ( entry.getName().equals( PACKAGE_INFO_YAML ) ) {
                    final PackageInfo pkgInfo = yamlMapper
                        .readValue( tis, PackageInfo.class );
                    return new Package( pkgInfo, null, path, DEFAULT_REPOSITORY_ID );
                }
            }
            throw new IOException( "Illegal package format" );
        }
    }

    private static Package loadFile( byte[] data, Path path ) throws IOException {
        final PackageInfo pkgInfo = yamlMapper
            .readValue( data, PackageInfo.class );
        return new Package( pkgInfo, null, path, DEFAULT_REPOSITORY_ID );
    }

    public Path extractArchive( Path targetBaseDir ) throws IOException {
        final Path targetDir = targetBaseDir.resolve( packageInfo.getPackageId().toPackageName() );
        FileUtil.rmdir( targetDir );
        FileUtil.mkdir( targetDir );
        try (
            TarArchiveInputStream tis = new TarArchiveInputStream(
                new GzipCompressorInputStream(
                    new BufferedInputStream(
                        Files.newInputStream( localPath )
                    )
                )
            )
        ) {
            TarArchiveEntry entry = null;
            while ( ( entry = tis.getNextEntry() ) != null ) {
                if ( !tis.canReadEntryData( entry ) ) {
                    throw new IOException( "Cannot read tar entry: " + entry.getName() );
                }
                final Path currPath = targetDir.resolve( entry.getName() );
                if ( entry.isDirectory() ) {
                    FileUtil.mkdir( currPath );
                }
                else {
                    Path parent = currPath.getParent();
                    FileUtil.mkdir( parent );
                    try ( OutputStream os = Files.newOutputStream( currPath ) ) {
                        tis.transferTo( os );
                    }
                }
            }
        }
        return targetDir;
    }

    public byte[] readPackageDefaults() throws IOException {
        if ( Files.isDirectory( localPath ) ) {
            final Path file = localPath.resolve( PACKAGE_DEFAULTS_YAML );
            if ( Files.exists( file ) ) {
                return Files.readAllBytes( file );
            }
            return null;
        }
        try (
            TarArchiveInputStream tis = new TarArchiveInputStream(
                new GzipCompressorInputStream(
                    new BufferedInputStream(
                        Files.newInputStream( localPath )
                    )
                )
            )
        ) {
            TarArchiveEntry entry = null;
            while ( ( entry = tis.getNextEntry() ) != null ) {
                if ( !PACKAGE_DEFAULTS_YAML.equals( entry.getName() ) ) {
                    continue;
                }
                return tis.readAllBytes();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.packageInfo.getPackageId().toString();
    }

}
