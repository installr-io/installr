`Installr` is an open-source **toolbox for software delivery and installation**. It bundles everything required to
run your software, such as deployment descriptors, configuration files, database scripts or Keycloak resources, into
a single package and installs it on the target infrastructure, e.g. Kubernetes or Docker Swarm.

## Components

| Component | Description |
|---|---|
| [`ipf`](https://github.com/installr-io/ipf) | bundles your software into installable packages |
| [`atrium`](https://github.com/installr-io/atrium) | stores packages in repositories and distributes them |
| [`installrd`](https://github.com/installr-io/installr) | the installation daemon: installs packages using *Deployers* |
| [`ic`](https://github.com/installr-io/ic) | the command-line interface to control `installrd` |

## Documentation

* [Installation & Setup Guide](INSTALL.md)
* [Developer Guide](DEVELOPER_GUIDE.md)
* [Plugins](PLUGINS.md)
* [Contributing](CONTRIBUTING.md)
* [Code of Conduct](CODE_OF_CONDUCT.md)
* [Security Policy](SECURITY.md)

---

[Impressum](IMPRESSUM.md)
