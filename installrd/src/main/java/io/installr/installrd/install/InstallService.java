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
package io.installr.installrd.install;

import static io.installr.installrd.util.Partials.not;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.semver4j.Semver;

import io.installr.installrd.deployment.DeploymentService;
import io.installr.installrd.deployment.DeploymentWarning;
import io.installr.installrd.pkg.PackageCache;
import io.installr.installrd.pkg.PackageDatabase;
import io.installr.installrd.pkg.TransactionCanceledException;
import io.installr.installrd.pkg.config.PackageConfigReader;
import io.installr.installrd.pkg.config.PackageDefaultsExtractor;
import io.installr.installrd.pkg.resolver.PackageResolver;
import io.installr.installrd.pkg.resolver.ResolverResult;
import io.installr.installrd.pkg.resolver.ResolverStatus;
import io.installr.installrd.plugin.PluginLoader;
import io.installr.installrd.transaction.Transaction;
import io.installr.installrd.util.MapUtil;
import io.installr.lib.deployer.DeploymentSpec;
import io.installr.lib.installrd.event.Events;
import io.installr.lib.installrd.event.Status;
import io.installr.lib.installrd.pkg.Action;
import io.installr.lib.installrd.pkg.ItemLogRecord;
import io.installr.lib.installrd.pkg.PackageStatus;
import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageContent;
import io.installr.lib.pkg.PackageId;
import io.installr.lib.pkg.PackageInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class InstallService {

    @Inject
    PackageCache packageCache;

    @Inject
    PackageResolver packageResolver;

    @Inject
    PackageDatabase packageDatabase;

    @Inject
    PluginLoader pluginLoader;

    @Inject
    PackageDefaultsExtractor defaultsExtractor;

    @Inject
    PackageConfigReader packageConfigReader;

    @Inject
    DeploymentService deploymentService;

    public void install( Transaction tx, List<PackageId> packageIds ) {
        install( tx, Action.Install, () -> packageResolver.resolve( packageIds ) );
    }

    public void installFile( Transaction tx, List<String> files ) {
        install( tx, Action.Install, () -> resolvePackagesFromFiles( files ) );
    }

    public void reinstall( Transaction tx, List<PackageId> packageIds ) {
        install( tx, Action.ReInstall, () -> packageResolver.resolve( packageIds ) );
    }

    public void reinstallFile( Transaction tx, List<String> files ) {
        install( tx, Action.ReInstall, () -> resolvePackagesFromFiles( files ) );
    }

    private List<ResolverResult> resolvePackagesFromFiles( List<String> files ) {
        return packageResolver.resolve(
            files.stream()
                .map( this::toPath )
                .map( this::toPackage )
                .map( this::extractPackage )
                .collect(
                    Collectors.toMap(
                        k -> k.getPackageInfo().getPackageId(),
                        v -> Optional.of( v )
                    )
                )
        );
    }

    private void install( Transaction tx, Action action, Supplier<List<ResolverResult>> supplier ) {
        // update cache
        updatePackageCache( tx );
        // resolve
        final Optional<List<ResolverResult>> resolved = resolvePackages( tx, action, supplier );
        if ( resolved.isEmpty() ) {
            return; // no further processing
        }
        final List<ResolverResult> selected = resolved.get();
        // download
        if ( !downloadPackages( tx, selected ) ) {
            return; // no further processing
        }
        // install
        installPackages( tx, action, selected );
    }

    private void updatePackageCache( Transaction tx ) {
        if ( packageCache.isOutdated() ) {
            tx.emitEvent( new Events.PackageCacheOutdated( packageCache.getLastUpdated() ) );
            packageCache.updatePackageIndex();
        }
    }

    private Optional<List<ResolverResult>> resolvePackages(
        Transaction tx, Action action, Supplier<List<ResolverResult>> supplier
    ) {
        tx.emitEvent( new Events.PackageResolvingStarted() );
        final List<ResolverResult> resolved = supplier.get();
        log.debug( "Resolved packages:" );
        log.debug( resolved );
        final List<PackageId> notFound = resolved.stream()
            .filter( x -> x.status().equals( ResolverStatus.NotFoundInIndex ) )
            .map( ResolverResult::packageId )
            .toList();
        final List<ResolverResult> selected = resolved.stream()
            .filter( x -> !x.status().equals( ResolverStatus.NotFoundInIndex ) )
            .filter(
                // always (re-)install non-release versions
                x -> !isReleaseVersion( x.resolvedPackage() )
                    // always re-install on Action.ReInstall
                    || action == Action.ReInstall
                    // otherwise install only when not or not successfully installed
                    || ( action == Action.Install && !packageDatabase.isSuccessfullyInstalled( x.resolvedPackage() ) )
            )
            .toList();
        if ( !notFound.isEmpty() ) {
            tx.emitEvent( new Events.PackageResolvingFinished( selected.size(), notFound, Status.Fatal ) );
            return Optional.empty(); // no further processing
        }
        if ( action == Action.ReInstall ) {
            final List<PackageId> selectedButNotInstalled = selected.stream()
                .map( ResolverResult::resolvedPackage )
                .filter( not( packageDatabase::isInstalled ) )
                .map( Package::getPackageInfo )
                .map( PackageInfo::getPackageId )
                .toList();
            if ( !selectedButNotInstalled.isEmpty() ) {
                tx.emitEvent(
                    new Events.PackageResolvingFinished(
                        selected.size(),
                        notFound,
                        Status
                            .Fatal( "Packages selected for re-install but not installed:\n%s", selectedButNotInstalled )
                    )
                );
                return Optional.empty();
            }
        }
        tx.emitEvent( new Events.PackageResolvingFinished( selected.size(), notFound, Status.Success ) );
        return Optional.of( selected );
    }

    private boolean isReleaseVersion( Package pkg ) {
        return pkg.getPackageInfo().getPackageId().version().getPreRelease().isEmpty();
    }

    private boolean downloadPackages( Transaction tx, List<ResolverResult> selected ) {
        final List<Package> toDownload = selected.stream()
            .filter( x -> x.status().equals( ResolverStatus.FoundInIndex ) )
            .map( ResolverResult::resolvedPackage )
            .toList();
        tx.emitEvent( new Events.DownloadProcessStarted( toDownload.size() ) );
        try {
            toDownload.forEach( packageCache::downloadPackage );
        }
        catch ( Throwable t ) {
            tx.emitEvent( new Events.DownloadProcessFinished( Status.Fatal ) );
            return false; // no further processing
        }
        tx.emitEvent( new Events.DownloadProcessFinished( Status.Success ) );
        return true;
    }

    private void installPackages( Transaction tx, Action action, List<ResolverResult> selected ) {
        tx.emitEvent( new Events.InstallationProcessStarted( selected.size() ) );
        final AtomicInteger counter = new AtomicInteger( 0 );
        final Status installStatus = selected.stream()
            .map( ResolverResult::resolvedPackage )
            .map( pkg -> installPackage( tx, action, pkg, counter ) )
            .reduce( Status.Success, this::aggregateStatus );
        tx.emitEvent( new Events.InstallationProcessFinished( installStatus ) );
    }

    private Status installPackage( Transaction tx, Action action, Package pkg, AtomicInteger counter ) {
        final int packageIdx = counter.incrementAndGet();
        final PackageId packageId = pkg.getPackageInfo().getPackageId();

        // retrieve installed items
        final Map<String, ItemLogRecord> installedItems = packageDatabase.getInstalledItems(
            tx.namespace().name(),
            packageId.name()
        );

        // filter contents
        final List<PackageContent> contentsToInstall = filterContents( pkg, action, installedItems );
        // apply any package content filter found via PluginLoader
        pluginLoader.getPackageContentFilters()
            .ifPresent( filters -> filters.values().forEach( filter -> filter.filter( contentsToInstall ) ) );

        // put package into database (status == InProgress)
        final Long id = packageDatabase.putPackage( tx, action, pkg );

        // extract package defaults
        defaultsExtractor.extract( tx.namespace(), pkg );

        // Prepare the environment for the deployment artifacts of this package:
        // The environment consists of the environment configured for the namespace (aka global env) and the package
        // specific configuration.
        final Map<String, String> env = tx.namespace().config().env();
        env.put( "namespace", tx.namespace().name() );
        env.putAll( packageConfigReader.readAll( tx.namespace().location(), env, packageId ) );
        log.debug( "environment:" );
        MapUtil.dump( log, env );

        // install new items
        tx.emitEvent( new Events.PackageInstallationStarted( packageId, packageIdx, contentsToInstall.size() ) );
        final AtomicReference<Status> packageStatusRef = new AtomicReference<>( Status.Success );
        // todo: needs testing!
        contentsToInstall.forEach(
            item -> {
                if ( tx.isCanceled() ) {
                    throw new TransactionCanceledException();
                }
                tx.emitEvent( new Events.PackageContentDeploymentStarted( item.getPath(), 0 ) );
                log.debugf( "%s:", item.getPath() );
                Status itemStatus = Status.Success;
                try {
                    deploymentService.deploy( toDeploymentSpec( item, pkg, env ) );
                    itemStatus = Status.Success;
                    packageStatusRef.compareAndSet( Status.Success, Status.Success );
                }
                catch ( DeploymentWarning w ) {
                    itemStatus = Status.Skipped( w.getMessage() );
                }
                catch ( Throwable t ) {
                    log.debugf( "  error: %s", t.toString() );
                    itemStatus = Status.Failed( t );
                    packageStatusRef.set( Status.Failed );
                    packageDatabase.updateStatus( id, PackageStatus.Failed );
                }
                finally {
                    packageDatabase.putItem( tx, packageId, item, !itemStatus.equals( Status.Success ) );
                    tx.emitEvent( new Events.PackageContentDeploymentFinished( item.getPath(), 0, itemStatus ) );
                }
            }
        );

        // update version of already installed items (equal hash)
        final List<PackageContent> contentsToUpdate = new ArrayList<>( pkg.getPackageInfo().getContents() );
        contentsToUpdate.removeAll( contentsToInstall );
        contentsToUpdate.stream()
            .map(
                item -> installedItems.get( item.getPath() )
            )
            .filter(
                item -> !item.getVersion().equals( packageId.version() )
            )
            .forEach(
                item -> {
                    log.debugf( "updating version of item '%s' to '%s", item.getPath(), packageId.version() );
                    packageDatabase.putItem( tx, packageId, item, false );
                }
            );

        log.debugf( "  txStatus (at end): %s", packageStatusRef.get() );
        packageDatabase.updateStatus( id, PackageStatus.fromEventStatus( packageStatusRef.get() ) );
        tx.emitEvent( new Events.PackageInstallationFinished( packageId, packageIdx, packageStatusRef.get() ) );
        return packageStatusRef.get();
    }

    private List<PackageContent> filterContents(
        final Package pkg, Action action, Map<String, ItemLogRecord> installedItems
    ) {
        return pkg.getPackageInfo()
            .getContents()
            .stream()
            .filter(
                newItem -> {
                    log.tracef( "%s:", newItem.getPath() );
                    final ItemLogRecord installedItem = installedItems.get( newItem.getPath() );
                    if ( installedItem == null ) {
                        log.trace( "=> installedItem == null (=> not yet installed)" );
                        return true;
                    }
                    final Semver instVersion = installedItem.getVersion();
                    final Semver pkgVersion = pkg.getPackageInfo().getPackageId().version();

                    log.tracef( "=> installed version = %s", instVersion );
                    log.tracef( "=> new package version = %s", pkgVersion );

                    if ( instVersion.isGreaterThan( pkgVersion ) ) {
                        log.tracef( "=> '%s' > '%s' => skip", instVersion, pkgVersion );
                        return false;
                    }
                    if ( installedItem.getHash().equals( newItem.getHash() ) ) {
                        log.trace( "=> equal hash => skip" );
                        return installedItem.hasError() || action == Action.ReInstall;
                    }
                    return true;
                }
            )
            .toList();
    }

    private DeploymentSpec toDeploymentSpec( PackageContent content, Package pkg, final Map<String, String> env )
        throws IOException {
        final PackageId packageId = pkg.getPackageInfo().getPackageId();
        final Path file = pkg.getLocalPath().getParent()
            .resolve( packageId.toPackageName() )
            .resolve( content.getLocationInArchive() );
        log.debugf( "%s", file );
        if ( !Files.exists( file ) ) {
            throw new FileNotFoundException( String.format( "File not found: %s", file ) );
        }
        final DeploymentSpec spec = new DeploymentSpec();
        spec.setName( encodeName( packageId, content.getPath() ) );
        spec.setVersion( packageId.version() );
        spec.setData( Files.readAllBytes( file ) );
        spec.setHash( content.getHash() );
        spec.setMediaType( content.getMediaType() );
        spec.setEncoding( content.getEncoding() );
        spec.setEnv( env );
        return spec;
    }

    private String encodeName( PackageId packageId, String path ) {
        return Base64.getEncoder().encodeToString( ( packageId.name() + "/" + path ).getBytes() );
    }

    private Status aggregateStatus( Status s1, Status s2 ) {
        return ( s1 == Status.Success && s2 == Status.Success )
            ? Status.Success
            : Status.Failed;
    }

    private Path toPath( String file ) {
        final Path path = Path.of( file ).toAbsolutePath();
        if ( !Files.exists( path ) ) {
            throw new IllegalStateException( String.format( "File does not exist: %s", path ) );
        }
        return path;
    }

    @SneakyThrows
    private Package toPackage( Path path ) {
        return Package.loadFromFile( path );
    }

    @SneakyThrows
    private Package extractPackage( Package pkg ) {
        final Path archive = pkg.extractArchive();
        log.debugf( "Extracted package '%s' to '%s'.", pkg, archive );
        return pkg;
    }

}
