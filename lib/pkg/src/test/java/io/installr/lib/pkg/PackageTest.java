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
package io.installr.lib.pkg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.installr.lib.pkg.packager.InstallrPackager;
import io.installr.lib.pkg.packager.PackagerConfig;
import io.installr.lib.util.FileUtil;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class PackageTest {

    @Test
    void packageInfo_should_contain_empty_lists_instead_of_null() throws IOException {
        // create directory
        final Path target = Path.of( "target/test-classes/empty-package" ).toAbsolutePath();
        log.infof( "target: %s", target );
        FileUtil.mkdir( target );
        try {
            // create package-info.yaml
            final PackageId pkgId = PackageId.valueOf( "empty-package:1.0.0" );
            final PackageInfo pkgInfo = new PackageInfo();
            pkgInfo.setPackageId( pkgId );
            Files.write( target.resolve( Package.PACKAGE_INFO_YAML ), new YAMLMapper().writeValueAsBytes( pkgInfo ) );
            // create package/archive
            final PackagerConfig config = new PackagerConfig();
            config.setSource( target );
            config.setTarget( Path.of( "target/test-classes" ) );
            final InstallrPackager packager = new InstallrPackager( pkgId, config );
            final Path archive = packager.createArchive( pkgInfo );
            assertTrue( Files.exists( archive ) );

            // test
            final Package pkg = Package.loadFromFile( archive );
            assertNotNull( pkg );
            assertNotNull( pkg.getPackageInfo() );
            assertEquals( pkgId, pkg.getPackageInfo().getPackageId() );
            assertNotNull( pkg.getPackageInfo().getContents() );
            assertEquals( 0, pkg.getPackageInfo().getContents().size() );
        }
        finally {
            FileUtil.rmdir( target );
        }
    }

}
