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
package io.installr.lib.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public final class FileUtil {

    private FileUtil() {
    }

    public static boolean isExecutable( byte[] b ) throws IOException {
        if ( b == null || b.length < 3 ) {
            return false;
        }
        return ( b[ 0 ] == 'E' && b[ 1 ] == 'L' && b[ 2 ] == 'F' )
            || ( b[ 0 ] == '#' && b[ 1 ] == '!' && b[ 2 ] == '/' );
    }

    public static void chmod( Path file, int mode ) throws IOException {
        Files.setPosixFilePermissions( file, FileUtil.toPosixFilePermission( mode ) );
    }

    private static Set<PosixFilePermission> toPosixFilePermission( int mode ) {
        // bitmasks
        final int r = 0b100;
        final int w = 0b010;
        final int x = 0b001;

        if ( mode < 0 || mode > 777 ) {
            throw new IllegalArgumentException( "invalid octal mode" );
        }
        final Set<PosixFilePermission> perms = new HashSet<>();
        final String[] smode = String.format( "%03d", mode ).split( "" );
        if ( perm( smode, 0, r ) ) {
            perms.add( PosixFilePermission.OWNER_READ );
        }
        if ( perm( smode, 0, w ) ) {
            perms.add( PosixFilePermission.OWNER_WRITE );
        }
        if ( perm( smode, 0, x ) ) {
            perms.add( PosixFilePermission.OWNER_EXECUTE );
        }
        if ( perm( smode, 1, r ) ) {
            perms.add( PosixFilePermission.GROUP_READ );
        }
        if ( perm( smode, 1, w ) ) {
            perms.add( PosixFilePermission.GROUP_WRITE );
        }
        if ( perm( smode, 1, x ) ) {
            perms.add( PosixFilePermission.GROUP_EXECUTE );
        }
        if ( perm( smode, 2, r ) ) {
            perms.add( PosixFilePermission.OTHERS_READ );
        }
        if ( perm( smode, 2, w ) ) {
            perms.add( PosixFilePermission.OTHERS_WRITE );
        }
        if ( perm( smode, 2, x ) ) {
            perms.add( PosixFilePermission.OTHERS_EXECUTE );
        }
        return perms;
    }

    private static boolean perm( String[] m, int i, int p ) {
        return ( Integer.parseInt( m[ i ] ) & p ) == p;
    }

    @Deprecated
    public static final void deleteRecursive( final Path directory ) throws IOException {
        Files.walk( directory )
            .sorted( Comparator.reverseOrder() )
            .map( Path::toFile )
            .forEach( File::delete );
    }

    public static void mkdir( Path path ) {
        try {
            if ( !Files.exists( path ) ) {
                Files.createDirectories( path );
            }
        }
        catch ( IOException ex ) {
            throw new IllegalStateException( ex );
        }
    }

    public static void rmdir( Path path ) {
        try {
            if ( !Files.exists( path ) ) {
                return;
            }
            if ( Files.isDirectory( path ) ) {
                Files.list( path ).forEach( FileUtil::rmdir );
            }
            Files.delete( path );
        }
        catch ( IOException ex ) {
            throw new IllegalStateException( ex );
        }
    }

}
