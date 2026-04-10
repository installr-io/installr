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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

import io.installr.installrd.common.http.auth.ApiKeyAuthConfig;
import io.installr.installrd.common.http.auth.AuthConfigType;
import io.installr.installrd.common.http.auth.NoAuthConfig;
import io.installr.installrd.common.http.auth.provider.HeaderValueProviderConfig;
import io.installr.installrd.common.http.auth.provider.ValueProviderType;

public class HttpDeployerConfigTest {

    @Test
    void test_serialize_from_json() throws IOException {
        String json = """
                {
                    "url": "http://foo.com",
                    "auth": {
                        "type": "api-key"
                    }
                }
            """;
        ObjectMapper om = new ObjectMapper();
        HttpDeployerConfig config = om.readValue( json, HttpDeployerConfig.class );
        assertEquals( AuthConfigType.ApiKey, config.getAuth().getType() );
        assertEquals( ApiKeyAuthConfig.class, config.getAuth().getClass() );
    }

    @Test
    void test_serialize_from_json_no_auth() throws IOException {
        String json = """
                {
                    "url": "http://foo.com"
                }
            """;
        ObjectMapper om = new ObjectMapper();
        HttpDeployerConfig config = om.readerFor( HttpDeployerConfig.class ).readValue( json );
        assertEquals( AuthConfigType.None, config.getAuth().getType() );
    }

    @Test
    void test_serialize_from_map() throws IOException {
        // @formatter:off
        Map<String, String> map = Map.of(
            "url", "http://foo.com",
            "auth.type", "api-key"
        );
        // @formatter:on
        JavaPropsMapper om = new JavaPropsMapper();
        HttpDeployerConfig config = om.readMapAs( map, HttpDeployerConfig.class );
        assertEquals( AuthConfigType.ApiKey, config.getAuth().getType() );
        assertEquals( ApiKeyAuthConfig.class, config.getAuth().getClass() );
    }

    @Test
    void test_serialize_from_map_without_auth_type() throws IOException {
        // @formatter:off
        Map<String, String> map = Map.of(
            "url", "http://foo.com"
        );
        // @formatter:on
        JavaPropsMapper om = new JavaPropsMapper();
        HttpDeployerConfig config = om.readMapAs( map, HttpDeployerConfig.class );
        assertEquals( AuthConfigType.None, config.getAuth().getType() );
        assertEquals( NoAuthConfig.class, config.getAuth().getClass() );
    }

    @Test
    void test_default_key_name() throws IOException {
        // @formatter:off
        Map<String, String> map = Map.of(
            "url", "http://foo.com",
            "auth.type", "api-key"
        );
        // @formatter:on
        JavaPropsMapper om = new JavaPropsMapper();
        HttpDeployerConfig config = om.readMapAs( map, HttpDeployerConfig.class );
        assertEquals( AuthConfigType.ApiKey, config.getAuth().getType() );
        final ApiKeyAuthConfig authConfig = (ApiKeyAuthConfig)config.getAuth();
        assertEquals( "x-api-key", authConfig.getKeyName() );
    }

    @Test
    void test_custom_key_name() throws IOException {
        // @formatter:off
        Map<String, String> map = Map.of(
            "url", "http://foo.com",
            "auth.type", "api-key",
            "auth.key-name", "x-auth-token"
        );
        // @formatter:on
        JavaPropsMapper om = new JavaPropsMapper();
        HttpDeployerConfig config = om.readMapAs( map, HttpDeployerConfig.class );
        assertEquals( AuthConfigType.ApiKey, config.getAuth().getType() );
        final ApiKeyAuthConfig authConfig = (ApiKeyAuthConfig)config.getAuth();
        assertEquals( "x-auth-token", authConfig.getKeyName() );
    }

    @Test
    void test_static_key_value() throws IOException {
        // @formatter:off
        Map<String, String> map = Map.of(
            "url", "http://foo.com",
            "auth.type", "api-key",
            "auth.key-value", "bar"
        );
        // @formatter:on
        JavaPropsMapper om = new JavaPropsMapper();
        HttpDeployerConfig config = om.readMapAs( map, HttpDeployerConfig.class );
        assertEquals( AuthConfigType.ApiKey, config.getAuth().getType() );
        final ApiKeyAuthConfig authConfig = (ApiKeyAuthConfig)config.getAuth();
        assertEquals( "bar", authConfig.getKeyValue() );
    }

    @Test
    void test_dynamic_key_value() throws IOException {
        // @formatter:off
        Map<String, String> map = Map.of(
            "url", "http://foo.com",
            "auth.type", "api-key",
            "auth.value-provider.type", "header-value",
            "auth.value-provider.header-name", "x-auth-token"
        );
        // @formatter:on
        JavaPropsMapper om = new JavaPropsMapper();
        HttpDeployerConfig config = om.readMapAs( map, HttpDeployerConfig.class );
        assertEquals( AuthConfigType.ApiKey, config.getAuth().getType() );
        final ApiKeyAuthConfig authConfig = (ApiKeyAuthConfig)config.getAuth();
        assertEquals( ValueProviderType.HeaderValue, authConfig.getValueProviderConfig().getType() );
        assertEquals( HeaderValueProviderConfig.class, authConfig.getValueProviderConfig().getClass() );
        final HeaderValueProviderConfig headerValCfg = (HeaderValueProviderConfig)authConfig.getValueProviderConfig();
        assertEquals( "x-auth-token", headerValCfg.getHeaderName() );
    }

    @Test
    void test_trust_store_config() throws IOException {
        // @formatter:off
        Map<String, String> map = Map.of(
            "url", "http://foo.com",
            "trust-store.path", "/tmp/foo.certs"
        );
        // @formatter:on
        JavaPropsMapper om = new JavaPropsMapper();
        HttpDeployerConfig config = om.readMapAs( map, HttpDeployerConfig.class );
        assertNotNull( config.getTrustStore() );
        assertEquals( "/tmp/foo.certs", config.getTrustStore().getPath().toString() );
        assertNull( config.getTrustStore().getPasswd() );
    }

}
