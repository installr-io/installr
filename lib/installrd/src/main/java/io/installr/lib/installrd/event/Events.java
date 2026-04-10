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
package io.installr.lib.installrd.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.installr.lib.pkg.PackageId;

public class Events {

    // Transaction
    public record TransactionStarted( UUID id, Instant timestamp, Status status ) implements Event {
        public TransactionStarted( UUID id ) {
            this( id, Instant.now(), Status.Success );
        }
    }

    public record TransactionFinished( UUID id, Instant timestamp, Status status ) implements Event {
        public TransactionFinished( UUID id, Status status ) {
            this( id, Instant.now(), status );
        }
    }

    // Cache & Index
    public record PackageCacheOutdated( Instant lastUpdated, Instant timestamp, Status status )
        implements Event {
        public PackageCacheOutdated( Instant lastUpdated ) {
            this( lastUpdated, Instant.now(), Status.Success );
        }
    }

    public record PackageIndexUpdateStarted( Instant timestamp, Status status ) implements Event {
        public PackageIndexUpdateStarted() {
            this( Instant.now(), Status.Success );
        }
    }

    public record PackageIndexDownloadStarted( String repository, Instant timestamp, Status status )
        implements Event {
        public PackageIndexDownloadStarted( String repository ) {
            this( repository, Instant.now(), Status.Success );
        }
    }

    public record PackageIndexDownloadFinished( String repository, Instant timestamp, Status status )
        implements Event {
        public PackageIndexDownloadFinished( String repository, Status status ) {
            this( repository, Instant.now(), status );
        }
    }

    public record PackageIndexUpdateFinished( Instant timestamp, Status status )
        implements Event {
        public PackageIndexUpdateFinished( Status status ) {
            this( Instant.now(), status );
        }
    }

    // Resolving
    public record PackageResolvingStarted( Instant timestamp, Status status ) implements Event {
        public PackageResolvingStarted() {
            this( Instant.now(), Status.Success );
        }
    }

    public record PackageResolvingFinished(
        int totalPackages, List<PackageId> notFoundPackages, Instant timestamp, Status status
    ) implements Event {
        public PackageResolvingFinished(
            int totalPackages, List<PackageId> notFoundPackages, Status status
        ) {
            this( totalPackages, notFoundPackages, Instant.now(), status );
        }
    }

    // Download
    public record DownloadProcessStarted( int totalPackages, Instant timestamp, Status status ) implements Event {
        public DownloadProcessStarted( int totalPackages ) {
            this( totalPackages, Instant.now(), Status.Success );
        }
    }

    public record PackageDownloadStarted( PackageId packageId, Instant timestamp, Status status )
        implements Event {
        public PackageDownloadStarted( PackageId packageId ) {
            this( packageId, Instant.now(), Status.Success );
        }
    }

    public record PackageDownloadFinished( PackageId packageId, Instant timestamp, Status status )
        implements Event {
        public PackageDownloadFinished( PackageId packageId, Status status ) {
            this( packageId, Instant.now(), status );
        }
    }

    public record DownloadProcessFinished( Instant timestamp, Status status ) implements Event {
        public DownloadProcessFinished( Status status ) {
            this( Instant.now(), status );
        }
    }

    // Installation
    public record InstallationProcessStarted( int totalPackages, Instant timestamp, Status status )
        implements Event {
        public InstallationProcessStarted( int totalPackages ) {
            this( totalPackages, Instant.now(), Status.Success );
        }
    }

    public record PackageInstallationStarted(
        PackageId packageId, int packageIndex, int totalItems, Instant timestamp, Status status
    ) implements Event {
        public PackageInstallationStarted( PackageId packageId, int packageIndex, int totalItems ) {
            this( packageId, packageIndex, totalItems, Instant.now(), Status.Success );
        }
    }

    public record PackageContentDeploymentStarted(
        String itemPath, int itemIndex, Instant timestamp, Status status
    )
        implements Event {
        public PackageContentDeploymentStarted( String itemPath, int itemIndex ) {
            this( itemPath, itemIndex, Instant.now(), Status.Success );
        }
    }

    public record PackageContentDeploymentFinished(
        String itemPath, int itemIndex, Instant timestamp, Status status
    ) implements Event {
        public PackageContentDeploymentFinished( String itemPath, int itemIndex, Status status ) {
            this( itemPath, itemIndex, Instant.now(), status );
        }
    }

    public record PackageInstallationFinished(
        PackageId packageId, int packageIndex, Instant timestamp, Status status
    ) implements Event {
        public PackageInstallationFinished( PackageId packageId, int packageIndex, Status status ) {
            this( packageId, packageIndex, Instant.now(), status );
        }
    }

    public record InstallationProcessFinished( Instant timestamp, Status status ) implements Event {
        public InstallationProcessFinished( Status status ) {
            this( Instant.now(), status );
        }
    }

}
