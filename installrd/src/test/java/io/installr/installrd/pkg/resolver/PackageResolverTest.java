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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.installr.installrd.pkg.PackageCache;
import io.installr.installrd.pkg.PackageDatabase;
import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageId;
import io.installr.lib.pkg.PackageInfo;

@ExtendWith( MockitoExtension.class )
public class PackageResolverTest {

    @Mock
    PackageCache packageCache;

    @Mock
    PackageDatabase packageDatabase;

    @InjectMocks
    PackageResolver resolver;

    private static Package makePackage( PackageId id ) {
        return makePackage( id, null );
    }

    private static Package makePackage( PackageId id, List<PackageId> dependencies ) {
        final PackageInfo info = new PackageInfo();
        info.setPackageId( id );
        info.setDependencies( dependencies );
        return new Package( info, null, null, null );
    }

    @Test
    void resolve_should_list_dependencies_before_meta_package() {
        final PackageId idA = new PackageId( "pkg-a", "1.0.0" );
        final PackageId idB = new PackageId( "pkg-b", "1.0.0" );
        final PackageId idC = new PackageId( "pkg-c", "1.0.0" );
        final PackageId idMeta = new PackageId( "meta-pkg", "1.0.0" );

        final Package pkgA = makePackage( idA );
        final Package pkgB = makePackage( idB );
        final Package pkgC = makePackage( idC );
        final Package metaPkg = makePackage( idMeta, List.of( idA, idB, idC ) );

        Mockito.when( packageCache.searchIndex( idMeta ) ).thenReturn( Optional.of( metaPkg ) );
        Mockito.when( packageCache.searchIndex( idA ) ).thenReturn( Optional.of( pkgA ) );
        Mockito.when( packageCache.searchIndex( idB ) ).thenReturn( Optional.of( pkgB ) );
        Mockito.when( packageCache.searchIndex( idC ) ).thenReturn( Optional.of( pkgC ) );
        Mockito.when( packageCache.getPathInLocalRepository( Mockito.any() ) )
            .thenReturn( Path.of( "/nonexistent" ) );

        final List<ResolverResult> results = resolver.resolve( List.of( idMeta ) );

        assertEquals( 4, results.size() );
        assertEquals( idA, results.get( 0 ).packageId(), "pkg-a should come first (dependency)" );
        assertEquals( idB, results.get( 1 ).packageId(), "pkg-b should come second (dependency)" );
        assertEquals( idC, results.get( 2 ).packageId(), "pkg-c should come third (dependency)" );
        assertEquals( idMeta, results.get( 3 ).packageId(), "meta-pkg should come last" );
    }

}
