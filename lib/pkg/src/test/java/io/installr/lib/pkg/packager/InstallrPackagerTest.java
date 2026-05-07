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
package io.installr.lib.pkg.packager;

import static io.installr.lib.pkg.packager.InstallrPackager.MEDIA_TYPE_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageId;

public class InstallrPackagerTest {

    private static final Path PACKAGE_SOURCES = Path.of( "target/test-classes/package-sources" );
    private static final Path GENERATED_PACKAGES = Path.of( "target/test-classes/generated-packages" );
    private static final PackageId TEST_PACKAGE_ID_1 = PackageId.valueOf( "test-package:0.1.0" );
    private static final PackageId TEST_PACKAGE_ID_2 = PackageId.valueOf( "test-package:0.2.0" );
    private static final PackageId TEST_PACKAGE_ID_3 = PackageId.valueOf( "test-package:0.3.0" );
    private static final PackageId TEST_PACKAGE_LABELS_ID = PackageId.valueOf( "test-package-labels:0.1.0" );
    private static final List<ContentAssignment> DUMMY_ASSIGNMENTS = List.of(
        new ContentAssignment( "**", InstallrPackager.MEDIA_TYPE_PREFIX + "unknown", "UTF-8", 999 )
    );

    @Test
    void makePackage_should_throw_exception_when_source_not_found() throws IOException {
        final PackagerConfig config = new PackagerConfig();
        config.setSource( Path.of( "not-found" ) );
        final InstallrPackager packager = new InstallrPackager( TEST_PACKAGE_ID_1, config );
        assertThrows( FileNotFoundException.class, () -> packager.makePackage() );
    }

    @Test
    void makePackage_should_create_an_archive_at_the_right_place() throws IOException {
        final PackagerConfig config = new PackagerConfig();
        config.setSource( PACKAGE_SOURCES.resolve( TEST_PACKAGE_ID_1.name() + "-" + TEST_PACKAGE_ID_1.version() ) );
        config.setTarget( GENERATED_PACKAGES );
        config.setContentAssignments( DUMMY_ASSIGNMENTS );
        final InstallrPackager packager = new InstallrPackager( TEST_PACKAGE_ID_1, config );
        Path archive = packager.makePackage();
        assertTrue( Files.exists( archive ) );
        assertTrue( archive.toString().startsWith( GENERATED_PACKAGES.toString() ) );
    }

    @Test
    void makePackage_should_create_an_archive_with_the_right_contents() throws IOException {
        final PackagerConfig config = new PackagerConfig();
        config.setSource( PACKAGE_SOURCES.resolve( TEST_PACKAGE_ID_2.name() + "-" + TEST_PACKAGE_ID_2.version() ) );
        config.setTarget( GENERATED_PACKAGES );
        config.setContentAssignments( DUMMY_ASSIGNMENTS );
        final InstallrPackager packager = new InstallrPackager( TEST_PACKAGE_ID_2, config );
        final Package pkg = Package.loadFromFile( packager.makePackage() );
        assertEquals( TEST_PACKAGE_ID_2, pkg.getPackageInfo().getPackageId() );
        assertNotNull( pkg.getPackageInfo().getContents() );
        assertEquals( 2, pkg.getPackageInfo().getContents().size() );
        assertEquals( "sql/0001-script.sql", pkg.getPackageInfo().getContents().get( 0 ).getPath() );
        assertEquals( "sql/0002-script.sql", pkg.getPackageInfo().getContents().get( 1 ).getPath() );
    }

    @Test
    void makePackage_should_create_an_archive_with_the_right_content_order() throws IOException {
        List<ContentAssignment> contentAssignments = List.of(
            new ContentAssignment( "docker/**", MEDIA_TYPE_PREFIX + "docker", "UTF-8", 1 ),
            new ContentAssignment( "sql/**", MEDIA_TYPE_PREFIX + "sql", "ISO-8859-1", 2 ),
            new ContentAssignment( "keycloak/**", MEDIA_TYPE_PREFIX + "sql", "UTF-8", 3 )
        );
        final PackagerConfig config = new PackagerConfig();
        config.setSource( PACKAGE_SOURCES.resolve( TEST_PACKAGE_ID_3.name() + "-" + TEST_PACKAGE_ID_3.version() ) );
        config.setTarget( GENERATED_PACKAGES );
        config.setContentAssignments( contentAssignments );
        final InstallrPackager packager = new InstallrPackager( TEST_PACKAGE_ID_3, config );
        final Package pkg = Package.loadFromFile( packager.makePackage() );
        assertEquals( TEST_PACKAGE_ID_3, pkg.getPackageInfo().getPackageId() );
        assertNotNull( pkg.getPackageInfo().getContents() );
        assertEquals( 3, pkg.getPackageInfo().getContents().size() );
        assertEquals( "docker/descriptor.yaml", pkg.getPackageInfo().getContents().get( 0 ).getPath() );
        assertEquals( "sql/script.sql", pkg.getPackageInfo().getContents().get( 1 ).getPath() );
        assertEquals( "keycloak/realm.json", pkg.getPackageInfo().getContents().get( 2 ).getPath() );
    }

    @Test
    void should_read_config_from() throws IOException {
        final ContentAssignment contentAssignment = new ContentAssignment(
            "docker/**",
            MEDIA_TYPE_PREFIX + "docker",
            "UTF-8",
            1
        );
        final Path configFile = Path.of( "target/test-classes/packager-config.yaml" );
        final InstallrPackager packager = new InstallrPackager( TEST_PACKAGE_ID_1, configFile );
        assertNotNull( packager.getConfig() );
        assertEquals( Path.of( "package-sources" ), packager.getConfig().getSource() );
        assertEquals( Path.of( "target/packages" ), packager.getConfig().getTarget() );
        assertEquals( contentAssignment, packager.getConfig().getContentAssignments().get( 0 ) );
    }

    @Test
    void config_should_have_default_values() throws IOException {
        final ContentAssignment contentAssignment = new ContentAssignment();
        contentAssignment.setPathPattern( "sql/**" );
        contentAssignment.setMediaType( MEDIA_TYPE_PREFIX + "sql" );
        // encoding == UTF-8
        // order == 999
        final Path configFile = Path.of( "target/test-classes/packager-config.yaml" );
        final InstallrPackager packager = new InstallrPackager( TEST_PACKAGE_ID_1, configFile );
        assertNotNull( packager.getConfig() );
        assertEquals( Path.of( "package-sources" ), packager.getConfig().getSource() );
        assertEquals( Path.of( "target/packages" ), packager.getConfig().getTarget() );
        assertEquals( contentAssignment, packager.getConfig().getContentAssignments().get( 1 ) );
    }

    @Test
    void makePackage_should_set_platform_labels_using_default_assignments() throws IOException {
        final PackagerConfig config = new PackagerConfig();
        config.setSource(
            PACKAGE_SOURCES.resolve(
                TEST_PACKAGE_LABELS_ID.name() + "-" + TEST_PACKAGE_LABELS_ID.version()
            )
        );
        config.setTarget( GENERATED_PACKAGES );
        final InstallrPackager packager = new InstallrPackager( TEST_PACKAGE_LABELS_ID, config );
        final Package pkg = Package.loadFromFile( packager.makePackage() );

        final io.installr.lib.pkg.PackageContent dockerItem = pkg.getPackageInfo().getContents().stream()
            .filter( c -> c.getPath().startsWith( "docker/" ) )
            .findFirst()
            .orElseThrow();
        assertEquals( Map.of( "platform", "docker-swarm" ), dockerItem.getLabels() );

        final io.installr.lib.pkg.PackageContent k8sItem = pkg.getPackageInfo().getContents().stream()
            .filter( c -> c.getPath().startsWith( "kubernetes/" ) )
            .findFirst()
            .orElseThrow();
        assertEquals( Map.of( "platform", "kubernetes" ), k8sItem.getLabels() );
    }

    @Test
    void makePackage_should_propagate_labels_from_custom_content_assignment() throws IOException {
        final List<ContentAssignment> assignments = List.of(
            new ContentAssignment( "sql/**", MEDIA_TYPE_PREFIX + "sql", "UTF-8", 1, Map.of( "type", "database" ) )
        );
        final PackagerConfig config = new PackagerConfig();
        config.setSource( PACKAGE_SOURCES.resolve( TEST_PACKAGE_ID_2.name() + "-" + TEST_PACKAGE_ID_2.version() ) );
        config.setTarget( GENERATED_PACKAGES );
        config.setContentAssignments( assignments );
        final InstallrPackager packager = new InstallrPackager( TEST_PACKAGE_ID_2, config );
        final Package pkg = Package.loadFromFile( packager.makePackage() );

        pkg.getPackageInfo().getContents().forEach(
            item -> assertEquals( Map.of( "type", "database" ), item.getLabels() )
        );
    }

    @Test
    void makePackage_should_not_set_labels_when_assignment_has_no_labels() throws IOException {
        final PackagerConfig config = new PackagerConfig();
        config.setSource( PACKAGE_SOURCES.resolve( TEST_PACKAGE_ID_2.name() + "-" + TEST_PACKAGE_ID_2.version() ) );
        config.setTarget( GENERATED_PACKAGES );
        config.setContentAssignments( DUMMY_ASSIGNMENTS );
        final InstallrPackager packager = new InstallrPackager( TEST_PACKAGE_ID_2, config );
        final Package pkg = Package.loadFromFile( packager.makePackage() );

        pkg.getPackageInfo().getContents().forEach(
            item -> assertNull( item.getLabels() )
        );
    }

}
