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
package io.installr.installrd.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

public class MapUtil {

    private MapUtil() {
    }

    /**
     * Returns a new map containing all entries of the given map, plus an additional entry for each key whose dots and
     * hyphens are replaced by underscores (e.g. {@code my.key-name} → {@code my_key_name}). If a key already has no
     * dots or hyphens, no duplicate entry is added.
     */
    public static Map<String, String> flatten( Map<String, ?> map ) {
        if ( map == null ) {
            return null;
        }
        final Map<String, String> dest = new HashMap<>( map.size() );
        map.forEach(
            ( k, v ) -> flatten( dest, k, v )
        );
        return expand( dest );
    }

    private static void flatten( Map<String, String> dest, String k, Object v ) {
        if ( v instanceof Optional<?> opt ) {
            v = opt.isEmpty() ? "" : opt.get();
        }
        if ( v instanceof Map ) {
            @SuppressWarnings( "unchecked" )
            Map<String, ?> map = (Map<String, ?>)v;
            map.forEach(
                ( sk, sv ) -> {
                    String nko = k + "." + sk;
                    if ( sv instanceof Map ) {
                        flatten( dest, nko, sv );
                    }
                    else {
                        dest.put( nko, toString( sv ) );
                    }
                }
            );
        }
        else {
            dest.put( k, toString( v ) );
        }
    }

    private static String toString( Object value ) {
        if ( value == null ) {
            return null;
        }
        return value.toString();
    }

    private static Map<String, String> expand( Map<String, String> map ) {
        return map.entrySet().stream()
            .flatMap(
                entry -> Stream.of(
                    Map.entry( entry.getKey().toLowerCase(), entry.getValue() ),
                    Map.entry( toSnakeCase( entry.getKey() ), entry.getValue() )
                )
            )
            .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue, ( k, v ) -> k ) );
    }

    private static String toSnakeCase( String key ) {
        return key.replace( ".", "_" ).replace( "-", "_" ).toLowerCase();
    }

    public static void dump( Logger log, Map<String, String> map ) {
        if ( map == null ) {
            log.debugf( "dump: map is null" );
        }
        else {
            map.entrySet().stream()
                .sorted( Map.Entry.comparingByKey() )
                .forEach( entry -> log.debugf( "  %s = %s", entry.getKey(), entry.getValue() ) );
        }
    }

}
