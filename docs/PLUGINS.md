# Plugins

`installrd` can be extended with plugins. A plugin is a JAR file in the plugin directory `~/.installrd/plugins/`.
Plugins are loaded once when `installrd` starts; restart `installrd` after adding, updating or removing a plugin.

There are three kinds of plugins:

| Kind | Interface | Purpose |
|---|---|---|
| Deployer | `io.installr.lib.deployer.DeployerPlugin` | deploys a type of artifact that no built-in deployer supports |
| Package content filter | `io.installr.lib.installrd.PackageContentFilter` | filters the contents of a package before they are installed |
| Database driver | `java.sql.Driver` | provides a JDBC driver, e.g. for the `LiquibaseDeployer` |

Plugins are found with the Java `ServiceLoader`: every plugin JAR lists its implementations in
`META-INF/services/<interface name>`. A JAR may contain several plugins and several kinds of plugins.

## Licensing

`installrd` is licensed under the AGPL. The
[Installr Linking Exception](https://github.com/installr-io/installr/blob/main/LINKING-EXCEPTION) allows you to
distribute `installrd` together with plugins under other licenses, including proprietary ones, provided that the
plugin:

* is loaded through the plugin API, i.e. as described on this page,
* uses only the plugin API (the libraries in `lib/`, licensed under the LGPL) and its own dependencies, but no
  classes of `installrd` itself,
* communicates with `installrd` only through the plugin API, e.g. through return values and exceptions, and
* does not change the state of `installrd` in any other way.

Database drivers may be distributed together with `installrd` regardless of their license.

Changes to `installrd` itself always remain under the AGPL. The text of the exception is authoritative; this summary is
for orientation only.

## Deployer Plugins

A deployer plugin installs and uninstalls a specific type of artifact. Add the plugin API as a dependency; it is
published to the Installr Maven repository:

```xml
<repositories>
    <repository>
        <id>installr-io-maven-repo</id>
        <url>https://maven.pkg.github.com/installr-io/installr</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.installr</groupId>
        <artifactId>installr-lib-deployer</artifactId>
        <version>${installr.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Use the scope `provided`: `installrd` already contains the plugin API. GitHub Packages requires authentication, also
for public packages; see
[Working with the Apache Maven registry](https://docs.github.com/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry).

Implement `DeployerPlugin` and a configuration class:

```java
package com.example;

public class ExampleDeployer extends AbstractConfigurableDeployer<ExampleDeployerConfig>
    implements DeployerPlugin<ExampleDeployerConfig> {

    @Override
    public Class<ExampleDeployerConfig> getConfigType() {
        return ExampleDeployerConfig.class;
    }

    @Override
    public void deploy( DeploymentSpec spec ) throws DeploymentException {
        // install spec.getData() using config.getUrl()
    }

    @Override
    public void undeploy( DeploymentSpec spec ) throws DeploymentException {
        // uninstall spec.getData()
    }

}
```

```java
package com.example;

public class ExampleDeployerConfig extends AbstractDeployerConfig {

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl( String url ) {
        this.url = url;
    }

}
```

Register the deployer in `META-INF/services/io.installr.lib.deployer.DeployerPlugin`:

```text
com.example.ExampleDeployer
```

Configure it in `~/.installrd/config.yaml` or in the `config.yaml` of a namespace, using the fully qualified class name
as `type`. The entries below `config` are mapped to the configuration class:

```yaml
deployer:
  "application/vnd.example.resource":
    type: com.example.ExampleDeployer
    config:
      url: https://example.com/api
```

`init` is called with the configuration before the deployer is used; `AbstractConfigurableDeployer` stores it in the
field `config`.

### The Deployment Spec

`deploy` and `undeploy` receive a `DeploymentSpec` for each item of a package:

| Field | Description |
|---|---|
| `name` | name of the item: `<package name>/<path in the package>` |
| `version` | version of the package |
| `data` | content of the item |
| `hash` | hash of the content |
| `mediaType` | media type of the item |
| `encoding` | encoding of the content, e.g. `UTF-8` |
| `env` | package values as flat keys, e.g. `db.host` and `db_host` |

### Results

* Return normally if the item was deployed.
* Throw a `DeploymentException` if the deployment failed. The item and the package are marked as failed.
* Throw a `DeploymentWarning` if the item was skipped, e.g. because it already exists. The item is not marked as
  failed.

Other exceptions are treated like a `DeploymentException`. Override `translateException` to turn them into meaningful
messages.

## Package Content Filters

A package content filter receives the contents of a package that are about to be installed and can narrow them down,
e.g. to skip items depending on the environment. Add `installr-lib-installrd` as a `provided` dependency, implement
`PackageContentFilter` and register it in `META-INF/services/io.installr.lib.installrd.PackageContentFilter`:

```java
package com.example;

public class ExampleFilter implements PackageContentFilter {

    @Override
    public void filter( List<PackageContent> contents ) {
        contents.removeIf( content -> content.getPath().startsWith( "optional/" ) );
    }

}
```

All filters found are applied to every package in every namespace.

## Database Drivers

JDBC drivers are plugins, too: copy the driver JAR into `~/.installrd/plugins/` and restart `installrd`. Every driver
the JAR lists in `META-INF/services/java.sql.Driver` is registered; this is the case for all current JDBC drivers. Use
the driver with its JDBC URL, e.g. in the configuration of the `LiquibaseDeployer`:

```yaml
deployer:
  "application/vnd.io.installr.liquibase":
    type: LiquibaseDeployer
    config:
      url: "jdbc:oracle:thin:@//db.example.com:1521/APP"
```

The PostgreSQL driver is included in `installrd`.
