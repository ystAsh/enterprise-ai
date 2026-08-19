/*
 * =============================================================================
 * 파일명 : V7__create_vector_documents.sql
 * =============================================================================
 * 목적
 *  - 업로드된 문서의 관리 정보를 MSSQL에서 관리한다.
 *  - 문서의 조직, 부서, 보안등급 및 업로드 사용자를 기록한다.
 *  - 이후 PostgreSQL PGVector의 documentId 기준이 되는 MSSQL 문서 ID를 제공한다.
 */

CREATE TABLE vector_documents
(
    document_id BIGINT IDENTITY(1,1) NOT NULL,

    organization_id BIGINT NOT NULL,

    department_id BIGINT NOT NULL,

    owner_id BIGINT NOT NULL,

    security_level INT NOT NULL,

    original_file_name NVARCHAR(255) NOT NULL,

    stored_file_name NVARCHAR(255) NOT NULL,

    storage_path NVARCHAR(500) NOT NULL,

    content_type VARCHAR(100) NULL,

    file_size BIGINT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    error_message NVARCHAR(1000) NULL,

    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT pk_vector_documents
        PRIMARY KEY (document_id),

    CONSTRAINT fk_vector_documents_organization
        FOREIGN KEY (organization_id)
            REFERENCES organizations (organization_id),

    CONSTRAINT fk_vector_documents_department
        FOREIGN KEY (department_id)
            REFERENCES departments (department_id),

    CONSTRAINT fk_vector_documents_owner
        FOREIGN KEY (owner_id)
            REFERENCES app_users (user_id),

    CONSTRAINT ck_vector_documents_security_level
        CHECK (security_level BETWEEN 1 AND 5),

    CONSTRAINT ck_vector_documents_file_size
        CHECK (file_size >= 0),

    CONSTRAINT ck_vector_documents_status
        CHECK (
            status IN (
                       'PENDING',
                       'PROCESSING',
                       'COMPLETED',
                       'FAILED',
                       'DELETING',
                       'DELETED'
                )
            )
);

CREATE INDEX ix_vector_documents_security_scope
    ON vector_documents (
                         organization_id,
                         department_id,
                         security_level
        );

CREATE INDEX ix_vector_documents_owner
    ON vector_documents (owner_id);

CREATE INDEX ix_vector_documents_status
    ON vector_documents (status);