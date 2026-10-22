# Developer Guide

This guide walks you step by step through setting up the development environment, building the project, running all required services locally, and verifying everything works.

The goal is that you can follow this guide from top to bottom without prior project knowledge.

---

## 1. Prerequisites

Before you start, make sure all required tools are installed and working.

### 1.1 Docker

Docker is required to run the demo application.

To install Docker follow the steps for your distribution: [https://docs.docker.com/engine/install](https://docs.docker.com/engine/install)

After installation, verify Docker is working:

```bash
systemctl status docker.service
```

Now initialize a Docker Swarm (required for deployment features):

```bash
docker swarm init
```

You should see a message that the node is now a swarm manager.

---

### 1.2 Java & Maven

To build the project you need the following tools:

* JDK **21**
* Maven **3.9**

#### Option A: Using mise-en-place (recommended)

To install mise-en-place follow this guide: [https://mise.jdx.dev/getting-started.html](https://mise.jdx.dev/getting-started.html)

#### Option B: Manual installation

Install the required tools from your distribution's repositories or download them from the project pages. After installation, ensure that the tools are available in your PATH.

---

## 2. Clone Repositories

All components are split into multiple repositories. Clone all required repositories into the same parent directory:

```bash
git clone git@github.com:installr-io/installr.git
git clone git@github.com:installr-io/atrium.git
git clone git@github.com:installr-io/ipf.git
git clone git@github.com:installr-io/ic.git
git clone git@github.com:installr-io/demo-app.git
```

---

## 3. Build & Run `installr`

The installr repository contains the core backend service (`installrd`).

```bash
cd installr/
```

### 3.1 Install toolchain (if using mise)

```bash
mise trust
mise install
```

This installs the correct Java and Maven versions automatically.

### 3.2 Build the project

```bash
mvn clean install
```

### 3.3 Setup local database

`installrd` uses a local H2 database to store installation meta data. The database is located at:
`~/.installrd`

Create the initial configuration and empty database:

```bash
cp -r ./installrd/src/main/non-packaged-resources/config/ ~/.installrd/
```

Now edit the `~/.installrd/config.yaml`:

Required changes:

* Uncomment the **deployer** section
* Uncomment the **repositories** section

If these are not enabled, deployments will not work.

### 3.4 Create demo namespace

`installrd` uses namespaces to separate installations from each other:

```bash
mkdir -p ~/.installrd/namespaces/demo
touch ~/.installrd/namespaces/demo/config.yaml
```

This prepares a namespace where applications can be installed later.

For now `~/.installrd/namespaces/demo/config.yaml` can be left empty.

### 3.5 Create dev certificates

`installrd` only accepts HTTPS requests with a client certificate (mTLS), also in dev mode. Create a dev CA, a server
certificate for `localhost` and a client certificate for yourself:

```bash
./scripts/dev-certs
```

The script copies `server.crt`, `server.key` and `ca.crt` to `~/.installrd/tls/`. The CA key and your client
certificate stay in `~/.installr-dev-pki/`. It prints the subject of your client certificate; add it as admin to
`~/.installrd/config.yaml`:

```yaml
security:
    admins:
        - "CN=<your user>,O=installr-dev"
```

See [INSTALL.md](../INSTALL.md), section "Set up Security", for roles and certificates in general.

### 3.6 Start installrd

```bash
mvn -f installrd/pom.xml quarkus:dev
```

This starts `installrd` in dev mode on `https://localhost:9316`. Check the setup with your client certificate:

```bash
curl --cert ~/.installr-dev-pki/${USER}.crt --key ~/.installr-dev-pki/${USER}.key \
    --cacert ~/.installr-dev-pki/ca.crt https://localhost:9316/namespaces
```

If you prefer to run the binary, run
```bash
./installrd/target/installrd
```

Leave this terminal running.

---

## 4. Build & Run `atrium`

Open a **new terminal**.

```bash
cd atrium/
```

If you work with `mise-en-place` run
```bash
mise trust
```

### 4.1 Build the project

```bash
mvn clean install
```

### 4.2 Setup configuration

```bash
mkdir ~/.atrium/ && cp -r src/main/non-packaged-resources/config/* ~/.atrium/
```

This copies default configuration files.

### 4.3 Start `atrium`

```bash
mvn quarkus:dev
```

This starts `atrium` in dev mode.

Leave this terminal running, too.

---

## 5. Build ipf and ic

Open **another terminal**.

Now we build two required tooling components:

* ipf: The packaging tool
* ic: installrd's command line client

### 5.1 Build `ipf`

_If you use `mise-en-place`, you know what to do ;)_

```bash
cd ipf/
mvn clean install
```


### 5.2 Build `ic`

```bash
cd ../ic/
mvn clean install
```

---

## 6. Build and Package `demo-app`

```bash
cd ../demo-app/
```

Build the application and create an IPF package:

```bash
mvn clean package -Dquarkus.container-image.build ipf:package
```

What happens here:

* The application is compiled
* A container image is built
* An IPF package is generated

### 6.1 Push to local registry

```bash
mvn ipf:push
```

This uploads the package to the local `atrium` registry.

---

## 7. Install the Application

Now we install the demo-app with `ic`:

```bash
../ic/target/ic install -n demo demo-app:0.1.0
```

Explanation:

* `-n demo` → installs into the demo namespace
* `demo-app:0.1.0` → package name and version

---

## 8. Test the Setup

Verify the application is running:

```bash
docker service ls
```

This should show the running docker container `demo-demo-app_srv`.


```bash
curl http://127.0.0.1:8088
```

This should greet you with an html saying: `Hello 'installrd' World :)`

---

## Troubleshooting

### Docker issues

* Ensure Docker daemon is running
* Re-run `docker swarm init` if needed

### Port conflicts

* Make sure no other service is using port 8088

### Build failures

* Verify Java and Maven versions
* Run `mvn clean install` again

### Service not reachable

* Check terminal logs
* Ensure all services are running in parallel

---

## Final Result

You now have:

* All services running locally
* A built and deployed demo application
* A working end-to-end development setup

You can now start developing, modifying, and testing changes locally.
