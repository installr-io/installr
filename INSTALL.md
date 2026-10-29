# Installation & Setup Guide

## Overview

This guide describes the installation, configuration, and basic usage of `installrd`.

---

## Prerequisites

Before starting the installation, ensure the following requirements are met:

* A Linux system using `dnf` (e.g., Fedora, RHEL, CentOS Stream)
* `sudo` or root privileges
* A running Docker Swarm cluster
  (see the [Developer Guide](https://github.com/installr-io/installr/blob/main/docs/DEVELOPER_GUIDE.md#11-docker) for setup instructions)

---

## Installation

### 1. Import the GPG Key

Import the GPG key used to verify the RPM packages:

```bash
sudo rpm --import https://raw.githubusercontent.com/installr-io/installr/refs/heads/main/gpg/installr-io.gpg
```

---

### 2. Install `installrd` and `ic`

Install the required RPM packages:

```bash
sudo dnf install \
  https://github.com/installr-io/installr/releases/download/0.1.0/installrd-0.1.0-1.noarch.rpm \
  https://github.com/installr-io/ic/releases/download/0.1.0/ic-0.1.0-1.noarch.rpm
```

---

### 3. Add `installr` User to Docker Group

`installrd` runs under the dedicated user `installr`.
To allow access to the Docker socket, add this user to the `docker` group:

```bash
sudo usermod -aG docker installr
```

---

## Configuration

### 1. Login as user `installr`

```bash
sudo su - installr
```

---

### 2. Create the Configuration File

Create a basic configuration at `~/.installrd/config.yaml`:

```bash
cat << EOF > ~/.installrd/config.yaml
deployer:
  ## Deploy Docker Swarm descriptors
  "application/vnd.io.installr.docker-stack":
    type: DockerStackDeployer

repositories:
  - id: demo-repository
    name: "Demo Applications"
    url: https://conexus.tugraz.at/atrium/repositories/installr-io-demo
EOF
```

---

### 3. Create a Demo Namespace

```bash
mkdir -p ~/.installrd/namespaces/demo
touch ~/.installrd/namespaces/demo/config.yaml
```

---

### 4. Set up Security (mTLS & Roles)

`installrd` only accepts HTTPS requests on port `9316`. Clients authenticate with a **client certificate** (mTLS); the
subject DN of the certificate is the identity used for authorization. Security is always enabled. Only `/version` and
`/version-info` are reachable without a certificate.

#### Certificates

`installrd` expects the following PEM files in `~/.installrd/tls/`:

| File | Content |
|---|---|
| `server.crt` | Server certificate (with the host name in the SAN), optionally followed by intermediate certificates |
| `server.key` | Private key of the server certificate |
| `ca.crt` | CA certificate(s) used to verify client certificates |

`installrd` does not start if one of these files is missing. Changed files are picked up within one hour, without
a restart.

The package creates them on installation (and on upgrade, if `~/.installrd/tls/` does not exist yet) with the script
`installrd-certs init`, together with a CA and an admin certificate. The server certificate is valid for `localhost`,
`127.0.0.1`, `::1` and the host name; add all further names and IPs, under which clients reach installrd:

```bash
/opt/installr-io/bin/installrd-certs renew-server --san installrd.example.org
```

| File | Content |
|---|---|
| `~/.installrd/pki/ca.crt`, `ca.key` | CA (valid for 10 years) |
| `~/.installrd/tls/server.crt`, `server.key`, `ca.crt` | Server certificate (valid for 1 year) and CA certificate for installrd |
| `~/.installrd/admin.yaml` | `ic` config with the admin certificate `CN=admin,O=installr.io` (valid for 1 year), see "Configure `ic`" |

Grant the admin certificate `CN=admin,O=installr.io` the `admin` role in
`~/.installrd/config.yaml` (see "Roles"):

```yaml
security:
  admins:
    - "CN=admin,O=installr.io"
```

Further client certificates (valid for 1 year, `--days` to change) are issued as ready-to-use `ic` config; the private
key is only contained in this output. The config is named after the installrd host (short host name, `--name` to
change), so a user can merge the configs of several hosts. Grant the printed subject access via roles:

```bash
installrd-certs issue alice --name prod > alice.yaml         # CN=alice,O=installr.io, config 'prod'
installrd-certs issue ci-pipeline --org acme > ci.yaml       # CN=ci-pipeline,O=acme
```

Renew the server certificate before it expires (or to add names); installrd picks it up without restart:

```bash
installrd-certs renew-server [--san other.example.org]
```

Whoever has `~/.installrd/pki/ca.key` can issue certificates for any subject, i.e. also for an admin. On the installrd
host, this is the user `installr`, who can change `config.yaml` anyway. Issued client certificates cannot be revoked
yet; remove their subject from `config.yaml` instead.

If your organization runs a CA, use it instead of `installrd-certs`: request the server certificate (with all host
names as SAN) and the client certificates there and replace `server.crt`, `server.key` and `ca.crt` (the CA
certificate) in `~/.installrd/tls/`. To recreate the certificates with `installrd-certs init` (options `--san`, `--url`,
`--name`), remove `~/.installrd/pki/`, `~/.installrd/tls/` and `~/.installrd/admin.yaml` first.

#### Roles

Access is configured in `~/.installrd/config.yaml` and in the `config.yaml` of each namespace. Changes are applied
without a restart.

```yaml
security:
  ## built-in role 'admin': all operations, globally
  admins:
    - "CN=admin,O=installr.io"

  ## custom roles: a list of operations; wildcards like 'pkg:*' or '*' are allowed
  roles:
    - name: developer
      operations: [ "pkg:install", "pkg:reinstall", "pkg:read", "tx:*", "ns:read" ]
    - name: viewer
      operations: [ "ns:read", "pkg:read", "tx:read" ]
    - name: ns-manager
      operations: [ "ns:create", "ns:delete" ]

  ## global bindings: apply to all namespaces and are the only way to grant global operations
  bindings:
    - subject: "CN=ops,O=installr.io"
      roles: [ ns-manager, viewer ]
```

```yaml
## ~/.installrd/namespaces/demo/config.yaml
access:
  - subject: "CN=ci-pipeline,O=acme"
    roles: [ developer ]
```

Subject DNs are written in RFC 2253 order (most specific first, e.g. `CN=alice,O=installr.io`), as printed by
`openssl x509 -noout -subject -nameopt RFC2253`. Case and whitespace don't matter.

| Operation | Scope | Endpoint |
|---|---|---|
| `ns:read` | namespace | `GET /namespaces/{ns}`, visibility in `GET /namespaces` |
| `pkg:install` | namespace | `POST /namespaces/{ns}/install` |
| `pkg:reinstall` | namespace | `POST /namespaces/{ns}/reinstall` |
| `pkg:install-file` | namespace | `POST /namespaces/{ns}/install-file` (package files on the daemon host) |
| `pkg:reinstall-file` | namespace | `POST /namespaces/{ns}/reinstall-file` (package files on the daemon host) |
| `pkg:read` | namespace | `GET /namespaces/{ns}/package-log` |
| `tx:read` | namespace | `GET /namespaces/{ns}/transactions[/...]` |
| `tx:cancel` | namespace | `POST /namespaces/{ns}/cancel` |
| `ns:create` | global | `POST /namespaces` |
| `ns:delete` | global | `DELETE /namespaces/{ns}` |
| `daemon:stop` | global | `POST /stop` |

Rules:

* The role name `admin` is reserved. Unknown operations or roles make the configuration invalid.
* Global operations of a role bound in a namespace are ignored (with a warning in the log).
* The client, which started a transaction, is recorded in the transaction log (`ACTOR`).

#### Database

The H2 database accepts TCP connections on port `9416` from other hosts, e.g. for external tools. It is protected by
the user `INSTALLRD` and a password, which is generated on first start and stored in `~/.installrd/.db-credentials`
(readable by the user `installr` and its group only):

```bash
cat ~/.installrd/.db-credentials
## JDBC URL: jdbc:h2:tcp://<host>:9416/installrd, user: INSTALLRD
```

In an existing database created by an older version (without password), the user `INSTALLRD` is created on first
start and the previous user is locked. External processes that access the database need these credentials from now on.

---

### 5. Configure `ic`

`ic` reads its connections to `installrd` from `~/.ic/config.yaml`. Each named config contains the URL and the
client certificate, its key and the CA certificate as embedded PEM. For the demo, `ic` runs as user `installr` on the
installrd host and uses the admin config created during installation:

```bash
mkdir -p ~/.ic && install -m 600 ~/.installrd/admin.yaml ~/.ic/config.yaml
```

The file contains private keys, so `ic` refuses to use it if it is accessible by group or others (`chmod 600`).
Further configs, e.g. for remote installrd hosts or created with `installrd-certs issue`, are added below `configs` and
selected with `ic -c <name>`. If the server certificate is issued by a CA from the Java trust store, `ca` can be
omitted.

---

### 6. Update the `PATH`

Add `/opt/installr-io/bin` to `installr`'s PATH:

```bash
cat << EOF >> ~/.bashrc

## installr-io
export PATH=\$PATH:/opt/installr-io/bin
EOF
```

---

### 7. Logout

For now, logout user `installr`.

---

## Start the `installrd` Service

### 1. Start the Service

```bash
sudo systemctl start installrd.service
```

---

### 2. Enable at Boot

```bash
sudo systemctl enable installrd.service
```

---

### 3. Verify Service Status

```bash
sudo systemctl status installrd.service
```

The output should be something like this:

```
● installrd.service - Installrd - A multi-purpose package installr
     Loaded: loaded (/usr/lib/systemd/system/installrd.service; enabled; preset: disabled)
    Drop-In: /usr/lib/systemd/system/service.d
             └─10-timeout-abort.conf
     Active: active (running) since Mon 2026-04-13 14:47:02 CEST; 1min 16s ago
 Invocation: af8a24b2daf24922be4bc6fa0213e9bd
   Main PID: 68986 (java)
      Tasks: 31 (limit: 4634)
     Memory: 241.9M (peak: 282.6M)
        CPU: 4.495s
     CGroup: /system.slice/installrd.service
             └─68986 /usr/bin/java -jar /opt/installr-io/bin/installrd

Apr 13 14:47:04 mt installrd[68986]: INFO  - Timestamp: 2026-04-10T10:49:28.866Z[UTC]
Apr 13 14:47:04 mt installrd[68986]: INFO  - Timezone: UTC
Apr 13 14:47:04 mt installrd[68986]: INFO  Config:
Apr 13 14:47:04 mt installrd[68986]: INFO  - Location: /home/installr/.installrd
Apr 13 14:47:04 mt installrd[68986]: INFO  Namespaces:
Apr 13 14:47:04 mt installrd[68986]: INFO  - demo
Apr 13 14:47:04 mt installrd[68986]: INFO  Repositories:
Apr 13 14:47:04 mt installrd[68986]: INFO  - demo-repository: https://conexus.tugraz.at/atrium/repositories/installr-io-demo
Apr 13 14:47:04 mt installrd[68986]: INFO  Deployer:
Apr 13 14:47:04 mt installrd[68986]: INFO  - application/vnd.io.installr.docker-stack: DockerStackDeployer
```

---

## Install the Demo Application
If `installrd` is running, we can now install a demo application to test, if the setup works as expected.

### 1. Switch to `installr` User

```bash
sudo su - installr
```

---

### 2. Install Demo Application

```bash
ic -n demo install demo-app:0.1.0
```

---

### 3. Verify Running Services

```bash
docker service ls
```

Expected output:

```
ID             NAME                MODE         REPLICAS   IMAGE                                         PORTS
auh493y3wegy   demo-demo-app_srv   replicated   1/1        ghcr.io/installr-io/demo-app:0.1.0-a6f6877    *:8088->8080/tcp
```

---

### 4. Test the Demo Application

Open in browser or use curl:

```bash
curl http://127.0.0.1:8088
```

Expected response:

```
Hello 'installrd' World :)
```

---

## Notes & Troubleshooting

* Ensure the configuration file is correctly set up before starting the service.
* Verify network access to configured repositories if installation fails.
* Use system logs for debugging:

```bash
journalctl -u installrd.service
```