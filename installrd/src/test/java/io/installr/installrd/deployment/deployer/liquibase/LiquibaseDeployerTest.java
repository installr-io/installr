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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.semver4j.Semver;

import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentSpec;

class LiquibaseDeployerTest {

    // -------------------------------------------------------------------------
    // Changelogs
    // -------------------------------------------------------------------------

    private static final String SIMPLE_CHANGELOG = """
        -- liquibase formatted sql

        -- changeset deployer-test:create-person
        CREATE TABLE PERSON (ID INT PRIMARY KEY, NAME VARCHAR(255));
        -- rollback DROP TABLE PERSON;
        """;

    private static final String PARAMETERIZED_CHANGELOG = """
        -- liquibase formatted sql

        -- changeset deployer-test:create-prefixed-table
        CREATE TABLE ${prefix}_AUDIT (ID INT PRIMARY KEY, MESSAGE VARCHAR(255));
        -- rollback DROP TABLE ${prefix}_AUDIT;
        """;

    private static final String INVALID_CHANGELOG = """
        -- liquibase formatted sql

        -- changeset deployer-test:bad-sql
        THIS IS NOT VALID SQL;
        """;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void deploy_should_apply_changelog() throws Exception {
        final String dbUrl = uniqueDbUrl();
        deployer( dbUrl ).deploy( spec( SIMPLE_CHANGELOG ) );
        assertTrue( tableExists( dbUrl, "PERSON" ) );
    }

    @Test
    void deploy_should_be_idempotent() {
        final String dbUrl = uniqueDbUrl();
        final DeploymentSpec spec = spec( SIMPLE_CHANGELOG );
        assertDoesNotThrow( () -> deployer( dbUrl ).deploy( spec ) );
        assertDoesNotThrow( () -> deployer( dbUrl ).deploy( spec ) );
    }

    @Test
    void deploy_should_tag_database_before_migration() throws Exception {
        final String dbUrl = uniqueDbUrl();
        final DeploymentSpec spec = spec( "test-artifact", "1.0.0", SIMPLE_CHANGELOG );
        deployer( dbUrl ).deploy( spec );
        assertTrue( tagExists( dbUrl, spec.getName() + ":1.0.0" ) );
    }

    @Test
    void undeploy_should_rollback_applied_changesets() throws Exception {
        final String dbUrl = uniqueDbUrl();
        final DeploymentSpec spec = spec( SIMPLE_CHANGELOG );
        final LiquibaseDeployer deployer = deployer( dbUrl );

        deployer.deploy( spec );
        assertTrue( tableExists( dbUrl, "PERSON" ) );

        deployer.undeploy( spec );
        assertFalse( tableExists( dbUrl, "PERSON" ) );
    }

    @Test
    void undeploy_should_throw_when_tag_on_deploy_is_disabled() {
        final LiquibaseDeployer deployer = deployer( uniqueDbUrl(), false );
        final DeploymentException ex = assertThrows(
            DeploymentException.class,
            () -> deployer.undeploy( spec( SIMPLE_CHANGELOG ) )
        );
        assertTrue( ex.getMessage().contains( "tag-on-deploy is disabled" ) );
    }

    @Test
    void deploy_should_substitute_env_variables_in_changelog() throws Exception {
        final String dbUrl = uniqueDbUrl();
        final DeploymentSpec spec = spec( PARAMETERIZED_CHANGELOG );
        spec.setEnv( Map.of( "prefix", "NS1" ) );
        deployer( dbUrl ).deploy( spec );
        assertTrue( tableExists( dbUrl, "NS1_AUDIT" ) );
    }

    @Test
    void deploy_should_throw_on_invalid_sql() {
        assertThrows(
            DeploymentException.class,
            () -> deployer( uniqueDbUrl() ).deploy( spec( INVALID_CHANGELOG ) )
        );
    }

    @Test
    void deploy_should_throw_on_connection_failure() {
        final LiquibaseDeployerConfig config = new LiquibaseDeployerConfig();
        config.setUrl( "jdbc:invalid://not-a-real-db" );
        config.setUsername( "sa" );
        config.setPassword( "" );
        final LiquibaseDeployer deployer = new LiquibaseDeployer();
        deployer.init( config );

        assertThrows(
            DeploymentException.class,
            () -> deployer.deploy( spec( SIMPLE_CHANGELOG ) )
        );
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String uniqueDbUrl() {
        return "jdbc:h2:mem:liq_" + UUID.randomUUID().toString().replace( "-", "" ) + ";DB_CLOSE_DELAY=-1";
    }

    private LiquibaseDeployer deployer( String dbUrl ) {
        return deployer( dbUrl, true );
    }

    private LiquibaseDeployer deployer( String dbUrl, boolean tagOnDeploy ) {
        final LiquibaseDeployerConfig config = new LiquibaseDeployerConfig();
        config.setUrl( dbUrl );
        config.setUsername( "sa" );
        config.setPassword( "" );
        config.setTagOnDeploy( tagOnDeploy );
        final LiquibaseDeployer deployer = new LiquibaseDeployer();
        deployer.init( config );
        return deployer;
    }

    private DeploymentSpec spec( String changelogContent ) {
        return spec( "test-artifact", "1.0.0", changelogContent );
    }

    private DeploymentSpec spec( String name, String version, String changelogContent ) {
        final DeploymentSpec spec = new DeploymentSpec();
        spec.setName( name + "/liquibase/changelog.sql" );
        spec.setVersion( Semver.parse( version ) );
        spec.setData( changelogContent.getBytes() );
        spec.setHash( "test-hash" );
        spec.setMediaType( "application/vnd.io.installr.liquibase" );
        spec.setEnv( Map.of() );
        return spec;
    }

    private boolean tableExists( String dbUrl, String tableName ) throws SQLException {
        try (
            Connection conn = DriverManager.getConnection( dbUrl, "sa", "" );
            ResultSet rs = conn.getMetaData().getTables( null, null, tableName.toUpperCase(), null )
        ) {
            return rs.next();
        }
    }

    private boolean tagExists( String dbUrl, String tag ) throws SQLException {
        try (
            Connection conn = DriverManager.getConnection( dbUrl, "sa", "" );
            PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE TAG = ?"
            )
        ) {
            ps.setString( 1, tag );
            try ( ResultSet rs = ps.executeQuery() ) {
                rs.next();
                return rs.getInt( 1 ) > 0;
            }
        }
    }

}
