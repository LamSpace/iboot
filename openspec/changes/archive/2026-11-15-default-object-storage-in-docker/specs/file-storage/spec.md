# Spec Delta

## ADDED Requirements

### Requirement: Provisioned default uses object storage

A freshly initialized database SHALL provision `sys.file.storageType` as `silo`, so a full Docker Compose deployment uses object storage without manual reconfiguration. Upgrading an existing database SHALL also change a persisted `local` to `silo`; an environment that needs local storage SHALL opt out through an environment override or by setting the value back after upgrade.

#### Scenario: Fresh Docker deployment uses Silo

- **WHEN** a fresh database is initialized and all services start with Docker Compose
- **THEN** `sys.file.storageType` resolves to `silo`
- **AND** uploaded files are written to the configured Silo bucket

#### Scenario: Existing local value is upgraded

- **WHEN** an existing database has `sys.file.storageType` set to `local`
- **THEN** applying the new migration changes it to `silo`
- **AND** an environment that needs local storage can set `IBOOT_FILE_STORAGE_TYPE=local` to override it

### Requirement: Environment overrides for storage configuration

Storage type, internal endpoint, public endpoint, access key, secret key, and bucket name SHALL be overridable by environment variables. An environment value SHALL take precedence over the corresponding `sys_config` value. An unsupported storage type from either source SHALL fail startup and name the offending value.

#### Scenario: Environment type overrides database value

- **WHEN** `IBOOT_FILE_STORAGE_TYPE` is `silo` and `sys.file.storageType` is `local`
- **THEN** the effective storage type is `silo`

#### Scenario: Local override wins over Silo default

- **WHEN** `IBOOT_FILE_STORAGE_TYPE` is `local` and `sys.file.storageType` is `silo`
- **THEN** the effective storage type is `local`
- **AND** files are written to the local upload directory

#### Scenario: Environment endpoint is used inside containers

- **WHEN** `IBOOT_FILE_SILO_ENDPOINT` is `http://silo:9000`
- **THEN** server-side Silo operations connect to `http://silo:9000` instead of the database endpoint

#### Scenario: Invalid environment value fails startup

- **WHEN** `IBOOT_FILE_STORAGE_TYPE` is an unsupported value such as `s3`
- **THEN** startup fails with a message naming the invalid value

### Requirement: Separate internal and public object-storage endpoints

Object-storage server operations SHALL use the internal endpoint. Presigned download URLs SHALL be signed against a public endpoint that the browser can reach. When no public endpoint is configured, it SHALL default to the internal endpoint. Signing MUST use a client configured for the public endpoint so the URL signature remains valid for that host.

#### Scenario: Docker uses different internal and public hosts

- **WHEN** the internal endpoint is `http://silo:9000` and the public endpoint is `http://localhost:9000`
- **THEN** uploads and reads go to `http://silo:9000`
- **AND** a download URL redirects to a presigned URL whose host is `http://localhost:9000`

#### Scenario: Public endpoint defaults to internal

- **WHEN** no public endpoint is configured
- **THEN** presigned download URLs use the same host as the internal endpoint

#### Scenario: Presigned URL is usable by the browser

- **WHEN** a client follows the download redirect from outside the container network
- **THEN** the presigned URL is reachable and its signature is accepted by object storage
