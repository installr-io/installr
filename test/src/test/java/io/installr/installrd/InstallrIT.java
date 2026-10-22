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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.installr.installrd.install.InstallService;
import io.installr.installrd.logstore.ItemLog;
import io.installr.installrd.namespace.LockException;
import io.installr.installrd.namespace.NamespaceNotFoundException;
import io.installr.installrd.pkg.PackageCache;
import io.installr.installrd.util.ThreadUtil;
import io.installr.lib.installrd.controller.InstallController;
import io.installr.lib.installrd.event.Event;
import io.installr.lib.installrd.event.Events.PackageInstallationFinished;
import io.installr.lib.installrd.event.Events.PackageInstallationStarted;
import io.installr.lib.installrd.event.Events.PackageResolvingFinished;
import io.installr.lib.installrd.event.Events.TransactionFinished;
import io.installr.lib.installrd.event.Status;
import io.installr.lib.installrd.pkg.ItemLogRecord;
import io.installr.lib.installrd.resource.InstallPackageResource;
import io.installr.lib.pkg.InvalidPackageIdException;
import io.installr.lib.pkg.PackageId;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.jbosslog.JBossLog;

@QuarkusTest
@JBossLog
class InstallrIT {

    private static final String TEST_NAMESPACE = "Namespace1";
    private static final String TEST_PACKAGE = "test-package:0.1.0";
    private static final String TEST_PACKAGE_NOT_FOUND = "test-package-not-found:0.1.0";

    @Inject
    @RestClient
    InstallrClient installrClient;

    @Inject
    ItemLog itemLog;

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Test
    void install_should_throw_NamespaceNotFoundException() {
        final String namespace = "Non-Existent-Namespace";
        final InstallPackageResource resource = new InstallPackageResource();
        resource.setPackageIds( List.of( PackageId.valueOf( TEST_PACKAGE ) ) );
        final RuntimeException ex = assertThrows(
            RuntimeException.class,
            () -> installrClient.install( namespace, resource )
                .subscribe()
                .asStream()
                .forEach(
                    event -> System.out.println( event )
                )
        );
        checkException( new NamespaceNotFoundException( namespace ), ex );
    }

    void checkException( Throwable expected, Throwable actual ) {
        log.infof( "Exception: %s", actual.getMessage() );
        assertEquals( actual.getMessage(), expected.getMessage(), expected.getClass().getSimpleName() );
    }

    @Test
    void install_should_fail_on_package_not_found() throws InvalidPackageIdException, InterruptedException {
        // install
        final Map<Class<?>, List<Event>> events = install(
            TEST_NAMESPACE,
            List.of( PackageId.valueOf( TEST_PACKAGE_NOT_FOUND ), PackageId.valueOf( TEST_PACKAGE ) )
        );
        final PackageResolvingFinished prf = (PackageResolvingFinished)events.get( PackageResolvingFinished.class )
            .getFirst();
        assertEquals( Status.Type.Fatal, prf.status().type() );
        assertEquals( 1, prf.notFoundPackages().size() );
        assertEquals( TEST_PACKAGE_NOT_FOUND, prf.notFoundPackages().get( 0 ).toString() );
        final TransactionFinished txf = (TransactionFinished)events.get( TransactionFinished.class ).getFirst();
        assertEquals( Status.Type.Fatal, txf.status().type() );
    }

    private Map<Class<?>, List<Event>> install( String namespace, List<PackageId> packageIds ) {
        final InstallPackageResource resource = new InstallPackageResource();
        resource.setPackageIds( packageIds );
        return installrClient
            .install( namespace, resource )
            .onItem().invoke( event -> System.out.println( event ) )
            .subscribe()
            .asStream()
            .collect( Collectors.groupingBy( Event::getClass ) );
    }

    private Map<Class<?>, List<Event>> reinstall( String namespace, List<PackageId> packageIds ) {
        final InstallPackageResource resource = new InstallPackageResource();
        resource.setPackageIds( packageIds );
        return installrClient
            .reinstall( namespace, resource )
            .onItem().invoke( event -> System.out.println( event ) )
            .subscribe()
            .asStream()
            .collect( Collectors.groupingBy( Event::getClass ) );
    }

    @Test
    void install_should_throw_LockException() throws Exception {
        installPackageCacheMock();
        installInstallrServiceMock();

        // Test
        // We use a background thread to simulate concurrency. The background thread starts a dummy install session,
        // which waits 3 seconds and than completes. The foreground thread starts another install session, which should
        // than lead to a LockException.
        Thread backgroundThread = new Thread(
            () -> {
                final Map<Class<?>, List<Event>> events = install(
                    TEST_NAMESPACE,
                    List.of( PackageId.valueOf( TEST_PACKAGE ) )
                );
                final TransactionFinished txf = (TransactionFinished)events.get( TransactionFinished.class ).getFirst();
                assertEquals( Status.Type.Success, txf.status().type() );
            }
        );
        backgroundThread.start();
        TimeUnit.SECONDS.sleep( 1 );

        final InstallPackageResource resource = new InstallPackageResource();
        resource.setPackageIds( List.of( PackageId.valueOf( TEST_PACKAGE ) ) );
        RuntimeException ex = assertThrows(
            RuntimeException.class,
            () -> installrClient.install( TEST_NAMESPACE, resource )
                .subscribe()
                .asStream()
                .forEach(
                    event -> System.out.println( event )
                )
        );
        checkException( new LockException(), ex );
        backgroundThread.join();
    }

    private void installPackageCacheMock() {
        final PackageCache cacheMock = Mockito.mock( PackageCache.class );
        Mockito
            .when( cacheMock.isOutdated() )
            .thenReturn( false );
        // if ( packageId != null ) {
        // Mockito
        // .when( cacheMock.searchIndex( any() ) )
        // .thenReturn( null );
        // }
        QuarkusMock.installMockForType( cacheMock, PackageCache.class );
    }

    private void installInstallrServiceMock() {
        final InstallService installerMock = Mockito.mock( InstallService.class );
        Mockito.doAnswer(
            invocation -> {
                log.info( "simulate install task..." );
                ThreadUtil.sleep( Duration.ofSeconds( 3 ) );
                return null;
            }
        ).when( installerMock ).install( any(), anyList() );
        QuarkusMock.installMockForType( installerMock, InstallService.class );
    }

    @Test
    void ensure_parallel_install_to_2_namespaces_works() throws Exception {
        final String namespace1 = TEST_NAMESPACE;
        final String namespace2 = "Namespace2";

        List.of( namespace1, namespace2 ).parallelStream()
            .forEach(
                namespace -> {
                    final Map<Class<?>, List<Event>> events = install(
                        namespace,
                        List.of( PackageId.valueOf( TEST_PACKAGE ) )
                    );
                    final TransactionFinished txf = (TransactionFinished)events.get( TransactionFinished.class )
                        .getFirst();
                    assertEquals( Status.Type.Success, txf.status().type() );
                }
            );
    }

    /**
     * Two simultaneous install requests to the <em>same</em> namespace must behave like sequential requests: the first
     * acquires the lock and the second is rejected with a {@link LockException}.
     *
     * <p>
     * Guaranteed by {@code ConcurrentHashMap.computeIfAbsent} in {@code TransactionService.acquireTransaction} (ensures
     * both threads operate on the same {@code Transaction} instance) combined with {@code synchronized acquire} and
     * {@code volatile txId} in {@code Transaction} (ensures the check-then-act is atomic and the written {@code txId}
     * is immediately visible to all threads).
     */
    @Test
    void concurrent_install_to_same_namespace_must_result_in_lock_exception() throws Exception {
        installPackageCacheMock();
        installInstallrServiceMock();

        final AtomicInteger lockCount = new AtomicInteger( 0 );
        final AtomicInteger successCount = new AtomicInteger( 0 );

        List.of( TEST_NAMESPACE, TEST_NAMESPACE ).parallelStream()
            .forEach(
                namespace -> {
                    try {
                        final Map<Class<?>, List<Event>> events = install(
                            namespace,
                            List.of( PackageId.valueOf( TEST_PACKAGE ) )
                        );
                        final TransactionFinished txf = (TransactionFinished)events
                            .get( TransactionFinished.class )
                            .getFirst();
                        assertEquals( Status.Type.Success, txf.status().type() );
                        successCount.incrementAndGet();
                    }
                    catch ( RuntimeException ex ) {
                        checkException( new LockException(), ex );
                        lockCount.incrementAndGet();
                    }
                }
            );

        assertEquals( 1, successCount.get(), "Exactly one install must succeed" );
        assertEquals( 1, lockCount.get(), "Exactly one install must be rejected with LockException" );
    }

    @Test
    void install_should_always_install_snapshots() throws Exception {
        final PackageId testPackageId = PackageId.valueOf( "test-package:0.1.0-SNAPSHOT" );
        Map<Class<?>, List<Event>> events;
        int totalPackages;

        // install for the first time
        events = install( TEST_NAMESPACE, List.of( testPackageId ) );
        totalPackages = ( (PackageResolvingFinished)events.get( PackageResolvingFinished.class ).getFirst() )
            .totalPackages();
        assertEquals( 1, totalPackages );

        final PackageInstallationStarted firstTimeInstall = (PackageInstallationStarted)events
            .get( PackageInstallationStarted.class ).getFirst();
        assertEquals( testPackageId, firstTimeInstall.packageId() );

        // install a second time
        events = install( TEST_NAMESPACE, List.of( testPackageId ) );
        totalPackages = ( (PackageResolvingFinished)events.get( PackageResolvingFinished.class ).getFirst() )
            .totalPackages();
        assertEquals( 1, totalPackages );

        final PackageInstallationStarted secondTimeInstall = (PackageInstallationStarted)events
            .get( PackageInstallationStarted.class ).getFirst();
        assertEquals( testPackageId, secondTimeInstall.packageId() );

        final TransactionFinished txf = (TransactionFinished)events.get( TransactionFinished.class )
            .getFirst();
        assertEquals( Status.Type.Success, txf.status().type() );
    }

    @Test
    void install_should_install_releases_only_once() throws Exception {
        final PackageId testPackageId = PackageId.valueOf( "test-package:0.2.0" );
        Map<Class<?>, List<Event>> events;
        int totalPackages;

        // install for the first time
        events = install( TEST_NAMESPACE, List.of( testPackageId ) );
        totalPackages = ( (PackageResolvingFinished)events.get( PackageResolvingFinished.class ).getFirst() )
            .totalPackages();
        assertEquals( 1, totalPackages );

        final PackageInstallationStarted firstTimeInstall = (PackageInstallationStarted)events
            .get( PackageInstallationStarted.class ).getFirst();
        assertEquals( testPackageId, firstTimeInstall.packageId() );

        // install a second time
        events = install( TEST_NAMESPACE, List.of( testPackageId ) );
        totalPackages = ( (PackageResolvingFinished)events.get( PackageResolvingFinished.class ).getFirst() )
            .totalPackages();
        assertEquals( 0, totalPackages );

        final TransactionFinished txf = (TransactionFinished)events.get( TransactionFinished.class )
            .getFirst();
        assertEquals( Status.Type.Success, txf.status().type() );
    }

    @Test
    void reinstall_should_reinstall_already_installed_packages() throws Exception {
        final PackageId testPackageId = PackageId.valueOf( "test-package-3:0.1.0" );
        Map<Class<?>, List<Event>> events;

        // install for the first time
        events = install( TEST_NAMESPACE, List.of( testPackageId ) );
        assertEquals(
            1,
            ( (PackageResolvingFinished)events.get( PackageResolvingFinished.class ).getFirst() ).totalPackages()
        );
        assertEquals(
            3,
            ( (PackageInstallationStarted)events.get( PackageInstallationStarted.class ).getFirst() ).totalItems()
        );
        assertEquals(
            Status.Success,
            ( (PackageInstallationFinished)events.get( PackageInstallationFinished.class ).getFirst() ).status()
        );

        // re-install
        events = reinstall( TEST_NAMESPACE, List.of( testPackageId ) );
        assertEquals(
            1,
            ( (PackageResolvingFinished)events.get( PackageResolvingFinished.class ).getFirst() ).totalPackages()
        );
        assertEquals(
            3,
            ( (PackageInstallationStarted)events.get( PackageInstallationStarted.class ).getFirst() ).totalItems()
        );
        assertEquals(
            Status.Success,
            ( (PackageInstallationFinished)events.get( PackageInstallationFinished.class ).getFirst() ).status()
        );

        final TransactionFinished txf = (TransactionFinished)events.get( TransactionFinished.class )
            .getFirst();
        assertEquals( Status.Type.Success, txf.status().type() );
    }

    @Test
    void reinstall_should_not_install_not_installed_packages() throws Exception {
        final PackageId testPackageId = PackageId.valueOf( "test-package-4:0.1.0" );

        // re-install not installed package
        final Map<Class<?>, List<Event>> events = reinstall( TEST_NAMESPACE, List.of( testPackageId ) );
        final Status status = ( (PackageResolvingFinished)events.get( PackageResolvingFinished.class ).getFirst() )
            .status();
        assertEquals( Status.Type.Fatal, status.type() );
        assertTrue(
            status.message().contains( "Packages selected for re-install but not installed" ),
            "selected-but-not-installed-packages-check"
        );
        final TransactionFinished txf = (TransactionFinished)events.get( TransactionFinished.class )
            .getFirst();
        assertEquals( Status.Type.Fatal, txf.status().type() );
    }

    @Test
    void ensure_parallel_install_to_2_namespaces_uses_correct_http_deployer_auth_header() throws Exception {
        final String namespace1 = TEST_NAMESPACE;
        final String namespace2 = "Namespace2";

        List.of( namespace1, namespace2 ).parallelStream()
            .forEach(
                namespace -> {
                    final InstallController installr = QuarkusRestClientBuilder.newBuilder()
                        .baseUri( URI.create( "https://localhost:9322" ) )
                        .tlsConfiguration( tlsRegistry.get( "admin-client" ).orElseThrow() )
                        .clientHeadersFactory( ( incomingHeaders, clientOutgoingHeaders ) -> {
                            MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
                            headers.add( "x-auth-token", namespace );
                            return headers;
                        } )
                        .build( InstallController.class );
                    final InstallPackageResource resource = new InstallPackageResource();
                    resource.setPackageIds( List.of( PackageId.valueOf( "http-test-package-1:0.1.0" ) ) );
                    final Map<Class<?>, List<Event>> events = installr
                        .install( namespace, resource )
                        .onItem().invoke( event -> System.out.println( event ) )
                        .subscribe()
                        .asStream()
                        .collect( Collectors.groupingBy( Event::getClass ) );

                    final TransactionFinished txf = (TransactionFinished)events.get( TransactionFinished.class )
                        .getFirst();
                    assertEquals( Status.Type.Success, txf.status().type() );
                }
            );
    }

    @Test
    void install_should_record_items_without_error() throws InvalidPackageIdException {
        final String namespace = "Namespace2";
        final PackageId testPackageId = PackageId.valueOf( "test-package-3:0.1.0" );

        final Map<Class<?>, List<Event>> events = install( namespace, List.of( testPackageId ) );

        assertEquals(
            Status.Success,
            ( (PackageInstallationFinished)events.get( PackageInstallationFinished.class ).getFirst() ).status()
        );

        final Map<String, ItemLogRecord> items = itemLog.getByPackagename( namespace, "test-package-3" );
        assertEquals( 3, items.size() );
        assertTrue( items.values().stream().noneMatch( ItemLogRecord::hasError ) );
    }

    @Test
    void install_should_record_items_with_error() throws InvalidPackageIdException {
        final String namespace = "Namespace2";
        final PackageId testPackageId = PackageId.valueOf( "test-package-with-errors:0.1.0" );

        final Map<Class<?>, List<Event>> events = install( namespace, List.of( testPackageId ) );

        assertEquals(
            Status.Failed,
            ( (PackageInstallationFinished)events.get( PackageInstallationFinished.class ).getFirst() ).status()
        );

        final Map<String, ItemLogRecord> items = itemLog.getByPackagename( namespace, "test-package-with-errors" );
        assertEquals( 1, items.size() );
        assertTrue(
            items.values().stream().allMatch( ItemLogRecord::hasError )
        );
    }

    @Test
    void install_should_not_set_error_flag_for_skipped_items() throws InvalidPackageIdException {
        final String namespace = "Namespace2";
        final PackageId testPackageId = PackageId.valueOf( "test-package-skip-item:0.1.0" );

        // first install: item will be installed
        Map<Class<?>, List<Event>> events = install( namespace, List.of( testPackageId ) );

        assertEquals(
            Status.Success,
            ( (PackageInstallationFinished)events.get( PackageInstallationFinished.class ).getFirst() ).status()
        );

        // second install: item will be skipped
        events = reinstall( namespace, List.of( testPackageId ) );

        assertEquals(
            Status.Success,
            ( (PackageInstallationFinished)events.get( PackageInstallationFinished.class ).getFirst() ).status()
        );

        // skipped item should have no error flag set
        final Map<String, ItemLogRecord> items = itemLog.getByPackagename( namespace, "test-package-skip-item" );
        assertEquals( 1, items.size() );
        assertTrue(
            items.values().stream().noneMatch( ItemLogRecord::hasError )
        );
    }

}
