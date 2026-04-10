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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.installr.lib.installrd.event.Events.DownloadProcessFinished;
import io.installr.lib.installrd.event.Events.DownloadProcessStarted;
import io.installr.lib.installrd.event.Events.InstallationProcessFinished;
import io.installr.lib.installrd.event.Events.InstallationProcessStarted;
import io.installr.lib.installrd.event.Events.PackageCacheOutdated;
import io.installr.lib.installrd.event.Events.PackageContentDeploymentFinished;
import io.installr.lib.installrd.event.Events.PackageContentDeploymentStarted;
import io.installr.lib.installrd.event.Events.PackageDownloadFinished;
import io.installr.lib.installrd.event.Events.PackageDownloadStarted;
import io.installr.lib.installrd.event.Events.PackageIndexDownloadFinished;
import io.installr.lib.installrd.event.Events.PackageIndexDownloadStarted;
import io.installr.lib.installrd.event.Events.PackageIndexUpdateFinished;
import io.installr.lib.installrd.event.Events.PackageIndexUpdateStarted;
import io.installr.lib.installrd.event.Events.PackageInstallationFinished;
import io.installr.lib.installrd.event.Events.PackageInstallationStarted;
import io.installr.lib.installrd.event.Events.PackageResolvingFinished;
import io.installr.lib.installrd.event.Events.PackageResolvingStarted;
import io.installr.lib.installrd.event.Events.TransactionFinished;
import io.installr.lib.installrd.event.Events.TransactionStarted;

// Basis: jedes Event hat einen timestamp
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes(
    {
        @JsonSubTypes.Type( TransactionStarted.class ),
        @JsonSubTypes.Type( TransactionFinished.class ),
        @JsonSubTypes.Type( PackageCacheOutdated.class ),
        @JsonSubTypes.Type( PackageIndexUpdateStarted.class ),
        @JsonSubTypes.Type( PackageIndexDownloadStarted.class ),
        @JsonSubTypes.Type( PackageIndexDownloadFinished.class ),
        @JsonSubTypes.Type( PackageIndexUpdateFinished.class ),
        @JsonSubTypes.Type( PackageResolvingStarted.class ),
        @JsonSubTypes.Type( PackageResolvingFinished.class ),
        @JsonSubTypes.Type( DownloadProcessStarted.class ),
        @JsonSubTypes.Type( PackageDownloadStarted.class ),
        @JsonSubTypes.Type( PackageDownloadFinished.class ),
        @JsonSubTypes.Type( DownloadProcessFinished.class ),
        @JsonSubTypes.Type( InstallationProcessStarted.class ),
        @JsonSubTypes.Type( PackageInstallationStarted.class ),
        @JsonSubTypes.Type( PackageContentDeploymentStarted.class ),
        @JsonSubTypes.Type( PackageContentDeploymentFinished.class ),
        @JsonSubTypes.Type( PackageInstallationFinished.class ),
        @JsonSubTypes.Type( InstallationProcessFinished.class ),
    }
)
public sealed interface Event permits
    TransactionStarted,
    TransactionFinished,
    PackageCacheOutdated,
    PackageIndexUpdateStarted,
    PackageIndexDownloadStarted,
    PackageIndexDownloadFinished,
    PackageIndexUpdateFinished,
    PackageResolvingStarted,
    PackageResolvingFinished,
    DownloadProcessStarted,
    PackageDownloadStarted,
    PackageDownloadFinished,
    DownloadProcessFinished,
    InstallationProcessStarted,
    PackageInstallationStarted,
    PackageContentDeploymentStarted,
    PackageContentDeploymentFinished,
    PackageInstallationFinished,
    InstallationProcessFinished {

    Instant timestamp();

    Status status();
}
