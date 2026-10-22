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
package io.installr.installrd.database;

import java.nio.file.Path;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.Unremovable;
import io.quarkus.credentials.CredentialsProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import lombok.SneakyThrows;

/**
 * Provides the {@link DatabaseCredentials} to the datasource
 * ({@code quarkus.datasource.credentials-provider-name: installrd-database}).
 */
@ApplicationScoped
@Unremovable
@Named( DatabaseCredentialsProvider.NAME )
public class DatabaseCredentialsProvider implements CredentialsProvider {

    public static final String NAME = "installrd-database";

    @ConfigProperty( name = "installrd.config.location" )
    Path configLocation;

    @Override
    @SneakyThrows
    public Map<String, String> getCredentials( String credentialsProviderName ) {
        final DatabaseCredentials credentials = DatabaseCredentials.ensure( configLocation.toAbsolutePath() );
        return Map.of(
            USER_PROPERTY_NAME,
            DatabaseCredentials.USERNAME,
            PASSWORD_PROPERTY_NAME,
            credentials.password()
        );
    }

}
