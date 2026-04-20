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
package io.installr.installrd.deployment.deployer.docker;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.json.JsonMapper;

import de.gesellix.docker.remote.api.ConfigReference;
import de.gesellix.docker.remote.api.IPAM;
import de.gesellix.docker.remote.api.Network;
import de.gesellix.docker.remote.api.NetworkCreateRequest;
import io.installr.installrd.deployment.DeploymentWarning;
import io.installr.installrd.util.interpolation.EnvironmentInterpolator;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentSpec;

public class DockerNetworkDeployer extends AbstractDockerDeployer {

    private final EnvironmentInterpolator interpolator;
    private final JsonMapper jsonMapper;

    public DockerNetworkDeployer( DockerDeployerConfig config ) {
        super( config );
        this.interpolator = new EnvironmentInterpolator();
        this.jsonMapper = new JsonMapper();
        jsonMapper.addMixIn( NetworkCreateRequest.class, NetworkCreateRequestMixIn.class );
    }

    @Override
    public void deploy( DeploymentSpec spec ) throws DeploymentException {
        final NetworkCreateRequest networkSpec = readNetworkSpec( spec );
        if ( existsNetwork( networkSpec ) ) {
            throw new DeploymentWarning( "Network '%s' already exists.", networkSpec.getName() );
        }
        else {
            client.createNetwork( networkSpec );
        }
    }

    @Override
    public void undeploy( DeploymentSpec spec ) throws DeploymentException {
        client.rmNetwork( readNetworkSpec( spec ).getName() );
    }

    private NetworkCreateRequest readNetworkSpec( DeploymentSpec spec ) throws DeploymentException {
        try {
            final String json = new String( spec.getData() );
            return jsonMapper
                .readValue( interpolator.interpolate( json, spec.getEnv() ), NetworkCreateRequest.class );
        }
        catch ( IOException ex ) {
            throw new DeploymentException( ex.toString() );
        }
    }

    private boolean existsNetwork( NetworkCreateRequest networkSpec ) {
        final List<Network> networks = client.networks( filter( "name", networkSpec.getName() ) ).getContent();
        return networks != null && !networks.isEmpty();
    }

    private String filter( final String name, final String value ) {
        return String.format( "{ \"%s\": [\"%s\"] }", name, value );
    }

    /**
     * We use this abstract class to "mirror" the constructor of NetworkCreateRequest an annotate each parameter with
     * the correct JsonProperty annotation.
     */
    static abstract class NetworkCreateRequestMixIn {
        @JsonCreator
        NetworkCreateRequestMixIn(
            @JsonProperty( "Name" ) String name,
            @JsonProperty( "CheckDuplicate" ) Boolean checkDuplicate,
            @JsonProperty( "Driver" ) String driver,
            @JsonProperty( "Scope" ) String scope,
            @JsonProperty( "Internal" ) Boolean internal,
            @JsonProperty( "Attachable" ) Boolean attachable,
            @JsonProperty( "Ingress" ) Boolean ingress,
            @JsonProperty( "ConfigOnly" ) Boolean configOnly,
            @JsonProperty( "ConfigFrom" ) ConfigReference configFrom,
            @JsonProperty( "IPAM" ) IPAM IPAM,
            @JsonProperty( "EnableIPv6" ) Boolean enableIPv6,
            @JsonProperty( "Options" ) Map<String, String> options,
            @JsonProperty( "Labels" ) Map<String, String> labels
        ) {
        }
    }

}
