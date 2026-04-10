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
package io.installr.registry;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageId;
import io.installr.lib.pkg.packager.ContentAssignment;
import io.installr.lib.pkg.packager.InstallrPackager;
import io.installr.lib.pkg.packager.PackagerConfig;
import io.installr.lib.registry.PackageUpload;
import io.installr.lib.registry.RepositoryResource;
import io.installr.lib.registry.repository.Repository;
import io.installr.lib.registry.repository.RepositoryIndex;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class RegistryMockServer implements RepositoryResource {

    private static final List<ContentAssignment> DUMMY_ASSIGNMENTS = List.of(
        new ContentAssignment( "http/**", InstallrPackager.MEDIA_TYPE_PREFIX + "http-test", "UTF-8", 1 ),
        new ContentAssignment( "**", InstallrPackager.MEDIA_TYPE_PREFIX + "unknown", "UTF-8", 999 )
    );

    private Map<String, Repository> repositories;

    @Inject
    UriInfo uriInfo;

    @PostConstruct
    void init() {
        repositories = new HashMap<>();
        repositories.put( "test-repository", createTestRepository() );
    }

    @SneakyThrows
    private Repository createTestRepository() {
        final Path repoPath = Path.of(
            Thread.currentThread().getContextClassLoader().getResource( "repositories" ).toURI()
        );
        final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder()
            .path( "repositories" );
        final URI repoUri = uriBuilder.path( "test-repository" ).build();
        final RepositoryIndex index = new RepositoryIndex( "test-repository", repoPath, repoUri );

        makePackages( index );

        final Repository repo = new Repository();
        repo.setId( "test-repository" );
        repo.setUri( repoUri );
        repo.setIndex( index );
        return repo;
    }

    private void makePackages( RepositoryIndex index ) throws IOException {
        final Path repoPath = index.repoPath();
        log.debugf( "makePackages: repoPath = %s", repoPath );
        Files.walk( repoPath, 1, FileVisitOption.FOLLOW_LINKS )
            .filter(
                path -> Files.isDirectory( path )
                    && !path.equals( repoPath )
            )
            .forEach( path -> makePackage( index, path ) );
    }

    @SneakyThrows
    private void makePackage( RepositoryIndex index, Path packagePath ) {
        final Pattern pattern = Pattern.compile( "(.*)-(\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?)" );
        final Matcher matcher = pattern.matcher( packagePath.getFileName().toString() );
        if ( !matcher.matches() ) {
            log.debugf( "path does not match 'package-path' pattern: %s", packagePath );
            return;
        }
        final String packageName = matcher.group( 1 );
        final String version = matcher.group( 2 );
        final PackageId packageId = new PackageId( packageName, version );
        final PackagerConfig packagerConfig = new PackagerConfig();
        packagerConfig.setSource( packagePath );
        packagerConfig.setTarget( packagePath.resolve( ".." ) );
        packagerConfig.setContentAssignments( DUMMY_ASSIGNMENTS );
        final InstallrPackager packager = new InstallrPackager( packageId, packagerConfig );
        index.put( Package.loadFromFile( packager.makePackage() ) );
    }

    @Override
    public List<Repository> listRepositories() {
        return new ArrayList<>( repositories.values() );
    }

    @Override
    public Repository getRepository( String repositoryId ) {
        return repositories.get( repositoryId );
    }

    @Override
    public Uni<Response> index( String repositoryId ) {
        final Path indexFile = getRepository( repositoryId ).getIndex().indexFile();
        if ( !Files.exists( indexFile ) ) {
            return Uni.createFrom()
                .item( Response.serverError().status( 400, "index.gz not found" ).build() );
        }
        return Uni.createFrom().item(
            Response.ok( indexFile )
                .header( "Content-Disposition", "attachment; filename=index.gz" )
                .build()
        );
    }

    @Override
    public List<Package> getPackages( String repositoryId ) {
        return new ArrayList<>( getRepository( repositoryId ).getIndex().packages().values() );
    }

    @Override
    public Uni<Response> getPackage( String repositoryId, PackageId packageId ) {
        final Package pkg = getRepository( repositoryId ).getIndex().get( packageId );
        if ( pkg == null ) {
            throw new NotFoundException();
        }
        return Uni.createFrom().item(
            Response.ok( pkg.getLocalPath() )
                .header( "Content-Disposition", "attachment; filename=" + packageId.toArchiveName() )
                .build()
        );
    }

    @Override
    public Response putPackage( PackageUpload packageUpload ) {
        // unused
        throw new UnsupportedOperationException( "Unimplemented method 'putPackage'" );
    }

    @Override
    public Response deletePackage( String repositoryId, PackageId packageId ) {
        // unused
        throw new UnsupportedOperationException( "Unimplemented method 'deletePackage'" );
    }

}
