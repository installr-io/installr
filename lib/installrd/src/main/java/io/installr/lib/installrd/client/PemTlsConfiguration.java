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

import java.util.Optional;

import io.quarkus.tls.BaseTlsConfiguration;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

/**
 * TLS configuration of a connection to installrd, built from the PEM values of an {@link InstallrdConnection} (no files
 * involved). The client certificate is used for mTLS; the server certificate is verified against {@code ca} (or the JDK
 * trust store) including its host name.
 */
public class PemTlsConfiguration extends BaseTlsConfiguration {

    private final PemKeyCertOptions keyCertOptions;
    private final PemTrustOptions trustOptions;

    public PemTlsConfiguration( InstallrdConnection connection ) {
        this.keyCertOptions = new PemKeyCertOptions()
            .setCertValue( Buffer.buffer( connection.cert() ) )
            .setKeyValue( Buffer.buffer( connection.key() ) );
        this.trustOptions = connection.ca() == null
            ? null
            : new PemTrustOptions().addCertValue( Buffer.buffer( connection.ca() ) );
    }

    @Override
    public KeyCertOptions getKeyStoreOptions() {
        return keyCertOptions;
    }

    @Override
    public TrustOptions getTrustStoreOptions() {
        // null: JDK trust store
        return trustOptions;
    }

    @Override
    public SSLOptions getSSLOptions() {
        return new SSLOptions();
    }

    @Override
    public Optional<String> getHostnameVerificationAlgorithm() {
        return Optional.of( "HTTPS" );
    }

}
