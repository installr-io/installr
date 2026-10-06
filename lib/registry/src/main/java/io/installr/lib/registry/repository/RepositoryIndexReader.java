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
package io.installr.lib.registry.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.yaml.snakeyaml.LoaderOptions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageId;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class RepositoryIndexReader {

    private static final YAMLMapper yamlMapper;
    static {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit( 100 * 1024 * 1024 ); // 100 MB
        yamlMapper = new YAMLMapper(
            YAMLFactory.builder()
                .loaderOptions( loaderOptions )
                .build()
        );
    }

    private final String repoId;
    private final Path indexFile;
    private final Path packagePath;

    public RepositoryIndexReader( String repoId, Path indexFile, Path packagePath ) {
        this.repoId = repoId;
        this.indexFile = indexFile;
        this.packagePath = packagePath;
    }

    public Map<PackageId, Package> readIndex() {
        try {
            if ( !Files.exists( indexFile ) ) {
                log.warnf( "index file not found: %s", indexFile );
                return Map.of();
            }
            try ( GzipCompressorInputStream is = new GzipCompressorInputStream( Files.newInputStream( indexFile ) ) ) {
                return yamlMapper
                    .readValue( is.readAllBytes(), new TypeReference<List<Package>>() {} )
                    .stream()
                    .map(
                        pkg -> {
                            pkg.setLocalPath(
                                packagePath.resolve( pkg.getPackageInfo().getPackageId().toArchiveName() )
                            );
                            pkg.setRepositoryId( repoId );
                            return pkg;
                        }
                    )
                    .collect( Collectors.toMap( k -> k.getPackageInfo().getPackageId(), v -> v ) );
            }
        }
        catch ( IOException ex ) {
            log.errorf( ex.toString() );
            throw new IllegalStateException( ex );
        }
    }

}
