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
package io.installr.lib.registry.client;

import java.io.IOException;
import java.util.Base64;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

@Priority( Priorities.AUTHENTICATION )
public class BasicAuthRequestFilter implements ClientRequestFilter {

    private final String username;
    private final String password;

    public BasicAuthRequestFilter( String username, String password ) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void filter( ClientRequestContext rc ) throws IOException {
        rc.getHeaders().add( HttpHeaders.AUTHORIZATION, buildAuthToken() );
    }

    private String buildAuthToken() {
        return String.format(
            "Basic %s",
            Base64.getEncoder().encodeToString(
                String.format( "%s:%s", username, password ).getBytes()
            )
        );
    }

}
