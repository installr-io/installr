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
package io.installr.lib.installrd.controller;

import java.util.List;

import org.jboss.resteasy.reactive.RestPath;

import io.installr.lib.installrd.pkg.PackageLogRecord;
import io.installr.lib.installrd.pkg.PackageLogRecord.Filter;
import io.installr.lib.installrd.util.Pagination;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path( "/" )
@Produces( MediaType.APPLICATION_JSON )
public interface PackageLogController {

    @GET
    @Path( "/namespaces/{namespace}/package-log" )
    List<PackageLogRecord> packageLog(
        @RestPath String namespace, @BeanParam Pagination pagination, @BeanParam Filter filter
    );

}
