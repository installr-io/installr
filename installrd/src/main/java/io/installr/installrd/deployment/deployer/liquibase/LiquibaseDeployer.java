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
package io.installr.installrd.deployment.deployer.liquibase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import io.installr.installrd.deployment.deployer.BuiltInDeployer;
import io.installr.lib.deployer.AbstractConfigurableDeployer;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentSpec;
import jakarta.enterprise.context.Dependent;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;

/**
 * Deployer that applies a Liquibase changelog (SQL, XML, YAML or JSON) against a configured JDBC data source.
 *
 * <p>
 * On {@link #deploy} the changelog carried in {@code spec.getData()} is executed via {@code liquibase.update()}. If
 * {@code tag-on-deploy} is enabled (default), a tag {@code <name>:<version>} is written to {@code DATABASECHANGELOG}
 * <em>before</em> the update, allowing a precise rollback with {@link #undeploy}.
 *
 * <p>
 * On {@link #undeploy} {@code liquibase.rollback(tag)} is called, which reverses all changesets that were applied after
 * the tag was set.
 */
@Dependent
@BuiltInDeployer( "LiquibaseDeployer" )
public class LiquibaseDeployer extends AbstractConfigurableDeployer<LiquibaseDeployerConfig> {

    @Override
    public Class<LiquibaseDeployerConfig> getConfigType() {
        return LiquibaseDeployerConfig.class;
    }

    @Override
    public void deploy( DeploymentSpec spec ) throws DeploymentException {
        withLiquibase( spec, liquibase -> {
            if ( config.isTagOnDeploy() ) {
                liquibase.tag( tag( spec ) );
            }
            liquibase.update( new Contexts() );
        } );
    }

    @Override
    public void undeploy( DeploymentSpec spec ) throws DeploymentException {
        if ( !config.isTagOnDeploy() ) {
            throw new DeploymentException( "Rollback not supported: tag-on-deploy is disabled." );
        }
        withLiquibase( spec, liquibase -> liquibase.rollback( tag( spec ), new Contexts() ) );
    }

    private void withLiquibase( DeploymentSpec spec, LiquibaseOperation operation ) throws DeploymentException {
        try {
            final Connection conn = openConnection( spec.getEnv() );
            final Database db = database( conn );
            try (
                Liquibase liquibase = new Liquibase(
                    spec.getName(),
                    new ByteArrayResourceAccessor( spec.getName(), spec.getData() ),
                    db
                )
            ) {
                spec.getEnv().forEach( liquibase::setChangeLogParameter );
                operation.execute( liquibase );
            }
        }
        catch ( LiquibaseException | SQLException ex ) {
            throw new DeploymentException( "%s", ex.getMessage() );
        }
    }

    @FunctionalInterface
    private interface LiquibaseOperation {
        void execute( Liquibase liquibase ) throws LiquibaseException, SQLException;
    }

    private Connection openConnection( Map<String, String> env ) throws SQLException {
        return DriverManager.getConnection(
            config.getUrl(),
            env.getOrDefault( "liquibase.username", config.getUsername() ),
            env.getOrDefault( "liquibase.password", config.getPassword() )
        );
    }

    private Database database( Connection conn ) throws LiquibaseException {
        return DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation( new JdbcConnection( conn ) );
    }

    private String tag( DeploymentSpec spec ) {
        return spec.getName() + ":" + spec.getVersion();
    }

    @Override
    public DeploymentException translateException( Throwable ex ) {
        if ( ex instanceof LiquibaseException ) {
            return new DeploymentException( "%s", ex.getMessage() );
        }
        if ( ex instanceof SQLException sqlEx ) {
            return new DeploymentException(
                "Database error [%s]: %s",
                sqlEx.getSQLState(),
                sqlEx.getMessage()
            );
        }
        return super.translateException( ex );
    }

}
