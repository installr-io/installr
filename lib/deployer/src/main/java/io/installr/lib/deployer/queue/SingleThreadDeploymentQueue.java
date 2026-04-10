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
package io.installr.lib.deployer.queue;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import io.installr.lib.deployer.DeploymentResponse;
import io.installr.lib.deployer.DeploymentStatus;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class SingleThreadDeploymentQueue {

    private enum QueueState {
        Idle,
        Processing
    }

    private class Job {
        final Runnable task;
        final DeploymentResponse response;
        Instant finishedTimestamp;

        Job( Runnable task, DeploymentResponse response ) {
            this.task = task;
            this.response = response;
        }
    }

    private final AtomicReference<QueueState> state;
    private final Map<String, Job> jobQueue;
    private final ExecutorService executor;
    private final Duration retention;

    public SingleThreadDeploymentQueue() {
        this( Duration.ofSeconds( 30 ) );
    }

    public SingleThreadDeploymentQueue( Duration retention ) {
        this.state = new AtomicReference<>( QueueState.Idle );
        this.jobQueue = Collections.synchronizedMap( new LinkedHashMap<>() );
        this.executor = Executors.newSingleThreadExecutor();
        this.retention = retention;
    }

    public void enqueue( String id, Runnable task ) {
        final DeploymentResponse response = DeploymentResponse.builder()
            .status( DeploymentStatus.Queued )
            .build();
        jobQueue.put( id, new Job( task, response ) );
        cleanRetiredJobs();
        processJobQueue();
    }

    private void cleanRetiredJobs() {
        if ( jobQueue.isEmpty() ) {
            log.trace( "CLEAN: queue is empty" );
            return;
        }
        log.trace( "CLEAN: queue before" );
        jobQueue.forEach( ( id, job ) -> log.tracef( "%s: %s", id, job.response.getStatus() ) );
        jobQueue.entrySet().removeIf(
            entry -> entry.getValue().response.getStatus().isFinished()
                && entry.getValue().finishedTimestamp.isBefore( Instant.now().plus( retention ) )
        );
        log.trace( "CLEAN: queue after" );
        jobQueue.forEach( ( id, job ) -> log.tracef( "%s: %s", id, job.response.getStatus() ) );
    }

    private void processJobQueue() {
        if ( !state.compareAndSet( QueueState.Idle, QueueState.Processing ) ) {
            return;
        }
        executor.submit(
            () -> {
                try {
                    jobQueue.entrySet().stream()
                        .filter( this::filterJobQueue )
                        .forEach( this::processJob );
                }
                finally {
                    state.set( QueueState.Idle );
                    if ( hasUnprocessedJobs() ) {
                        processJobQueue();
                    }
                }
            }
        );
    }

    private boolean filterJobQueue( Map.Entry<String, Job> entry ) {
        return !entry.getValue().response.getStatus().isFinished();
    }

    private void processJob( Map.Entry<String, Job> entry ) {
        log.tracef( "job: %s", entry.getKey() );
        final Job job = entry.getValue();
        try {
            job.response.setStatus( DeploymentStatus.InProgress );
            job.task.run();
            job.response.setStatus( DeploymentStatus.Success );
        }
        catch ( Throwable t ) {
            job.response.setStatus( DeploymentStatus.Failed );
            job.response.setMessage( t.getMessage() );
        }
        finally {
            job.finishedTimestamp = Instant.now();
        }
    }

    private boolean hasUnprocessedJobs() {
        return jobQueue.entrySet().stream().anyMatch( this::filterJobQueue );
    }

    public DeploymentResponse getStatus( String id ) {
        if ( jobQueue.containsKey( id ) ) {
            return jobQueue.get( id ).response;
        }
        return null;
    }

}
