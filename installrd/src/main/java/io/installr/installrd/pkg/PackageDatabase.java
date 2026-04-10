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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.semver4j.Semver;

import io.installr.installrd.logstore.ItemLog;
import io.installr.installrd.logstore.PackageLog;
import io.installr.installrd.namespace.Namespace;
import io.installr.installrd.transaction.Transaction;
import io.installr.lib.installrd.pkg.Action;
import io.installr.lib.installrd.pkg.ItemLogRecord;
import io.installr.lib.installrd.pkg.PackageLogRecord;
import io.installr.lib.installrd.pkg.PackageStatus;
import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageContent;
import io.installr.lib.pkg.PackageId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PackageDatabase {

    @Inject
    PackageLog pkgLog;

    @Inject
    ItemLog itemLog;

    @Inject
    Namespace namespace;

    public Long putPackage( Transaction transaction, Action action, Package pkg ) {
        return pkgLog.put(
            new PackageLogRecord(
                null,
                transaction.namespace().name(),
                pkg.getPackageInfo().getPackageId().name(),
                pkg.getPackageInfo(),
                pkg.getRepositoryId(),
                action,
                transaction.id(),
                Instant.now(),
                PackageStatus.InProgress
            )
        )
            .getId();
    }

    public Optional<PackageLogRecord> getInstalledPackageLog( String namespace, String packageName ) {
        Optional<List<PackageLogRecord>> packages = pkgLog.getByPackagename( namespace, packageName );
        if ( packages.isEmpty() ) {
            return Optional.empty();
        }
        // get all packages which were not deleted, sort them by their installation time descending and return the
        // first result
        return packages.get()
            .stream()
            .filter( x -> x.getStatus() != PackageStatus.InProgress && x.getAction() != Action.Delete )
            .sorted( ( e1, e2 ) -> e2.getActionTimestamp().compareTo( e1.getActionTimestamp() ) )
            .findFirst();
    }

    public void updateStatus( Long id, PackageStatus status ) {
        final PackageLogRecord pkgLogRecord = pkgLog.get( id );
        pkgLogRecord.setStatus( status );
        pkgLog.put( pkgLogRecord );
    }

    public void putItem( Transaction transaction, PackageId packageId, PackageContent content, boolean error ) {
        final ItemLogRecord item = new ItemLogRecord();
        item.setNamespace( transaction.namespace().name() );
        item.setPackageName( packageId.name() );
        item.setPath( content.getPath() );
        item.setVersion( packageId.version() );
        item.setMediaType( content.getMediaType() );
        item.setEncoding( content.getEncoding() );
        item.setHash( content.getHash() );
        item.setInstalledBy( transaction.id() );
        item.setInstalledOn( Instant.now() );
        item.hasError( error );
        itemLog.put( item );
    }

    public Map<String, ItemLogRecord> getInstalledItems( String namespace, String packageName ) {
        return itemLog.getByPackagename( namespace, packageName );
    }

    public boolean isInstalled( final Package pkg ) {
        return getPackageStatus( pkg ) != PackageStatus.NotInstalled;
    }

    public boolean isSuccessfullyInstalled( final Package pkg ) {
        return getPackageStatus( pkg ) == PackageStatus.Success;
    }

    public PackageStatus getPackageStatus( final Package pkg ) {
        final PackageId packageId = pkg.getPackageInfo().getPackageId();
        // get installed version of the package
        final Optional<PackageLogRecord> installedPackageLog = getInstalledPackageLog(
            namespace.name(),
            packageId.name()
        );
        // check version
        if ( installedPackageLog.isPresent() ) {
            final Semver newVersion = packageId.version();
            final Semver instVersion = installedPackageLog.get().getPackageInfo().getPackageId().version();
            if ( newVersion.isLowerThanOrEqualTo( instVersion ) ) {
                return installedPackageLog.get().getStatus();
            }
        }
        return PackageStatus.NotInstalled;
    }

}
