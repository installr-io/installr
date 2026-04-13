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

### 4. Update the `PATH`

Add `/opt/installr-io/bin` to `installr`'s PATH:

```bash
cat << EOF >> ~/.bashrc

## installr-io
export PATH=\$PATH:/opt/installr-io/bin
EOF
```

---

### 5. Logout

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