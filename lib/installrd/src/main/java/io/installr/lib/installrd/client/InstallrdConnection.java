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

/**
 * A validated connection to installrd, see {@link ConnectionConfiguration#toConnection(String)}.
 *
 * @param name name of the config entry
 * @param url  HTTPS URL of installrd
 * @param ca   PEM encoded CA certificate(s), or {@code null} to use the JDK trust store
 * @param cert PEM encoded client certificate
 * @param key  PEM encoded private key of the client certificate
 */
public record InstallrdConnection( String name, URI url, String ca, String cert, String key ) {

    @Override
    public String toString() {
        // never print the key
        return String.format( "InstallrdConnection[name=%s, url=%s]", name, url );
    }

}
