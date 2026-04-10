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
package io.installr.lib.registry.repository;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageId;
import io.installr.lib.pkg.PackageInfo;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
@Data
@Accessors( fluent = true )
public class RepositoryIndex {

    private static final String PACKAGES_PATH = "packages";

    private final String repositoryId;
    private final Path repoPath;
    private final Path indexFile;
    private final Path packagePath;
    private final URI baseUri;
    private final URI packagesUri;
    private final Map<PackageId, Package> packages;

    @Getter( AccessLevel.NONE )
    private final ReentrantLock mutex = new ReentrantLock();

    public RepositoryIndex(
        @NonNull final String repositoryId, @NonNull final Path basePath, @NonNull final URI baseUri
    ) {
        this.repositoryId = repositoryId;
        this.repoPath = createRepoPath( basePath.resolve( repositoryId ) );
        this.indexFile = repoPath.resolve( "index.gz" );
        this.packagePath = repoPath.resolve( PACKAGES_PATH );
        this.baseUri = baseUri;
        this.packagesUri = URI.create( baseUri.toString() + "/" + PACKAGES_PATH + "/" );
        final RepositoryIndexReader indexReader = new RepositoryIndexReader( repositoryId, indexFile, packagePath );
        this.packages = new TreeMap<>( indexReader.readIndex() );
        log.debug( "RepositoryIndex created" );
        log.debugf( "  repoPath: %s", repoPath );
        log.debugf( "  baseUri: %s", baseUri );
    }

    public Package get( @NonNull final PackageId packageId ) {
        return packages.get( packageId );
    }

    public void put( @NonNull final Package pkg ) {
        mutex.lock();
        try {
            final Package copy = copyPackage( pkg );
            packages.put( copy.getPackageInfo().getPackageId(), copy );
            writeIndex();
        }
        catch ( IOException ex ) {
            log.errorf( ex.toString() );
            throw new IllegalStateException( ex );
        }
        finally {
            mutex.unlock();
        }
    }

    public void rm( @NonNull final PackageId packageId ) {
        mutex.lock();
        try {
            Files.delete( packages.get( packageId ).getLocalPath() );
            packages.remove( packageId );
            writeIndex();
        }
        catch ( IOException ex ) {
            log.errorf( ex.toString() );
            throw new IllegalStateException( ex );
        }
        finally {
            mutex.unlock();
        }

    }

    public int packageCount() {
        return packages.size();
    }

    private Path createRepoPath( final Path repoPath ) {
        if ( Files.exists( repoPath ) ) {
            checkIsDirectory( repoPath );
            return repoPath;
        }
        checkIsDirectory( repoPath.getParent() );
        try {
            Files.createDirectory( repoPath );
        }
        catch ( IOException ex ) {
            throw new IllegalStateException( ex );
        }
        return repoPath;
    }

    private void checkIsDirectory( final Path directory ) {
        if ( !Files.isDirectory( directory ) ) {
            throw new IllegalArgumentException( String.format( "'%s' is not a directory.", directory ) );
        }
    }

    private void writeIndex() throws IOException {
        log.debugf( "write index: %s", indexFile );
        try ( GzipCompressorOutputStream os = new GzipCompressorOutputStream( Files.newOutputStream( indexFile ) ) ) {
            new YAMLMapper()
                .enable( SerializationFeature.INDENT_OUTPUT )
                .writeValue( os, packages().values() );
        }
        log.debug( "index written" );
    }

    private Package copyPackage( final Package pkg ) throws IOException {
        final PackageInfo pkgInfo = pkg.getPackageInfo();
        final PackageId packageId = pkgInfo.getPackageId();
        final Package copy = new Package(
            pkgInfo,
            URI.create( packagesUri.toString() + packageId ),
            packagePath.resolve( packageId.toArchiveName() ),
            repositoryId
        );

        if ( !Files.exists( packagePath ) ) {
            Files.createDirectory( packagePath );
        }
        Files.deleteIfExists( copy.getLocalPath() );
        log.debugf( "copying package from '%s' to '%s'", pkg.getLocalPath(), copy.getLocalPath() );
        Files.copy( pkg.getLocalPath(), copy.getLocalPath() );
        Files.delete( pkg.getLocalPath() );
        return copy;
    }

}
