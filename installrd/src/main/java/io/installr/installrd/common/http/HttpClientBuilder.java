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
package io.installr.installrd.common.http;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import io.installr.installrd.common.http.auth.ApiKeyAuthConfig;
import io.installr.installrd.common.http.truststore.TrustStoreConfig;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

public class HttpClientBuilder {

    public static <T> T build( HttpClientConfig config, Class<T> clazz ) {
        final QuarkusRestClientBuilder builder = QuarkusRestClientBuilder.newBuilder()
            .baseUri( URI.create( config.getUrl() ) )
            .clientHeadersFactory(
                ( incomingHeaders, clientOutgoingHeaders ) -> {
                    MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
                    switch ( config.getAuth().getType() ) {
                        case None:
                            break;
                        case ApiKey:
                            final ApiKeyAuthConfig apiKeyConfig = (ApiKeyAuthConfig)config.getAuth();
                            result.add( apiKeyConfig.getKeyName(), apiKeyConfig.getKeyValue() );
                            break;
                    }
                    return result;
                }
            );
        if ( config.getTrustStore() != null ) {
            final TrustStoreConfig tsConfig = config.getTrustStore();
            final Path trustStorePath = tsConfig.getPath();
            if ( !Files.exists( trustStorePath ) ) {
                throw new IllegalStateException(
                    String.format( "Trust store path does not exist: %s", trustStorePath )
                );
            }
            try {
                final String passwd = tsConfig.getPasswd() == null ? "" : tsConfig.getPasswd();
                builder.trustStore( KeyStore.getInstance( trustStorePath.toFile(), passwd.toCharArray() ) );
                builder.verifyHost( false );
            }
            catch ( IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex ) {
                throw new IllegalStateException(
                    String.format( "Could not read trust store: %s", ex.getMessage() )
                );
            }
        }
        return builder.build( clazz );
    }

}
