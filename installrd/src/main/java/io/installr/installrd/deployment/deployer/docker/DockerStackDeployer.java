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

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.client.stack.DeployConfigReader;
import de.gesellix.docker.client.stack.DeployStackConfig;
import de.gesellix.docker.client.stack.DeployStackOptions;
import de.gesellix.docker.remote.api.Service;
import de.gesellix.docker.remote.api.Task;
import de.gesellix.docker.remote.api.TaskState;
import de.gesellix.docker.remote.api.TaskStatus;
import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.deployment.deployer.BuiltInDeployer;
import io.installr.installrd.util.PeriodicEvaluator;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentSpec;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;

@Dependent
@BuiltInDeployer( "DockerStackDeployer" )
@JBossLog
public class DockerStackDeployer extends AbstractDockerDeployer {

    @Inject
    InstallrdConfiguration installrdConfig;

    private static final String MEM_REGEX = "\\d+([.]\\d+)?[KkMmGg]";

    private DeployConfigReader reader;
    private DeployStackOptions options;

    @Override
    public void init( DockerDeployerConfig config ) {
        super.init( config );
        this.reader = new DeployConfigReader( this.client );
        this.options = new DeployStackOptions();
        this.options.setSendRegistryAuth( true );
    }

    @Override
    public void deploy( DeploymentSpec spec ) throws DeploymentException {
        final String stack = genStackName( spec );
        if ( existsStack( stack ) ) {
            undeploy( spec );
        }
        log.debugf( "Deploying stack '%s...", stack );
        final Map<String, String> env = convertValues( spec.getEnv() );
        final DeployStackConfig stackConfig = reader.loadCompose(
            stack,
            new ByteArrayInputStream( spec.getData() ),
            installrdConfig.location().toAbsolutePath().toString(),
            env == null ? new HashMap<>() : env
        );
        client.stackDeploy( stack, stackConfig, options );
        final boolean running = new PeriodicEvaluator<>( () -> taskStateStream( stack ), checkAllTasksRunning() )
            .withInterval( config.getPollingInterval() )
            .withTimeout( config.getPollingTimeout() )
            .evaluate();
        if ( !running ) {
            throw new DeploymentException( "Could not deploy '%s'. See 'docker service logs' for details.", stack );
        }
        log.debug( "  => done" );
    }

    @Override
    public void undeploy( DeploymentSpec spec ) throws DeploymentException {
        final String stack = genStackName( spec );
        log.debugf( "Un-deploying stack '%s...", stack );
        // determine all task IDs
        final List<String> taskIds = taskStream( stack ).map( Task::getID ).toList();
        // remove stack
        client.stackRm( stack );
        // and check if all tasks have been removed
        final boolean successful = new PeriodicEvaluator<>( () -> taskIds.stream(), checkAllTasksRemoved() )
            .withInterval( config.getPollingInterval() )
            .withTimeout( Duration.ofSeconds( 15 ) )
            .evaluate();
        if ( !successful ) {
            throw new DeploymentException( "Could not undeploy '%s'. See 'docker service logs' for details.", stack );
        }
        log.debug( "  => done" );
    }

    private String genStackName( DeploymentSpec spec ) {
        final String namespace = spec.getEnv().get( "namespace" );
        if ( namespace == null || namespace.isBlank() ) {
            throw new IllegalStateException( "Namespace must be provided via env." );
        }
        final String name = new String( Base64.getDecoder().decode( spec.getName() ) )
            .split( "/" )[ 0 ];
        return String.format( "%s-%s", namespace, name );
    }

    private boolean existsStack( String stack ) {
        return client.lsStacks().stream()
            .anyMatch( x -> x.getName().equals( stack ) );
    }

    private Map<String, String> convertValues( Map<String, String> env ) {
        if ( env == null ) {
            return null;
        }
        return env.entrySet().stream()
            .map(
                entry -> {
                    final String val = entry.getValue();
                    if ( val.matches( MEM_REGEX ) ) {
                        return Map.entry( entry.getKey(), toByteRepresentation( val ) );
                    }
                    return entry;
                }
            )
            .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
    }

    static String toByteRepresentation( String val ) {
        if ( val == null ) {
            return null;
        }
        if ( !val.matches( MEM_REGEX ) ) {
            return null;
        }
        String[] parts = val.split( "(?<=\\[KkMmGg])(?=\\d)|(?<=\\d)(?=[KkMmGg])" );
        double num = Double.parseDouble( parts[ 0 ] );
        String unit = parts[ 1 ];
        long bytes;
        switch ( unit ) {
            case "K":
            case "k":
                bytes = (long)( num * 1024 );
                break;
            case "M":
            case "m":
                bytes = (long)( num * 1024 * 1024 );
                break;
            case "G":
            case "g":
                bytes = (long)( num * 1024 * 1024 * 1024 );
                break;
            default:
                bytes = 0;
        }
        return String.valueOf( bytes );
    }

    private Stream<TaskState> taskStateStream( final String stackName ) {
        return taskStream( stackName )
            .map( Task::getStatus )
            .map( this::handleErrors )
            .map( TaskStatus::getState );
    }

    private Stream<Task> taskStream( final String stackName ) {
        return client.services( filter( "name", stackName ) )
            .getContent()
            .stream()
            .map( Service::getID )
            .map( client::tasksOfService )
            .map( EngineResponseContent::getContent )
            .flatMap( List::stream )
            .sorted( new TaskComparator() );
    }

    private String filter( final String name, final String value ) {
        return String.format( "{ \"%s\": [\"%s\"] }", name, value );
    }

    private TaskStatus handleErrors( TaskStatus taskStatus ) {
        return switch ( taskStatus.getState() ) {
            case Failed, Rejected -> throw new DeploymentException( taskStatus.getErr() );
            default               -> taskStatus;
        };
    }

    private Predicate<Stream<TaskState>> checkAllTasksRunning() {
        return ( stream ) -> stream.allMatch( state -> state == TaskState.Running );
    }

    private Predicate<Stream<String>> checkAllTasksRemoved() {
        return ( stream ) -> stream.allMatch( id -> client.tasks( filter( "id", id ) ).getContent().isEmpty() );
    }

    private static class TaskComparator implements Comparator<Task> {

        @Override
        public int compare( Task t1, Task t2 ) {
            int slot1 = t1.getSlot() == null ? t1.getNodeID().hashCode() : t1.getSlot();
            int slot2 = t2.getSlot() == null ? t2.getNodeID().hashCode() : t2.getSlot();
            int compare = Integer.compare( slot1, slot2 );
            if ( compare == 0 ) {
                ZonedDateTime zdt1 = ZonedDateTime.parse( t1.getCreatedAt() );
                ZonedDateTime zdt2 = ZonedDateTime.parse( t2.getCreatedAt() );
                return zdt2.compareTo( zdt1 );
            }
            return compare;
        }

    }

}
