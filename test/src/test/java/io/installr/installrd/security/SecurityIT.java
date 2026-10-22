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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.LocationType;
import io.installr.installrd.util.ThreadUtil;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Verifies authentication (mTLS) and authorization (roles from {@code test-config}) of the installrd API.
 * <ul>
 * <li>admin: built-in admin role</li>
 * <li>ops: global binding to 'ns-manager' (ns:create, ns:delete)</li>
 * <li>developer: 'developer' and 'sneaky' (with global operations) in Namespace1</li>
 * <li>viewer: 'viewer' in Namespace1</li>
 * <li>stranger: valid certificate, but no bindings</li>
 * </ul>
 */
@QuarkusTest
class SecurityIT {

    static final String INSTALL_BODY = "{\"packageId\":[\"test-package:0.1.0\"]}";
    static final String INSTALL_FILE_BODY = "{\"file\":[\"/etc/passwd\"]}";

    @Inject
    InstallrdConfiguration config;

    // --- authentication ---

    @Test
    void request_without_certificate_should_be_rejected() {
        assertEquals( 401, MtlsClient.anonymous().get( "/namespaces" ).statusCode() );
        assertEquals( 401, MtlsClient.anonymous().get( "/namespaces/Namespace1/package-log" ).statusCode() );
        assertEquals( 401, MtlsClient.anonymous().post( "/stop", "" ).statusCode() );
    }

    @Test
    void version_should_be_public() {
        assertEquals( 200, MtlsClient.anonymous().get( "/version" ).statusCode() );
        assertEquals( 200, MtlsClient.anonymous().get( "/version-info" ).statusCode() );
    }

    // --- namespace scoped operations ---

    @Test
    void viewer_should_read_but_not_install() {
        final MtlsClient viewer = MtlsClient.as( "viewer" );
        assertEquals( 200, viewer.get( "/namespaces/Namespace1" ).statusCode() );
        assertEquals( 200, viewer.get( "/namespaces/Namespace1/package-log" ).statusCode() );
        assertEquals( 200, viewer.get( "/namespaces/Namespace1/transactions" ).statusCode() );
        assertEquals( 403, viewer.post( "/namespaces/Namespace1/install", INSTALL_BODY ).statusCode() );
        assertEquals( 403, viewer.post( "/namespaces/Namespace1/reinstall", INSTALL_BODY ).statusCode() );
        assertEquals( 403, viewer.post( "/namespaces/Namespace1/cancel", "" ).statusCode() );
        assertEquals( 403, viewer.post( "/namespaces/Namespace1/install-file", INSTALL_FILE_BODY ).statusCode() );
        assertEquals( 403, viewer.post( "/namespaces/Namespace1/reinstall-file", INSTALL_FILE_BODY ).statusCode() );
    }

    @Test
    void developer_should_be_restricted_to_bound_namespace() {
        final MtlsClient developer = MtlsClient.as( "developer" );
        assertEquals( 200, developer.get( "/namespaces/Namespace1/transactions" ).statusCode() );
        assertEquals( 403, developer.get( "/namespaces/Namespace2/transactions" ).statusCode() );
        assertEquals( 403, developer.get( "/namespaces/Namespace2/package-log" ).statusCode() );
        assertEquals( 403, developer.post( "/namespaces/Namespace2/install", INSTALL_BODY ).statusCode() );
        // 'developer' does not include pkg:install-file / pkg:reinstall-file
        assertEquals( 403, developer.post( "/namespaces/Namespace1/install-file", INSTALL_FILE_BODY ).statusCode() );
        assertEquals( 403, developer.post( "/namespaces/Namespace1/reinstall-file", INSTALL_FILE_BODY ).statusCode() );
    }

    @Test
    void stranger_should_not_access_any_namespace() {
        final MtlsClient stranger = MtlsClient.as( "stranger" );
        assertEquals( 403, stranger.get( "/namespaces/Namespace1" ).statusCode() );
        assertEquals( 403, stranger.get( "/namespaces/Namespace1/package-log" ).statusCode() );
        assertEquals( "[]", stranger.get( "/namespaces" ).body() );
    }

    @Test
    void namespace_list_should_only_contain_visible_namespaces() {
        final String viewerNamespaces = MtlsClient.as( "viewer" ).get( "/namespaces" ).body();
        assertTrue( viewerNamespaces.contains( "Namespace1" ) );
        assertFalse( viewerNamespaces.contains( "Namespace2" ) );

        final String adminNamespaces = MtlsClient.as( "admin" ).get( "/namespaces" ).body();
        assertTrue( adminNamespaces.contains( "Namespace1" ) );
        assertTrue( adminNamespaces.contains( "Namespace2" ) );
    }

    // --- global operations ---

    @Test
    void global_operations_should_be_ignored_in_namespace_bindings() {
        final MtlsClient developer = MtlsClient.as( "developer" );
        // 'sneaky' grants daemon:stop and ns:create, but is bound in Namespace1 only
        assertEquals( 403, developer.post( "/stop", "" ).statusCode() );
        assertEquals( 403, developer.post( "/namespaces", "{\"name\":\"DeveloperNs\"}" ).statusCode() );
        assertEquals( 403, developer.delete( "/namespaces/Namespace1" ).statusCode() );
    }

    @Test
    void global_binding_should_grant_namespace_management() {
        final MtlsClient ops = MtlsClient.as( "ops" );
        assertEquals( 201, ops.post( "/namespaces", "{\"name\":\"SecurityItNs\"}" ).statusCode() );
        assertEquals( 204, ops.delete( "/namespaces/SecurityItNs" ).statusCode() );
        // ns-manager does not include any namespace scoped operation
        assertEquals( 403, ops.get( "/namespaces/Namespace1/package-log" ).statusCode() );
        assertEquals( 403, ops.post( "/stop", "" ).statusCode() );
    }

    @Test
    void invalid_namespace_name_should_be_rejected() {
        final MtlsClient admin = MtlsClient.as( "admin" );
        assertEquals( 400, admin.post( "/namespaces", "{\"name\":\"../escape\"}" ).statusCode() );
        assertEquals( 400, admin.post( "/namespaces", "{\"name\":\".hidden\"}" ).statusCode() );
    }

    // --- hot reload ---

    @Test
    void changed_bindings_should_apply_after_reload() throws Exception {
        final Path namespace2Config = config.locationFor( LocationType.Namespace )
            .resolve( "Namespace2" )
            .resolve( "config.yaml" );
        final String original = Files.readString( namespace2Config );
        final MtlsClient viewer = MtlsClient.as( "viewer" );
        assertEquals( 403, viewer.get( "/namespaces/Namespace2/package-log" ).statusCode() );
        try {
            Files.writeString(
                namespace2Config,
                original + "\naccess:\n    - subject: \"CN=viewer,O=installr-test\"\n      roles: [ viewer ]\n"
            );
            assertTrue(
                waitFor( () -> viewer.get( "/namespaces/Namespace2/package-log" ).statusCode() == 200 ),
                "binding not applied after reload"
            );
        }
        finally {
            Files.writeString( namespace2Config, original );
            assertTrue(
                waitFor( () -> viewer.get( "/namespaces/Namespace2/package-log" ).statusCode() == 403 ),
                "binding not removed after reload"
            );
        }
    }

    private static boolean waitFor( BooleanSupplier condition ) {
        final Instant deadline = Instant.now().plus( Duration.ofSeconds( 15 ) );
        while ( Instant.now().isBefore( deadline ) ) {
            if ( condition.getAsBoolean() ) {
                return true;
            }
            ThreadUtil.sleep( 250 );
        }
        return false;
    }

}
