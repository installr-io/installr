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
package io.installr.installrd.util.interpolation;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class EnvironmentInterpolator {

    private static final Pattern VAR_PATTERN = Pattern.compile( "\\$\\{(.*?)(:-(.*?))?\\}" );

    public String interpolate( String json, Map<String, String> env ) {
        final Matcher matcher = VAR_PATTERN.matcher( json );
        final StringBuilder resultBuilder = new StringBuilder();
        while ( matcher.find() ) {
            String key = matcher.group( 1 );
            String val = env.get( key );
            String def = matcher.group( 3 );
            log.debugf( "interpolate => key=%s; val=%s (def=%s);", key, val, def );
            if ( val == null || val.isBlank() ) {
                val = def == null ? "" : def;
            }
            log.debugf( "interpolate => effective value: %s", val );
            matcher.appendReplacement( resultBuilder, val );
        }
        matcher.appendTail( resultBuilder );
        return resultBuilder.toString();
    }

}
