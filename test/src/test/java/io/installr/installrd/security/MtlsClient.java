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
package io.installr.installrd.security;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import lombok.SneakyThrows;

/**
 * Minimal HTTPS client for the installrd API, authenticating with one of the test client certificates in
 * {@code certs/}; without a client name no certificate is sent.
 */
class MtlsClient {

    static final String BASE_URL = "https://localhost:9322";

    private static final char[] KEY_PASSWORD = "test".toCharArray();

    private final HttpClient httpClient;

    MtlsClient( String clientName ) {
        this.httpClient = HttpClient.newBuilder().sslContext( sslContext( clientName ) ).build();
    }

    static MtlsClient as( String clientName ) {
        return new MtlsClient( clientName );
    }

    static MtlsClient anonymous() {
        return new MtlsClient( null );
    }

    HttpResponse<String> get( String path ) {
        return send( HttpRequest.newBuilder( URI.create( BASE_URL + path ) ).GET() );
    }

    HttpResponse<String> post( String path, String json ) {
        return send(
            HttpRequest.newBuilder( URI.create( BASE_URL + path ) )
                .header( "Content-Type", "application/json" )
                .POST( BodyPublishers.ofString( json ) )
        );
    }

    HttpResponse<String> delete( String path ) {
        return send( HttpRequest.newBuilder( URI.create( BASE_URL + path ) ).DELETE() );
    }

    @SneakyThrows
    private HttpResponse<String> send( HttpRequest.Builder builder ) {
        return httpClient.send( builder.build(), BodyHandlers.ofString() );
    }

    @SneakyThrows
    private static SSLContext sslContext( String clientName ) {
        final CertificateFactory certificateFactory = CertificateFactory.getInstance( "X.509" );

        final KeyStore trustStore = KeyStore.getInstance( KeyStore.getDefaultType() );
        trustStore.load( null, null );
        try ( InputStream in = resource( "certs/ca.crt" ) ) {
            trustStore.setCertificateEntry( "ca", certificateFactory.generateCertificate( in ) );
        }
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
        tmf.init( trustStore );

        KeyManagerFactory kmf = null;
        if ( clientName != null ) {
            final Certificate certificate;
            try ( InputStream in = resource( "certs/" + clientName + ".crt" ) ) {
                certificate = certificateFactory.generateCertificate( in );
            }
            final KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
            keyStore.load( null, null );
            keyStore.setKeyEntry(
                "client",
                privateKey( "certs/" + clientName + ".key" ),
                KEY_PASSWORD,
                new Certificate[] { certificate }
            );
            kmf = KeyManagerFactory.getInstance( KeyManagerFactory.getDefaultAlgorithm() );
            kmf.init( keyStore, KEY_PASSWORD );
        }

        final SSLContext sslContext = SSLContext.getInstance( "TLS" );
        sslContext.init( kmf == null ? null : kmf.getKeyManagers(), tmf.getTrustManagers(), null );
        return sslContext;
    }

    @SneakyThrows
    private static PrivateKey privateKey( String name ) {
        final String pem;
        try ( InputStream in = resource( name ) ) {
            pem = new String( in.readAllBytes(), StandardCharsets.US_ASCII );
        }
        final String base64 = pem
            .replace( "-----BEGIN PRIVATE KEY-----", "" )
            .replace( "-----END PRIVATE KEY-----", "" )
            .replaceAll( "\\s", "" );
        return KeyFactory.getInstance( "RSA" )
            .generatePrivate( new PKCS8EncodedKeySpec( Base64.getDecoder().decode( base64 ) ) );
    }

    private static InputStream resource( String name ) throws IOException {
        final InputStream in = MtlsClient.class.getClassLoader().getResourceAsStream( name );
        if ( in == null ) {
            throw new IOException( "Resource not found: " + name );
        }
        return in;
    }

}
