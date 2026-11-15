# Spec Delta

## MODIFIED Requirements

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
