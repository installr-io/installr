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
package io.installr.lib.deployer;

import java.util.Map;

import org.jboss.resteasy.reactive.PartType;
import org.semver4j.Semver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import lombok.Data;

@Data
public class DeploymentSpec {

    @NotBlank
    @PathParam( "name" )
    private String name;

    @NotNull
    @FormParam( "version" )
    private Semver version;

    @NotNull
    @FormParam( "data" )
    @PartType( MediaType.APPLICATION_OCTET_STREAM )
    private byte[] data;

    @NotBlank
    @FormParam( "hash" )
    private String hash;

    @NotBlank
    @FormParam( "media-type" )
    private String mediaType;

    @NotNull
    @FormParam( "env" )
    @PartType( MediaType.APPLICATION_JSON )
    private Map<String, String> env;

    @FormParam( "encoding" )
    private String encoding;

}
