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
package io.installr.installrd.deployment;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public class MediaTypeMatcher {

    private final boolean matches;
    private final String mediaType;

    private MediaTypeMatcher( boolean matches, String mediaType ) {
        this.matches = matches;
        this.mediaType = mediaType;
    }

    public boolean matches() {
        return matches;
    }

    public String mediaType() {
        return mediaType;
    }

    public static MediaTypeMatcher matcher( Set<String> configuredTypes, String mediaType ) {
        if ( !configuredTypes.contains( mediaType ) ) {
            // check for wildcard
            final Optional<String> matchedType = configuredTypes.stream()
                .filter( x -> x.contains( "*" ) )
                .sorted( Comparator.reverseOrder() )
                .filter( x -> toPattern( x ).matcher( mediaType ).matches() )
                .findFirst();
            return new MediaTypeMatcher( matchedType.isPresent(), matchedType.orElse( null ) );
        }
        return new MediaTypeMatcher( true, mediaType );
    }

    private static Pattern toPattern( String str ) {
        return Pattern.compile(
            str.replace( ".", "\\." )
                .replace( "*", ".*" )
        );
    }

}
