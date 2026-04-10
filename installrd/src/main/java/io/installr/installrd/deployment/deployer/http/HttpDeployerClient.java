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
package io.installr.installrd.deployment.deployer.http;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.semver4j.Semver;

import io.installr.installrd.deployment.deployer.http.HttpDeployerClient.SemverConverterProvider;
import io.installr.lib.deployer.resource.DeployerResource;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Path( "/" )
@RegisterProvider( SemverConverterProvider.class )
public interface HttpDeployerClient extends DeployerResource {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class ErrorResponse {
        private String message;
    }

    @ClientExceptionMapper
    static RuntimeException toException( Response response ) {
        if ( response.hasEntity() ) {
            final ErrorResponse entity = response.readEntity( ErrorResponse.class );
            return new RuntimeException( entity.getMessage() );
        }

        return switch ( response.getStatusInfo().toEnum() ) {
            case BAD_REQUEST            -> new BadRequestException( response );
            case UNAUTHORIZED           -> new NotAuthorizedException( response );
            case FORBIDDEN              -> new ForbiddenException( response );
            case NOT_FOUND              -> new NotFoundException( response );
            case METHOD_NOT_ALLOWED     -> new NotAllowedException( response );
            case NOT_ACCEPTABLE         -> new NotAcceptableException( response );
            case UNSUPPORTED_MEDIA_TYPE -> new NotSupportedException( response );
            default                     -> new ServerErrorException( response );
        };
    }

    static class SemverConverterProvider implements ParamConverterProvider {

        @SuppressWarnings( "unchecked" )
        @Override
        public <T> ParamConverter<T> getConverter( Class<T> rawType, Type genericType, Annotation[] annotations ) {
            if ( rawType == Semver.class ) {
                return (ParamConverter<T>)new SemverParamConverter();
            }
            return null;
        }

    }

    static class SemverParamConverter implements ParamConverter<Semver> {

        @Override
        public Semver fromString( String value ) {
            return Semver.parse( value );
        }

        @Override
        public String toString( Semver value ) {
            return value.toString();
        }

    }

}
