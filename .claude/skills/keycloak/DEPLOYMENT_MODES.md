# Keycloak Deployment Modes

Complete guide to deploying Keycloak in standalone, standalone clustered, and domain clustered modes.

## Table of Contents

- [Standalone Mode](#standalone-mode)
  - [Installation](#standalone-installation)
  - [Configuration](#standalone-configuration)
  - [Starting the Server](#starting-standalone-server)
- [Standalone Clustered Mode](#standalone-clustered-mode)
  - [Prerequisites](#clustered-prerequisites)
  - [Infinispan Configuration](#infinispan-configuration)
  - [JGroups Configuration](#jgroups-configuration)
  - [Load Balancer Setup](#load-balancer-setup)
  - [Starting Cluster Nodes](#starting-cluster-nodes)
- [Domain Clustered Mode](#domain-clustered-mode)
  - [Architecture](#domain-architecture)
  - [Domain Controller Setup](#domain-controller-setup)
  - [Host Controller Configuration](#host-controller-configuration)
  - [Server Groups](#server-groups)
- [Docker Deployments](#docker-deployments)
- [Kubernetes Deployments](#kubernetes-deployments)
- [Database Configuration](#database-configuration)

---

## Standalone Mode

Simplest deployment mode - single Keycloak instance running independently.

### Standalone Installation

**1. Download Keycloak:**

```bash
wget https://github.com/keycloak/keycloak/releases/download/26.4.0/keycloak-26.4.0.tar.gz
tar -xzf keycloak-26.4.0.tar.gz
cd keycloak-26.4.0
```

**2. Create admin user:**

```bash
export KEYCLOAK_ADMIN=admin
export KEYCLOAK_ADMIN_PASSWORD=admin
```

**3. Start in development mode:**

```bash
./bin/kc.sh start-dev
```

Access admin console at: `http://localhost:8080/admin`

### Standalone Configuration

**Production configuration file:** `conf/keycloak.conf`

```properties
# HTTP/HTTPS configuration
http-enabled=true
http-port=8080
https-port=8443
https-certificate-file=/path/to/certificate.pem
https-certificate-key-file=/path/to/key.pem

# Hostname
hostname=keycloak.example.com
hostname-strict=false
hostname-strict-https=false

# Database configuration
db=postgres
db-url=jdbc:postgresql://localhost:5432/keycloak
db-username=keycloak
db-password=password

# Proxy configuration (if behind reverse proxy)
proxy=edge
http-relative-path=/auth
```

**Build optimized server:**

```bash
./bin/kc.sh build
```

### Starting Standalone Server

**Development mode:**
```bash
./bin/kc.sh start-dev
```

**Production mode:**
```bash
./bin/kc.sh start \
  --optimized \
  --hostname=keycloak.example.com \
  --https-certificate-file=/path/to/cert.pem \
  --https-certificate-key-file=/path/to/key.pem
```

**With environment variables:**
```bash
export KC_DB=postgres
export KC_DB_URL=jdbc:postgresql://db:5432/keycloak
export KC_DB_USERNAME=keycloak
export KC_DB_PASSWORD=password
export KC_HOSTNAME=keycloak.example.com

./bin/kc.sh start --optimized
```

---

## Standalone Clustered Mode

Multiple Keycloak instances with distributed caching for high availability.

### Clustered Prerequisites

**Requirements:**
- 2+ Keycloak nodes
- Shared database (PostgreSQL, MySQL, etc.)
- Load balancer (HAProxy, Nginx, AWS ALB, etc.)
- Network connectivity between nodes (for Infinispan/JGroups)

**Architecture:**
```
         Load Balancer (HAProxy/Nginx)
                    |
      +-------------+-------------+
      |                           |
  Keycloak Node 1          Keycloak Node 2
      |                           |
      +-------------+-------------+
                    |
          Shared Database (PostgreSQL)
```

### Infinispan Configuration

Keycloak uses Infinispan for distributed caching. Configuration in `conf/cache-ispn.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<infinispan
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="urn:infinispan:config:14.0 http://www.infinispan.org/schemas/infinispan-config-14.0.xsd"
    xmlns="urn:infinispan:config:14.0">

    <cache-container name="keycloak">
        <transport lock-timeout="60000"/>

        <!-- Sessions cache -->
        <distributed-cache name="sessions" owners="2">
            <expiration lifespan="-1"/>
        </distributed-cache>

        <!-- Authentication sessions -->
        <distributed-cache name="authenticationSessions" owners="2">
            <expiration lifespan="1800000"/>
        </distributed-cache>

        <!-- Offline sessions -->
        <distributed-cache name="offlineSessions" owners="2">
            <expiration lifespan="-1"/>
        </distributed-cache>

        <!-- Client sessions -->
        <distributed-cache name="clientSessions" owners="2">
            <expiration lifespan="-1"/>
        </distributed-cache>

        <!-- Offline client sessions -->
        <distributed-cache name="offlineClientSessions" owners="2">
            <expiration lifespan="-1"/>
        </distributed-cache>

        <!-- Login failures -->
        <distributed-cache name="loginFailures" owners="2">
            <expiration lifespan="-1"/>
        </distributed-cache>

        <!-- Action tokens -->
        <replicated-cache name="actionTokens">
            <expiration max-idle="300000" lifespan="-1"/>
        </replicated-cache>

        <!-- Work cache -->
        <distributed-cache name="work" owners="2">
            <expiration lifespan="-1"/>
        </distributed-cache>

        <!-- Realms and clients (local cache) -->
        <local-cache name="realms">
            <memory max-count="10000"/>
        </local-cache>

        <local-cache name="users">
            <memory max-count="10000"/>
        </local-cache>

        <local-cache name="authorization">
            <memory max-count="10000"/>
        </local-cache>

        <local-cache name="keys">
            <memory max-count="1000"/>
            <expiration max-idle="3600000"/>
        </local-cache>
    </cache-container>
</infinispan>
```

**Key parameters:**
- `owners="2"`: Number of copies of each cache entry (for redundancy)
- `lifespan`: Time in milliseconds before entry expires (-1 = never)
- `max-idle`: Time in milliseconds of inactivity before eviction

### JGroups Configuration

JGroups handles cluster communication. Configuration options:

**1. TCP with TCPPING (static discovery):**

```xml
<stack name="tcp">
    <transport type="TCP" socket-binding="jgroups-tcp"/>
    <protocol type="TCPPING">
        <property name="initial_hosts">192.168.1.10[7600],192.168.1.11[7600],192.168.1.12[7600]</property>
        <property name="port_range">0</property>
        <property name="timeout">3000</property>
    </protocol>
    <protocol type="MERGE3"/>
    <protocol type="FD_SOCK"/>
    <protocol type="FD_ALL"/>
    <protocol type="VERIFY_SUSPECT"/>
    <protocol type="pbcast.NAKACK2"/>
    <protocol type="UNICAST3"/>
    <protocol type="pbcast.STABLE"/>
    <protocol type="pbcast.GMS"/>
    <protocol type="MFC"/>
    <protocol type="FRAG3"/>
</stack>
```

**2. UDP with PING (multicast discovery):**

```xml
<stack name="udp">
    <transport type="UDP" socket-binding="jgroups-udp"/>
    <protocol type="PING"/>
    <protocol type="MERGE3"/>
    <protocol type="FD_SOCK"/>
    <protocol type="FD_ALL"/>
    <protocol type="VERIFY_SUSPECT"/>
    <protocol type="pbcast.NAKACK2"/>
    <protocol type="UNICAST3"/>
    <protocol type="pbcast.STABLE"/>
    <protocol type="pbcast.GMS"/>
    <protocol type="UFC"/>
    <protocol type="MFC"/>
    <protocol type="FRAG3"/>
</stack>
```

**3. Kubernetes (DNS_PING):**

```xml
<stack name="kubernetes">
    <transport type="TCP" socket-binding="jgroups-tcp"/>
    <protocol type="dns.DNS_PING">
        <property name="dns_query">keycloak-headless.default.svc.cluster.local</property>
    </protocol>
    <protocol type="MERGE3"/>
    <protocol type="FD_SOCK"/>
    <protocol type="FD_ALL"/>
    <protocol type="VERIFY_SUSPECT"/>
    <protocol type="pbcast.NAKACK2"/>
    <protocol type="UNICAST3"/>
    <protocol type="pbcast.STABLE"/>
    <protocol type="pbcast.GMS"/>
    <protocol type="MFC"/>
    <protocol type="FRAG3"/>
</stack>
```

**Configure JGroups in keycloak.conf:**

```properties
cache-config-file=cache-ispn.xml
cache-stack=tcp
```

### Load Balancer Setup

**HAProxy configuration:**

```
frontend keycloak_frontend
    bind *:443 ssl crt /etc/haproxy/certs/keycloak.pem
    mode http
    option httplog
    default_backend keycloak_backend

backend keycloak_backend
    mode http
    balance roundrobin
    option httpchk GET /health/ready
    cookie KEYCLOAK_SESSION insert indirect nocache httponly secure

    server node1 192.168.1.10:8080 check cookie node1
    server node2 192.168.1.11:8080 check cookie node2
    server node3 192.168.1.12:8080 check cookie node3
```

**Nginx configuration:**

```nginx
upstream keycloak {
    ip_hash;  # Sticky sessions based on client IP
    server 192.168.1.10:8080 max_fails=3 fail_timeout=30s;
    server 192.168.1.11:8080 max_fails=3 fail_timeout=30s;
    server 192.168.1.12:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 443 ssl;
    server_name keycloak.example.com;

    ssl_certificate /etc/nginx/certs/keycloak.crt;
    ssl_certificate_key /etc/nginx/certs/keycloak.key;

    location / {
        proxy_pass http://keycloak;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port 443;
    }

    location /health/ready {
        proxy_pass http://keycloak;
        access_log off;
    }
}
```

### Starting Cluster Nodes

**Node 1:**
```bash
export KC_DB=postgres
export KC_DB_URL=jdbc:postgresql://db:5432/keycloak
export KC_DB_USERNAME=keycloak
export KC_DB_PASSWORD=password
export KC_HOSTNAME=keycloak.example.com
export KC_PROXY=edge
export KC_CACHE=ispn
export KC_CACHE_CONFIG_FILE=cache-ispn.xml
export KC_CACHE_STACK=tcp
export JGROUPS_DISCOVERY_EXTERNAL_IP=192.168.1.10

./bin/kc.sh start --optimized
```

**Node 2:**
```bash
export KC_DB=postgres
export KC_DB_URL=jdbc:postgresql://db:5432/keycloak
export KC_DB_USERNAME=keycloak
export KC_DB_PASSWORD=password
export KC_HOSTNAME=keycloak.example.com
export KC_PROXY=edge
export KC_CACHE=ispn
export KC_CACHE_CONFIG_FILE=cache-ispn.xml
export KC_CACHE_STACK=tcp
export JGROUPS_DISCOVERY_EXTERNAL_IP=192.168.1.11

./bin/kc.sh start --optimized
```

**Verify cluster formation:**

Check logs for:
```
INFO  [org.infinispan.CLUSTER] (MSC service thread 1-2) ISPN000094: Received new cluster view for channel keycloak: [node1|1] (2) [node1, node2]
```

---

## Domain Clustered Mode

**Note:** Domain mode is deprecated in Keycloak 26+. Use standalone clustered mode for new deployments. This section covers legacy deployments.

### Domain Architecture

Domain mode provides centralized management for multiple Keycloak instances:

```
Domain Controller (DC)
    |
    +-- Host Controller 1
    |       |
    |       +-- Server Instance 1A (auth-server-group)
    |       +-- Server Instance 1B (auth-server-group)
    |
    +-- Host Controller 2
            |
            +-- Server Instance 2A (auth-server-group)
            +-- Server Instance 2B (loadbalancer-group)
```

**Components:**
- **Domain Controller**: Central configuration management
- **Host Controller**: Manages server instances on a host
- **Server Groups**: Logical grouping of servers with shared configuration

### Domain Controller Setup

**Start domain controller:**

```bash
./bin/domain.sh --host-config=host-master.xml
```

**host-master.xml configuration:**

```xml
<host xmlns="urn:jboss:domain:20.0" name="master">
    <management>
        <security-realms>
            <security-realm name="ManagementRealm">
                <authentication>
                    <local default-user="$local"/>
                    <properties path="mgmt-users.properties" relative-to="jboss.domain.config.dir"/>
                </authentication>
            </security-realm>
        </security-realms>
        <management-interfaces>
            <http-interface security-realm="ManagementRealm">
                <http-upgrade enabled="true"/>
                <socket-binding http="management-http"/>
            </http-interface>
        </management-interfaces>
    </management>

    <domain-controller>
        <local/>
    </domain-controller>

    <servers>
        <server name="server-one" group="auth-server-group" auto-start="true">
            <socket-bindings port-offset="0"/>
        </server>
        <server name="server-two" group="auth-server-group" auto-start="true">
            <socket-bindings port-offset="150"/>
        </server>
    </servers>
</host>
```

### Host Controller Configuration

**host-slave.xml configuration:**

```xml
<host xmlns="urn:jboss:domain:20.0" name="slave1">
    <management>
        <security-realms>
            <security-realm name="ManagementRealm">
                <server-identities>
                    <secret value="c2xhdmVwYXNzd29yZA=="/>
                </server-identities>
                <authentication>
                    <local default-user="$local"/>
                    <properties path="mgmt-users.properties" relative-to="jboss.domain.config.dir"/>
                </authentication>
            </security-realm>
        </security-realms>
        <management-interfaces>
            <http-interface security-realm="ManagementRealm">
                <http-upgrade enabled="true"/>
                <socket-binding http="management-http"/>
            </http-interface>
        </management-interfaces>
    </management>

    <domain-controller>
        <remote host="192.168.1.10" port="9990" security-realm="ManagementRealm"/>
    </domain-controller>

    <servers>
        <server name="server-three" group="auth-server-group" auto-start="true">
            <socket-bindings port-offset="300"/>
        </server>
    </servers>
</host>
```

**Start slave host controller:**

```bash
./bin/domain.sh --host-config=host-slave.xml
```

### Server Groups

**domain.xml - server group configuration:**

```xml
<server-groups>
    <server-group name="auth-server-group" profile="auth-server-clustered">
        <jvm name="default">
            <heap size="512m" max-size="2048m"/>
        </jvm>
        <socket-binding-group ref="ha-sockets"/>
    </server-group>

    <server-group name="loadbalancer-group" profile="load-balancer">
        <jvm name="default">
            <heap size="256m" max-size="512m"/>
        </jvm>
        <socket-binding-group ref="load-balancer-sockets"/>
    </server-group>
</server-groups>
```

---

## Docker Deployments

### Single Node (Development)

**docker-compose.yml:**

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - keycloak-network

  keycloak:
    image: quay.io/keycloak/keycloak:26.4.0
    command: start-dev
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: password
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8080:8080"
    depends_on:
      - postgres
    networks:
      - keycloak-network

volumes:
  postgres_data:

networks:
  keycloak-network:
    driver: bridge
```

### Clustered (Production)

**docker-compose-cluster.yml:**

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - keycloak-network

  keycloak-1:
    image: quay.io/keycloak/keycloak:26.4.0
    command: start --optimized
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: password
      KC_HOSTNAME: keycloak.example.com
      KC_PROXY: edge
      KC_CACHE: ispn
      KC_CACHE_STACK: tcp
      JGROUPS_DISCOVERY_EXTERNAL_IP: keycloak-1
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8080:8080"
    depends_on:
      - postgres
    networks:
      - keycloak-network

  keycloak-2:
    image: quay.io/keycloak/keycloak:26.4.0
    command: start --optimized
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: password
      KC_HOSTNAME: keycloak.example.com
      KC_PROXY: edge
      KC_CACHE: ispn
      KC_CACHE_STACK: tcp
      JGROUPS_DISCOVERY_EXTERNAL_IP: keycloak-2
    ports:
      - "8081:8080"
    depends_on:
      - postgres
    networks:
      - keycloak-network

  nginx:
    image: nginx:alpine
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    ports:
      - "443:443"
    depends_on:
      - keycloak-1
      - keycloak-2
    networks:
      - keycloak-network

volumes:
  postgres_data:

networks:
  keycloak-network:
    driver: bridge
```

---

## Kubernetes Deployments

**keycloak-statefulset.yaml:**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: keycloak-headless
  labels:
    app: keycloak
spec:
  clusterIP: None
  ports:
    - name: http
      port: 8080
    - name: jgroups
      port: 7600
  selector:
    app: keycloak
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: keycloak
spec:
  serviceName: keycloak-headless
  replicas: 3
  selector:
    matchLabels:
      app: keycloak
  template:
    metadata:
      labels:
        app: keycloak
    spec:
      containers:
        - name: keycloak
          image: quay.io/keycloak/keycloak:26.4.0
          args: ["start", "--optimized"]
          env:
            - name: KC_DB
              value: postgres
            - name: KC_DB_URL
              value: jdbc:postgresql://postgres:5432/keycloak
            - name: KC_DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: keycloak-db
                  key: username
            - name: KC_DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: keycloak-db
                  key: password
            - name: KC_HOSTNAME
              value: keycloak.example.com
            - name: KC_PROXY
              value: edge
            - name: KC_CACHE
              value: ispn
            - name: KC_CACHE_STACK
              value: kubernetes
            - name: JAVA_OPTS_APPEND
              value: "-Djgroups.dns.query=keycloak-headless.default.svc.cluster.local"
            - name: KEYCLOAK_ADMIN
              valueFrom:
                secretKeyRef:
                  name: keycloak-admin
                  key: username
            - name: KEYCLOAK_ADMIN_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: keycloak-admin
                  key: password
          ports:
            - name: http
              containerPort: 8080
            - name: jgroups
              containerPort: 7600
          readinessProbe:
            httpGet:
              path: /health/ready
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /health/live
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: keycloak
spec:
  type: LoadBalancer
  ports:
    - port: 443
      targetPort: 8080
      protocol: TCP
  selector:
    app: keycloak
```

---

## Database Configuration

### PostgreSQL

**keycloak.conf:**
```properties
db=postgres
db-url=jdbc:postgresql://localhost:5432/keycloak
db-username=keycloak
db-password=password
db-pool-initial-size=5
db-pool-min-size=5
db-pool-max-size=20
```

**PostgreSQL setup:**
```sql
CREATE DATABASE keycloak;
CREATE USER keycloak WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;
```

### MySQL

**keycloak.conf:**
```properties
db=mysql
db-url=jdbc:mysql://localhost:3306/keycloak
db-username=keycloak
db-password=password
```

**MySQL setup:**
```sql
CREATE DATABASE keycloak CHARACTER SET utf8 COLLATE utf8_unicode_ci;
CREATE USER 'keycloak'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON keycloak.* TO 'keycloak'@'%';
FLUSH PRIVILEGES;
```

### MariaDB

**keycloak.conf:**
```properties
db=mariadb
db-url=jdbc:mariadb://localhost:3306/keycloak
db-username=keycloak
db-password=password
```

---

**Document Version**: 1.0
**Last Updated**: January 2025
