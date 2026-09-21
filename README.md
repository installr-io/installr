![Installr Logo](docs/images/installr-logo.png)

A **multi-purpose package installer**

---

`Installr` is an open-source **toolbox for software delivery and installation** designed to bundle, distribute, and deploy complete software stacks in a structured and reproducible way.

It consists of several components:

* `ipf` – bundles your software into installable packages
* `atrium` – distributes and provides packages
* `installrd` – the installation daemon — the heart of the system
* `inctl` – a command-line interface used to control `installrd`

With `Installr`, you can package everything required to run your software, including:

* Deployment descriptors (for different platforms and orchestrators)
* Configuration files
* Keycloak resources (realms, clients, scopes, etc.)
* Static files
* Any other artifact type supported by a **Deployer** - As long as a corresponding *Deployer* exists, the artifact can be managed and deployed by `Installr`.

---

## What `Installr` is **not**

`Installr` is **not** intended to replace existing OS package management systems.

Specifically, it is **not**:

* a replacement for distribution-specific package managers such as `apt`, `dnf`, or `pacman`
* a local desktop software installer
* a general-purpose operating system package manager

Instead, `Installr` focuses on the **structured delivery and deployment of complete software stacks**.

---

## Core Concepts

### IPF

An `ipf` package (**Installr Package Format**) bundles all artifacts and metadata required to deploy a software component. These packages are then published to `atrium`.

### atrium

`atrium` is the package distribution service.
It stores packages in multiple repositories and makes them available to `installrd`.

### installrd

`installrd` is the **installation daemon** responsible for performing deployments on the target infrastructure.

#### Deployer

A *Deployer* is responsible for installing and managing a specific type of artifact.

This modular architecture allows `Installr` to support different technologies without being tightly coupled to them.

### inctl

`inctl` is the **command-line interface** used to interact with and control `installrd`.

---

## Typical Use Cases

`Installr` is particularly useful for scenarios such as:

* Delivering **on-premise software distributions**
* Managing **customer-specific deployments**
* Installing **complete application stacks in cluster environments**

---

## Audience

`Installr` is intended for anyone responsible for delivering or operating complex software systems, particularly:

* **System administrators**
* **DevOps engineers**
* **Platform engineers**

---

## Vision

The long-term goal of `Installr` is to provide a **modular and extensible platform for software delivery**.

---

## Status

`Installr` is currently **in an early stage of development** and under active development.

At the moment, deployers exist for the following artifact types:

* Local files
* Docker Swarm
* Kubernetes
* Liquibase scripts
* Generic HTTP interface for integrating external [Deployer](lib/deployer/src/main/java/io/installr/lib/deployer/Deployer.java) implementations

  * Keycloak resources

    * [kcd](https://github.com/installr-io/installr-kcd) (Keycloak Deployer) is an implementation of the Deployer interface based on [keycloak-config-cli](https://github.com/adorsys/keycloak-config-cli).

### Planned Deployers

The following deployers are currently planned:

* Distribute files via **SSH**
* Distribute files via **HTTP**

---

## License

`Installr` is licensed under the **GNU Affero General Public License (AGPL)**, with the exception of all libraries located under `lib/`, which are licensed under the **GNU Lesser General Public License (LGPL)**.

---

## Installation
👉 See the [Installation & Setup Guide](INSTALL.md) for setup instructions.
