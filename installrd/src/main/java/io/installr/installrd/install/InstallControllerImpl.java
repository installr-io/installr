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
package io.installr.installrd.install;

import java.util.function.Consumer;

import org.eclipse.microprofile.context.ManagedExecutor;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.security.NamespacePermission;
import io.installr.installrd.security.Operation.Names;
import io.installr.installrd.transaction.Transaction;
import io.installr.installrd.transaction.TransactionService;
import io.installr.lib.installrd.controller.InstallController;
import io.installr.lib.installrd.event.Event;
import io.installr.lib.installrd.resource.InstallFileResource;
import io.installr.lib.installrd.resource.InstallPackageResource;
import io.quarkus.security.PermissionsAllowed;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InstallControllerImpl implements InstallController {

    @Inject
    TransactionService txService;

    @Inject
    InstallrdConfiguration installrdConfig;

    @Inject
    ManagedExecutor executor;

    @Inject
    InstallService installer;

    @Override
    @PermissionsAllowed( value = Names.PKG_INSTALL, permission = NamespacePermission.class, params = "namespace" )
    public Multi<Event> install( String namespace, InstallPackageResource resource ) {
        return handleInstall( namespace, ( tx ) -> installer.install( tx, resource.getPackageIds() ) );
    }

    @Override
    @PermissionsAllowed( value = Names.PKG_INSTALL_FILE, permission = NamespacePermission.class, params = "namespace" )
    public Multi<Event> install( String namespace, InstallFileResource resource ) {
        return handleInstall( namespace, ( tx ) -> installer.installFile( tx, resource.getFiles() ) );
    }

    @Override
    @PermissionsAllowed( value = Names.PKG_REINSTALL, permission = NamespacePermission.class, params = "namespace" )
    public Multi<Event> reinstall( String namespace, InstallPackageResource resource ) {
        return handleInstall( namespace, ( tx ) -> installer.reinstall( tx, resource.getPackageIds() ) );
    }

    @Override
    @PermissionsAllowed(
        value = Names.PKG_REINSTALL_FILE,
        permission = NamespacePermission.class,
        params = "namespace"
    )
    public Multi<Event> reinstall( String namespace, InstallFileResource resource ) {
        return handleInstall( namespace, ( tx ) -> installer.reinstallFile( tx, resource.getFiles() ) );
    }

    private Multi<Event> handleInstall( String namespace, Consumer<Transaction> consumer ) {
        final Transaction tx = txService.acquireTransaction();
        final Multi<Event> multiEvent = Multi.createFrom().emitter(
            emitter -> asyncExec( tx, consumer, emitter )
        );
        return multiEvent;
    }

    private void asyncExec(
        Transaction tx, Consumer<Transaction> consumer, MultiEmitter<? super Event> emitter
    ) {
        executor.runAsync(
            () -> {
                try {
                    tx.begin( emitter );
                    try {
                        consumer.accept( tx );
                    }
                    catch ( Throwable t ) {
                        tx.fail( t );
                    }
                    finally {
                        tx.end();
                    }
                }
                catch ( Throwable t ) {
                    tx.reset( t );
                    emitter.fail( t );
                }
            }
        );
    }

    @Override
    @PermissionsAllowed( value = Names.TX_CANCEL, permission = NamespacePermission.class, params = "namespace" )
    public void cancel( String namespace ) {
        txService.currentTransaction().cancel();
    }

}
