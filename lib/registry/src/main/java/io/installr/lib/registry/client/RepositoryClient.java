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
package io.installr.lib.registry.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageId;
import io.installr.lib.registry.PackageUpload;
import io.installr.lib.registry.repository.Repository;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class RepositoryClient {

    private final RepositoryClientResource client;

    public RepositoryClient( URL url ) {
        this( url, null, null );
    }

    public RepositoryClient( URL url, String username, String password ) {
        final RestClientBuilder builder = RestClientBuilder.newBuilder()
            .baseUrl( url );
        if ( username != null && password != null ) {
            builder.register( new BasicAuthRequestFilter( username, password ) );
        }
        this.client = builder
            .build( RepositoryClientResource.class );
    }

    public Repository getRepository() {
        return client.getRepository();
    }

    public Path downloadIndex( Path dest ) {
        try ( Response response = client.index().subscribe().asCompletionStage().get() ) {
            if ( response.getStatusInfo().getFamily() == Family.SUCCESSFUL ) {
                // TODO: get filename from header
                // response.getStringHeaders().get( "Content-Disposition" ).get( 0 )
                final String filename = "index.gz";
                final Path indexFile = dest.resolve( filename );
                Files.write( indexFile, response.readEntity( InputStream.class ).readAllBytes() );
                return indexFile;
            }
            // TODO: get error from entity (ErrorResource)
            throw new IllegalStateException( "download error" );
        }
        catch ( InterruptedException | ExecutionException | IOException ex ) {
            throw new IllegalStateException( ex );
        }
    }

    public List<Package> getPackages() {
        return client.getPackages();
    }

    public Path downloadPackage( Path dest, PackageId packageId ) {
        try ( Response response = client.getPackage( packageId ).subscribe().asCompletionStage().get() ) {
            if ( response.getStatusInfo().getFamily() == Family.SUCCESSFUL ) {
                final Path pkgFile = dest.resolve( packageId.toArchiveName() );
                log.debugf( "write file to: %s", pkgFile );
                byte[] data = response.readEntity( InputStream.class ).readAllBytes();
                log.debugf( "size: %d", data.length );
                Files.write( pkgFile, data );
                log.debugf( "file successfully downloaded", pkgFile );
                return pkgFile;
            }
            log.debugf( "error downloading file for packageId: %s", packageId );
            // TODO: get error from entity (ErrorResource)
            throw new IllegalStateException( "download error" );
        }
        catch ( InterruptedException | ExecutionException | IOException ex ) {
            throw new IllegalStateException( ex );
        }
    }

    public void uploadPackage( PackageUpload packageUpload ) {
        client.putPackage( packageUpload );
    }

    public void deletePackage( PackageId packageId ) {
        client.deletePackage( packageId );
    }

}
