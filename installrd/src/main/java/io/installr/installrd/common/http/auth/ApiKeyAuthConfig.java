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
package io.installr.installrd.common.http.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.installr.installrd.common.http.auth.provider.HeaderValueProvider;
import io.installr.installrd.common.http.auth.provider.HeaderValueProviderConfig;
import io.installr.installrd.common.http.auth.provider.ValueProviderConfig;
import io.quarkus.arc.Arc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.jbosslog.JBossLog;

@Data
@EqualsAndHashCode( callSuper = false )
@JBossLog
public class ApiKeyAuthConfig extends AuthConfig {

    @JsonProperty( "key-name" )
    private String keyName = "x-api-key"; // default

    @JsonProperty( "key-value" )
    private String keyValue;

    @JsonProperty( "value-provider" )
    private ValueProviderConfig valueProviderConfig;

    public String getKeyValue() {
        String value;
        if ( valueProviderConfig == null ) {
            log.trace( "no valueProvider configured" );
            value = keyValue;
        }
        else {
            log.tracef( "   value-provider: %s", valueProviderConfig.getType() );
            switch ( valueProviderConfig.getType() ) {
                case HeaderValue: {
                    final HeaderValueProvider provider = Arc.container()
                        .instance( HeaderValueProvider.class )
                        .get();
                    provider.setConfig( (HeaderValueProviderConfig)valueProviderConfig );
                    value = provider.value();
                    break;
                }
                default:
                    value = keyValue;
                    break;
            }
        }
        log.tracef( "   value: %s", value );
        return value;
    }

}
