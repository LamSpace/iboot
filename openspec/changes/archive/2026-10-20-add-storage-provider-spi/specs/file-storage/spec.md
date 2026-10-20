# Spec Delta

## Purpose

Defines how iBoot selects and uses a file storage provider so that the local filesystem, MinIO, Alibaba Cloud OSS, and a test-only in-memory implementation satisfy one behavioral contract and can be swapped by configuration without changing file-management clients.

## ADDED Requirements

### Requirement: Provider selection by configuration

The system SHALL select exactly one active file storage provider at startup based on `sys.file.storageType`. Supported values SHALL be `local`, `minio`, and `oss`, and an absent value SHALL resolve to `local`. An unrecognized value SHALL fail application startup with an error identifying the invalid value; the system MUST NOT silently fall back to local storage.

#### Scenario: MinIO selected
- **WHEN** `sys.file.storageType` is `minio`
- **THEN** all file operations use MinIO and the reported storage type is `minio`

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

For object storage providers (`minio` and `oss`), the download URL SHALL be a time-limited presigned URL, and `GET /api/file/download/{id}` SHALL respond with a 302 redirect to that URL. For the local provider, the download URL SHALL be the application download endpoint, and that endpoint SHALL stream the file with an attachment content disposition and an RFC 5987-encoded filename. File-management API paths and response schemas SHALL remain unchanged, except that the `storageType` response value MAY additionally be `oss`.

#### Scenario: Object storage redirect
- **WHEN** the active storage type is `minio` or `oss` and a client requests `/api/file/download/{id}`
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
