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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageContent;
import io.installr.lib.pkg.PackageId;
import io.installr.lib.pkg.PackageInfo;
import lombok.Data;

@Data
public class InstallrPackager {

    public static final String MEDIA_TYPE_PREFIX = "application/vnd.io.installr.";

    private final PackageId packageId;
    private final PackagerConfig config;
    private final ContentAssignmentMatcher contentAssignmentMatcher;

    public InstallrPackager( PackageId packageId, Path configFile ) throws IOException {
        this( packageId, new YAMLMapper().readValue( Files.readAllBytes( configFile ), PackagerConfig.class ) );
    }

    public InstallrPackager( PackageId packageId, PackagerConfig config ) {
        this.packageId = packageId;
        this.config = config;
        this.contentAssignmentMatcher = new ContentAssignmentMatcher( contentAssignments() );
    }

    private List<ContentAssignment> contentAssignments() {
        final List<ContentAssignment> contentAssignments = new ArrayList<>();
        if ( config.getContentAssignments() != null ) {
            contentAssignments.addAll( config.getContentAssignments() );
        }
        contentAssignments.addAll( defaultContentAssignments() );
        return contentAssignments;
    }

    private List<ContentAssignment> defaultContentAssignments() {
        return List.of(
            new ContentAssignment(
                "docker/*.network.json",
                "application/vnd.io.installr.docker-network",
                "UTF-8",
                5
            ),
            new ContentAssignment(
                "docker/**/*.network.json",
                "application/vnd.io.installr.docker-network",
                "UTF-8",
                5
            ),
            new ContentAssignment(
                "docker/**",
                "application/vnd.io.installr.docker-stack",
                "UTF-8",
                10
            )
        );
    }

    public Path makePackage() throws IOException {
        if ( !Files.exists( config.getSource() ) ) {
            throw new FileNotFoundException( "Source does not exist: " + config.getSource() );
        }
        if ( !Files.exists( config.getTarget() ) ) {
            Files.createDirectory( config.getTarget() );
        }
        PackageInfo pkgInfo = new PackageInfo();
        pkgInfo.setPackageId( packageId );
        pkgInfo.setContents(
            Files.walk( config.getSource(), FileVisitOption.FOLLOW_LINKS )
                .filter(
                    path -> !Files.isDirectory( path )
                        && !path.equals( config.getTarget() )
                        // ignore package defaults
                        && !path.equals( config.getSource().resolve( Package.PACKAGE_DEFAULTS_YAML ) )
                )
                .map( this::toPackageContent )
                .sorted( new PackageContentComparator() )
                .collect( Collectors.toList() )
        );
        return createArchive( pkgInfo );
    }

    private PackageContent toPackageContent( Path fullPath ) {
        final Path relativePath = config.getSource().relativize( fullPath );
        final String pathName = relativePath.toString().replace( "\\", "/" );
        final ContentAssignment contentAssignment = contentAssignmentMatcher.determineContentAssignment( relativePath );
        final PackageContent content = new PackageContent();
        content.setPath( pathName );
        content.setHash( generateFileHash( fullPath ) );
        content.setMediaType( contentAssignment.getMediaType() );
        content.setEncoding( contentAssignment.getEncoding() );
        content.setOrder( contentAssignment.getOrder() );
        return content;
    }

    private String generateFileHash( Path file ) {
        // https://stackoverflow.com/questions/552659/how-to-assign-a-git-sha1s-to-a-file-without-git/552725#552725
        try {
            byte[] data = Files.readAllBytes( file );
            byte[] header = ( "blob " + data.length + '\0' ).getBytes();
            try ( ByteArrayOutputStream os = new ByteArrayOutputStream() ) {
                os.write( header );
                os.write( data );
                return DigestUtils.sha1Hex( os.toByteArray() );
            }
        }
        catch ( IOException ex ) {
            // this should never happen
            // log.error( "Could not generate file hash: {}", ex );
            return null;
        }
    }

    public Path createArchive( PackageInfo pkgInfo ) throws IOException {
        final Path packageFile = config.getTarget().resolve( pkgInfo.getPackageId().toArchiveName() );
        try (
            TarArchiveOutputStream tos = new TarArchiveOutputStream(
                new GzipCompressorOutputStream(
                    new BufferedOutputStream(
                        Files.newOutputStream( packageFile )
                    )
                )
            )
        ) {
            tos.setLongFileMode( 2 );
            createEntry( tos, Package.PACKAGE_INFO_YAML, createYaml( pkgInfo ) );
            if ( config.getSource() != null && pkgInfo.getContents() != null ) {
                for ( PackageContent content : pkgInfo.getContents() ) {
                    final String path = content.getLocationInArchive();
                    createEntry( tos, path, Files.readAllBytes( config.getSource().resolve( path ) ) );
                }
                putDefaultsYaml( tos );
            }
        }
        return packageFile;
    }

    private void createEntry( TarArchiveOutputStream tos, String name, byte[] bytes ) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry( name );
        entry.setSize( bytes.length );
        tos.putArchiveEntry( entry );
        tos.write( bytes );
        tos.closeArchiveEntry();
    }

    private byte[] createYaml( Object obj ) throws JsonProcessingException {
        return new YAMLMapper()
            .enable( SerializationFeature.INDENT_OUTPUT )
            .writeValueAsBytes( obj );
    }

    private void putDefaultsYaml( TarArchiveOutputStream tos ) throws IOException {
        final Path path = config.getSource().resolve( Package.PACKAGE_DEFAULTS_YAML );
        if ( !Files.exists( path ) ) {
            return;
        }
        createEntry( tos, path.getFileName().toString(), Files.readAllBytes( path ) );
    }

}
