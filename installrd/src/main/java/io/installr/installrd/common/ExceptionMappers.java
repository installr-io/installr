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
package io.installr.installrd.common;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.quarkus.security.UnauthorizedException;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
class ExceptionMappers {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ErrorResponse {
        private String message;
    }

    private RestResponse<ErrorResponse> toErrorResponse( Status status, Throwable ex ) {
        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage( ex.getMessage() );
        return ResponseBuilder.create( status, errorResponse ).type( MediaType.APPLICATION_JSON ).build();
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapBadRequestException( BadRequestException ex ) {
        return toErrorResponse( Status.BAD_REQUEST, ex );
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapUnauthorizedException( UnauthorizedException ex ) {
        return toErrorResponse( Status.UNAUTHORIZED, ex );
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapForbiddenException( ForbiddenException ex ) {
        return toErrorResponse( Status.FORBIDDEN, ex );
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapNotFoundException( NotFoundException ex ) {
        return toErrorResponse( Status.NOT_FOUND, ex );
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapInternalServerError( InternalServerErrorException ex ) {
        return toErrorResponse( Status.INTERNAL_SERVER_ERROR, ex );
    }

    @ServerExceptionMapper( value = InstallrException.class )
    RestResponse<ErrorResponse> mapInstallrException( InstallrException ex ) {
        return toErrorResponse( ex.status(), ex );
    }

    @ServerExceptionMapper( value = Throwable.class )
    RestResponse<ErrorResponse> mapThrowable( Throwable ex ) {
        log.error( ex.getMessage() );
        ex.printStackTrace();
        return toErrorResponse( Status.INTERNAL_SERVER_ERROR, ex );
    }

    @ServerExceptionMapper( value = ValidationException.class )
    RestResponse<ErrorResponse> mapValidationException( ValidationException ex ) {
        return toErrorResponse( Status.BAD_REQUEST, ex );
    }

}
