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
package io.installr.installrd.deployment.deployer.kubernetes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.installr.installrd.InstallrClient;
import io.installr.lib.installrd.event.Event;
import io.installr.lib.installrd.event.Events.TransactionFinished;
import io.installr.lib.installrd.event.Status;
import io.installr.lib.installrd.resource.InstallResource;
import io.installr.lib.pkg.PackageId;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
/**
 * This test needs a working kubernetes cluster, so tests will not be executed automatically.
 */
@Disabled
public class KubernetesInstallIT {

    private static final String TEST_NAMESPACE = "Namespace1";

    @Inject
    @RestClient
    InstallrClient installrClient;

    @Test
    void install_package_with_kubernetes_resources_should_succeed() {
        final PackageId testPackageId = PackageId.valueOf( "k8s-test-package:1.0.0" );

        final InstallResource resource = new InstallResource();
        resource.setPackageIds( List.of( testPackageId ) );
        Map<Class<?>, List<Event>> events = installrClient
            .install( TEST_NAMESPACE, resource )
            .onItem().invoke( event -> System.out.println( event ) )
            .subscribe()
            .asStream()
            .collect( Collectors.groupingBy( Event::getClass ) );

        final TransactionFinished txf = (TransactionFinished)events.get( TransactionFinished.class )
            .getFirst();
        assertEquals( Status.Type.Success, txf.status().type() );
    }

}
