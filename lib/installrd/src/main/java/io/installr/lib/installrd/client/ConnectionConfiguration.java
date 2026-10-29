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
package io.installr.lib.installrd.client;

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single entry of a {@link ClientConfiguration}: where installrd runs and how to authenticate (mTLS).
 *
 * @param url  the HTTPS URL of installrd
 * @param ca   optional PEM encoded CA certificate(s) to verify the server certificate; if not set, the JDK trust store
 *             is used
 * @param cert PEM encoded client certificate (optionally followed by intermediate certificates)
 * @param key  PEM encoded private key of the client certificate
 */
public record ConnectionConfiguration(
    @JsonProperty( "url" ) String url,
    @JsonProperty( "ca" ) String ca,
    @JsonProperty( "cert" ) String cert,
    @JsonProperty( "key" ) String key
) {

    /**
     * Validates this configuration and returns it as connection with the given name.
     */
    public InstallrdConnection toConnection( String name ) {
        if ( isBlank( url ) ) {
            throw new InvalidClientConfigurationException( "Config '%s': 'url' is missing.", name );
        }
        final URI uri;
        try {
            uri = new URI( url.trim() );
        }
        catch ( URISyntaxException ex ) {
            throw new InvalidClientConfigurationException(
                ex,
                "Config '%s': invalid 'url': %s",
                name,
                ex.getMessage()
            );
        }
        if ( !"https".equalsIgnoreCase( uri.getScheme() ) || uri.getHost() == null ) {
            throw new InvalidClientConfigurationException(
                "Config '%s': 'url' must be an https URL (e.g. https://localhost:9316), but is '%s'.",
                name,
                url
            );
        }
        if ( isBlank( cert ) ) {
            throw new InvalidClientConfigurationException(
                "Config '%s': 'cert' (client certificate) is missing.",
                name
            );
        }
        if ( isBlank( key ) ) {
            throw new InvalidClientConfigurationException( "Config '%s': 'key' (client key) is missing.", name );
        }
        return new InstallrdConnection( name, uri, isBlank( ca ) ? null : ca, cert, key );
    }

    private static boolean isBlank( String value ) {
        return value == null || value.isBlank();
    }

}
