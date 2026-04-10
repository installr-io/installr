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
package io.installr.lib.installrd.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class QueryBuilder {

    private final Map<String, Object> params;

    public QueryBuilder() {
        this.params = new HashMap<>();
    }

    public void put( String field, Object value ) {
        if ( value != null ) {
            params.put( field, value );
        }
    }

    public String query() {
        return params.keySet().stream()
            .map( k -> k + " = :" + k )
            .collect( Collectors.joining( " and " ) );
    }

    public Map<String, Object> params() {
        return params;
    }

    public Map<String, Object> params( String key, Function<Object, Object> transformation ) {
        return params( Map.of( key, transformation ) );
    }

    public Map<String, Object> params( Map<String, Function<Object, Object>> map ) {
        return params.entrySet().stream()
            .map(
                entry -> map.containsKey( entry.getKey() )
                    ? Map.entry( entry.getKey(), map.get( entry.getKey() ).apply( entry.getValue() ) )
                    : entry
            )
            .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
    }

}
