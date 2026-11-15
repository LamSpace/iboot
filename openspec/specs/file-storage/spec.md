# file-storage Specification

## Purpose

Defines how iBoot selects and uses a file storage provider so that the local filesystem, Silo, Alibaba Cloud OSS, and a test-only in-memory implementation satisfy one behavioral contract and can be swapped by configuration without changing file-management clients.

## Requirements

### Requirement: Provider selection by configuration

The system SHALL select exactly one active file storage provider at startup based on `sys.file.storageType`. Supported values SHALL be `local`, `silo`, and `oss`, and an absent value SHALL resolve to `local`. An unrecognized value SHALL fail application startup with an error identifying the invalid value; the system MUST NOT silently fall back to local storage.

#### Scenario: MinIO selected
- **WHEN** `sys.file.storageType` is the legacy `minio` value
- **THEN** startup fails with a message naming the invalid value
- **AND** the migration path is to set the value to `silo`

#### Scenario: Silo selected
- **WHEN** `sys.file.storageType` is `silo`
- **THEN** all file operations use Silo and the reported storage type is `silo`

#### Scenario: OSS selected
- **WHEN** `sys.file.storageType` is `oss` and valid credentials are present
- **THEN** all file operations use OSS and the reported storage type is `oss`

#### Scenario: Invalid value rejected
- **WHEN** `sys.file.storageType` is an unsupported value such as `s3`
- **THEN** startup fails with a message naming the invalid value
- **AND** no file is written to local storage

### Requirement: Common storage operation contract

Every storage provider SHALL implement the same observable contract for save, existence check, read, and delete. After a successful save at a path, existence SHALL be true, and reading SHALL return the same bytes, byte length, and content type that were saved. Saving to an existing path SHALL replace the prior content. After a successful delete, existence SHALL be false. Deleting a path that does not exist SHALL return a non-success result without an unhandled error.

#### Scenario: Save-read round trip
- **WHEN** a file is saved through any provider
- **THEN** a subsequent read returns identical bytes, size, and content type

#### Scenario: Overwrite
- **WHEN** a second save targets an existing path
- **THEN** reading returns only the second content

#### Scenario: Delete existing and missing paths
- **WHEN** an existing path is deleted, then deleted again
- **THEN** the first delete reports success
- **AND** the second delete reports non-success without an unhandled error

### Requirement: Provider-specific download semantics

For object storage providers (`silo` and `oss`), the download URL SHALL be a time-limited presigned URL, and `GET /api/file/download/{id}` SHALL respond with a 302 redirect to that URL. For the local provider, the download URL SHALL be the application download endpoint, and that endpoint SHALL stream the file with an attachment content disposition and an RFC 5987-encoded filename. File-management API paths and response schemas SHALL remain unchanged, except that the `storageType` response value MAY additionally be `oss`.

#### Scenario: Object storage redirect
- **WHEN** the active storage type is `silo` or `oss` and a client requests `/api/file/download/{id}`
- **THEN** the response is a 302 redirect whose location is a presigned URL

#### Scenario: Local stream
- **WHEN** the active storage type is `local` and a client requests `/api/file/download/{id}`
- **THEN** the response body is the file content with an attachment disposition and an encoded filename

#### Scenario: Non-ASCII filename
- **WHEN** a file with a Chinese filename is downloaded in local mode
- **THEN** the encoded filename resolves to the original name without corruption

### Requirement: OSS credentials and bucket handling

OSS credentials SHALL be supplied through environment variables or an external secret and MUST NOT be stored in the repository or in `sys_config`. The OSS endpoint, bucket name, and presigned URL expiry SHALL be configurable. When OSS is selected but credentials are missing or invalid, startup SHALL fail with an error identifying the missing configuration. The OSS provider SHALL NOT create buckets implicitly; a missing bucket SHALL surface an error naming the bucket.

#### Scenario: Credentials supplied externally
- **WHEN** OSS is selected and credentials exist only in the environment
- **THEN** startup succeeds
- **AND** no credential value appears in configuration files or logs

#### Scenario: Missing credentials
- **WHEN** OSS is selected and credentials are absent
- **THEN** startup fails and names the missing environment variables

#### Scenario: Missing bucket
- **WHEN** OSS is selected and the configured bucket does not exist
- **THEN** the failure names the bucket
- **AND** no bucket is created

### Requirement: Upload validation is provider-independent

Empty files and files violating the configured size or extension limits SHALL be rejected before any storage provider is invoked, so that every provider observes the same accepted input set.

#### Scenario: Empty file rejected
- **WHEN** a zero-byte file is uploaded with any provider configured
- **THEN** the upload is rejected
- **AND** no object is written

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
