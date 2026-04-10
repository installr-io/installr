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

import org.semver4j.Semver;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Data
@Accessors( fluent = true )
@AllArgsConstructor
public class PackageId implements Comparable<PackageId> {

    private static final String PACKAGE_NAME_REGEX = "^[a-z]+[a-z0-9-_]*";

    @JsonProperty
    private final @NonNull String name;
    @JsonSerialize( using = ToStringSerializer.class )
    private final @NonNull Semver version;

    public PackageId(
        @NonNull @JsonProperty( "name" ) String name, @NonNull @JsonProperty( "version" ) String version
    ) {
        this.name = name;
        this.version = new Semver( version );
    }

    public static PackageId valueOf( @NonNull String str ) throws InvalidPackageIdException {
        String[] parts = str.split( ":" );
        if ( parts == null || parts.length != 2 ) {
            throw new InvalidPackageIdException( "str does not match format" );
        }
        final String namePart = parts[ 0 ];
        if ( !namePart.matches( PACKAGE_NAME_REGEX ) ) {
            throw new InvalidPackageIdException( "package name contains illegal characters" );
        }
        final String versionPart = parts[ 1 ];
        if ( !Semver.isValid( versionPart ) ) {
            throw new InvalidPackageIdException( "version does not match semver format" );
        }
        return new PackageId( namePart, new Semver( versionPart ) );
    }

    @Override
    public String toString() {
        return name + ":" + version;
    }

    @Override
    public int compareTo( @NonNull PackageId other ) {
        int compare = this.name.compareTo( other.name );
        if ( compare == 0 ) {
            return this.version.compareTo( other.version );
        }
        return compare;
    }

    public String toArchiveName() {
        return toPackageName() + Package.EXTENSION;
    }

    public String toPackageName() {
        return name + "-" + version;
    }

}
