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
package io.installr.installrd.deployment.deployer.liquibase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import liquibase.resource.AbstractResourceAccessor;
import liquibase.resource.OpenOptions;
import liquibase.resource.Resource;

/**
 * Liquibase ResourceAccessor that serves a single changelog file from an in-memory byte array, avoiding any filesystem
 * I/O.
 */
class ByteArrayResourceAccessor extends AbstractResourceAccessor {

    private final String filename;
    private final byte[] data;

    ByteArrayResourceAccessor( String filename, byte[] data ) {
        this.filename = filename;
        this.data = data;
    }

    @Override
    public List<Resource> getAll( String path ) throws IOException {
        if ( filename.equals( path ) || path.endsWith( "/" + filename ) ) {
            return List.of( new ByteArrayResource( filename, data ) );
        }
        return List.of();
    }

    @Override
    public List<Resource> search( String path, boolean recursive ) throws IOException {
        return List.of();
    }

    @Override
    public void close() {
        // no resources to release
    }

    @Override
    public List<String> describeLocations() {
        return List.of( "memory://" + filename );
    }

    private static final class ByteArrayResource implements Resource {

        private final String path;
        private final byte[] data;

        ByteArrayResource( String path, byte[] data ) {
            this.path = path;
            this.data = data;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public Resource resolve( String other ) {
            return this;
        }

        @Override
        public Resource resolveSibling( String other ) {
            return this;
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return new ByteArrayInputStream( data );
        }

        @Override
        public URI getUri() {
            return URI.create( "memory://" + path );
        }

        @Override
        public OutputStream openOutputStream( OpenOptions openOptions ) throws IOException {
            throw new IOException( "ByteArrayResource is read-only" );
        }

    }

}
