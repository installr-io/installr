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
package io.installr.installrd.pkg.resolver;

import static io.installr.installrd.util.Partials.not;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.installr.installrd.pkg.PackageCache;
import io.installr.installrd.pkg.PackageDatabase;
import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageId;
import io.installr.lib.pkg.PackageInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PackageResolver {

    @Inject
    PackageCache packageCache;

    @Inject
    PackageDatabase packageDatabase;

    public List<ResolverResult> resolve( List<PackageId> packageIds ) {
        return resolve( findPackages( packageIds ) );
    }

    public List<ResolverResult> resolve( Map<PackageId, Optional<Package>> packageMap ) {
        final List<PackageId> dependentPackageIds = packageMap.values().stream()
            .filter( Optional::isPresent )
            .map( Optional::get )
            .map( Package::getPackageInfo )
            .map( PackageInfo::getDependencies )
            .filter( not( Objects::isNull ) )
            .flatMap( List::stream )
            .distinct()
            .collect( Collectors.toList() );

        final Map<PackageId, Optional<Package>> searchResult = new LinkedHashMap<>(
            findPackages( dependentPackageIds )
        );
        searchResult.putAll( packageMap );

        return searchResult.entrySet().stream()
            .map( this::toResolverResult )
            .collect( Collectors.toList() );
    }

    private Map<PackageId, Optional<Package>> findPackages( List<PackageId> packageIds ) {
        return packageIds.stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    packageCache::searchIndex,
                    ( existing, duplicate ) -> existing,
                    LinkedHashMap::new
                )
            );
    }

    private ResolverResult toResolverResult( Map.Entry<PackageId, Optional<Package>> entry ) {
        if ( entry.getValue().isEmpty() ) {
            return new ResolverResult( entry.getKey(), ResolverStatus.NotFoundInIndex, null );
        }
        final Package pkg = entry.getValue().get();
        final Path pathInLocalRepository = packageCache.getPathInLocalRepository( pkg );
        final boolean foundInLocalRepository = Files.exists( pathInLocalRepository );
        final ResolverStatus status;
        if ( foundInLocalRepository ) {
            status = ResolverStatus.FoundInLocalRepository;
            pkg.setLocalPath( pathInLocalRepository );
        }
        else {
            status = ResolverStatus.FoundInIndex;
        }
        return new ResolverResult( entry.getKey(), status, pkg );
    }

}
