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

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

/**
 * Creates {@link InstallrdClient}s, which connect to installrd via mTLS.
 *
 * <pre>
 * final InstallrdConnection connection = ClientConfiguration.load( file ).select( name );
 * final InstallrdClient client = InstallrdClients.create( connection, "my-client/1.0" );
 * </pre>
 */
public final class InstallrdClients {

    private InstallrdClients() {
    }

    /**
     * @param connection the connection to use
     * @param userAgent  the user agent sent to installrd, e.g. {@code ic/0.2.0}
     * @param providers  additional providers to register, as class or instance, e.g. a {@code ResponseExceptionMapper}
     *                   class or a {@code ClientRequestFilter} lambda
     */
    public static InstallrdClient create( InstallrdConnection connection, String userAgent, Object ... providers ) {
        QuarkusRestClientBuilder builder = QuarkusRestClientBuilder.newBuilder()
            .baseUri( connection.url() )
            .tlsConfiguration( new PemTlsConfiguration( connection ) )
            .userAgent( userAgent );
        for ( Object provider : providers ) {
            builder = provider instanceof Class<?> type ? builder.register( type ) : builder.register( provider );
        }
        return builder.build( InstallrdClient.class );
    }

}
