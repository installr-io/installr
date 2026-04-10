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
package io.installr.installrd;

import java.time.Instant;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.semver4j.Semver;

public final class Installrd {

    public static Semver version() {
        return getConfigValue( "quarkus.application.version", Semver.class ).get();
    }

    public static String revision() {
        return getConfigValue( "build.revision", String.class ).orElse( "unknown" );
    }

    public static Instant buildTimestamp() {
        return Instant
            .ofEpochMilli(
                getConfigValue( "build.timestamp", Long.class )
                    .orElse( System.currentTimeMillis() )
            );
    }

    private static <T> Optional<T> getConfigValue( String propertyName, Class<T> propertyType ) {
        return ConfigProvider.getConfig().getOptionalValue( propertyName, propertyType );
    }

}
