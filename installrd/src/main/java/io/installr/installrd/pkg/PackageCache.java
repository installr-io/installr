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
package io.installr.installrd.pkg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.PackageCacheConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.RepositoryConfiguration;
import io.installr.installrd.namespace.Namespace;
import io.installr.installrd.transaction.Transaction;
import io.installr.installrd.transaction.TransactionService;
import io.installr.lib.installrd.event.Events;
import io.installr.lib.installrd.event.Status;
import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageId;
import io.installr.lib.registry.client.RepositoryClient;
import io.installr.lib.registry.repository.RepositoryIndexReader;
import io.installr.lib.util.FileUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class PackageCache {

    private static final String UPDATE_FILENAME = "last-updated";

    private final ReentrantLock mutex = new ReentrantLock();

    @Inject
    InstallrdConfiguration config;

    @Inject
    TransactionService txService;

    @Inject
    Namespace namespace;

    public boolean isOutdated() {
        return isOutdated( getMinExpiresAfter() );
    }

    private boolean isOutdated( Duration expiresAfter ) {
        final Path updateFile = getUpdateFilePath();
        if ( !Files.exists( updateFile ) ) {
            return true;
        }
        return getLastUpdated( updateFile ).isBefore( Instant.now().minus( expiresAfter ) );
    }

    private boolean isOutdated( RepositoryConfiguration repo ) {
        final Duration expiresAfter = repo.packageCache().isPresent()
            ? repo.packageCache().get().expiresAfter()
            : config.packageCache().expiresAfter();
        return isOutdated( expiresAfter );
    }

    private Path getUpdateFilePath() {
        return packageCacheLocation().resolve( UPDATE_FILENAME );
    }

    private Path packageCacheLocation() {
        return namespace.location()
            .resolve( ".pkg-cache" );
    }

    private Duration getMinExpiresAfter() {
        final List<Duration> durations = new ArrayList<>(
            unwrapRepositories().stream()
                .map( RepositoryConfiguration::packageCache )
                .filter( Optional::isPresent )
                .map( Optional::get )
                .map( PackageCacheConfiguration::expiresAfter )
                .toList()
        );
        durations.add( config.packageCache().expiresAfter() );
        return durations.stream()
            .min( Duration::compareTo )
            .get();
    }

    @SneakyThrows
    private Instant getLastUpdated( Path path ) {
        return Files.getLastModifiedTime( path ).toInstant();
    }

    public Instant getLastUpdated() {
        final Path updateFile = getUpdateFilePath();
        if ( !Files.exists( updateFile ) ) {
            return null;
        }
        return getLastUpdated( getUpdateFilePath() );
    }

    @SneakyThrows
    public void updatePackageIndex() {
        mutex.lock();
        final Transaction tx = txService.currentTransaction();
        try {
            tx.emitEvent( new Events.PackageIndexUpdateStarted() );
            final List<RepositoryConfiguration> repositories = unwrapRepositories();
            final AtomicInteger errors = new AtomicInteger( 0 );
            repositories.forEach(
                repo -> {
                    if ( !repo.enabled() ) {
                        log.infof( "repo '%s' not enabled...", repo.name() );
                        return;
                    }
                    if ( !isOutdated( repo ) ) {
                        return;
                    }
                    try {
                        tx.emitEvent( new Events.PackageIndexDownloadStarted( repo.name() ) );
                        final RepositoryClient repoClient = buildRepoClient( repo );
                        final Path repoDir = packageCacheLocation()
                            .resolve( repo.id() );
                        if ( Files.exists( repoDir ) ) {
                            FileUtil.rmdir( repoDir );
                        }
                        FileUtil.mkdir( repoDir );
                        repoClient.downloadIndex( repoDir );
                        tx.emitEvent( new Events.PackageIndexDownloadFinished( repo.name(), Status.Success ) );
                    }
                    catch ( Throwable t ) {
                        errors.incrementAndGet();
                        tx.emitEvent( new Events.PackageIndexDownloadFinished( repo.name(), Status.Failed( t ) ) );
                    }
                }
            );
            if ( errors.get() > 0 ) {
                tx.emitEvent(
                    new Events.PackageIndexUpdateFinished(
                        Status.Fatal( "'%d repositories could not be updated.", errors.get() )
                    )
                );
            }
            else {
                final Path updateFile = packageCacheLocation().resolve( UPDATE_FILENAME );
                Files.deleteIfExists( updateFile );
                Files.createFile( updateFile );
                tx.emitEvent( new Events.PackageIndexUpdateFinished( Status.Success ) );
            }
        }
        finally {
            mutex.unlock();
        }
    }

    private List<RepositoryConfiguration> unwrapRepositories() {
        return config.repositories()
            .orElseThrow( () -> new IllegalStateException( "No repositories configured." ) );
    }

    private RepositoryClient buildRepoClient( RepositoryConfiguration repo ) {
        if ( repo.auth().isPresent() ) {
            return new RepositoryClient(
                repo.url(),
                repo.auth().get().username(),
                repo.auth().get().password()
            );
        }
        return new RepositoryClient( repo.url() );
    }

    @SneakyThrows
    public Optional<Package> searchIndex( PackageId packageId ) {
        return unwrapRepositories().stream()
            .map( this::indexForRepo )
            .map( RepositoryIndexReader::readIndex )
            .filter( x -> x.containsKey( packageId ) )
            .map( x -> x.get( packageId ) )
            .findFirst();
    }

    private RepositoryIndexReader indexForRepo( RepositoryConfiguration repo ) {
        final Path basePath = packageCacheLocation().resolve( repo.id() );
        final Path indexFile = basePath.resolve( "index.gz" );
        final Path packagePath = basePath.resolve( "packages" );
        return new RepositoryIndexReader( repo.id(), indexFile, packagePath );
    }

    public Path getPathInLocalRepository( Package pkg ) {
        return pkg.getLocalPath().getParent()
            .resolve( pkg.getPackageInfo().getPackageId().toPackageName() );
    }

    @SneakyThrows
    public void downloadPackage( Package pkg ) {
        mutex.lock();
        final Transaction tx = txService.currentTransaction();
        tx.emitEvent( new Events.PackageDownloadStarted( pkg.getPackageInfo().getPackageId() ) );
        Status status = Status.Success;
        try {
            final RepositoryConfiguration repo = unwrapRepositories().stream()
                .filter( x -> pkg.getRepositoryId().equals( x.id() ) )
                .findFirst()
                .orElseThrow( IllegalStateException::new );
            final RepositoryClient repoClient = buildRepoClient( repo );
            final Path dest = packageCacheLocation()
                .resolve( repo.id() )
                .resolve( "packages" );
            FileUtil.mkdir( dest );
            repoClient.downloadPackage( dest, pkg.getPackageInfo().getPackageId() );
            final Path pkgDir = pkg.extractArchive();
            Files.delete( pkg.getLocalPath() );
            pkg.setLocalPath( pkgDir );
            status = Status.Success;
        }
        catch ( Throwable t ) {
            status = Status.Failed( t.toString() );
        }
        finally {
            tx.emitEvent(
                new Events.PackageDownloadFinished( pkg.getPackageInfo().getPackageId(), status )
            );
            mutex.unlock();
        }
    }

}
