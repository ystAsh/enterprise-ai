/*
 * =============================================================================
 * 클래스명 : VectorDocument
 * =============================================================================
 * 목적
 *  - MSSQL vector_documents 테이블의 문서 관리 정보를 표현한다.
 *  - 업로드 문서의 조직, 부서, 보안등급, 소유자 및 처리 상태를 관리한다.
 *  - 이후 PostgreSQL PGVector의 documentId와 논리적으로 연결되는 기준 ID를 제공한다.
 */

package com.example.enterpriseai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 이 클래스를 JPA가 관리하는 MSSQL Entity로 지정한다.
@Entity

// MSSQL vector_documents 테이블과 연결한다.
@Table(name = "vector_documents")
public class VectorDocument {

    // 문서 처리 상태이다.
    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        DELETING,
        DELETED
    }

    // MSSQL vector_documents의 Primary Key이다.
    // 이후 PGVector metadata의 documentId 값으로 사용한다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    // 문서가 속한 조직이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "organization_id",
            nullable = false
    )
    private Organization organization;

    // 문서가 속한 부서이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "department_id",
            nullable = false
    )
    private Department department;

    // 문서를 업로드한 사용자이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "owner_id",
            nullable = false
    )
    private AppUser owner;

    // 문서 자체의 보안등급이다.
    @Column(
            name = "security_level",
            nullable = false
    )
    private int securityLevel;

    // 사용자가 업로드한 원래 파일명이다.
    @Column(
            name = "original_file_name",
            nullable = false,
            length = 255
    )
    private String originalFileName;

    // 서버 저장소에서 사용하는 실제 파일명이다.
    @Column(
            name = "stored_file_name",
            nullable = false,
            length = 255
    )
    private String storedFileName;

    // 서버 저장소에서 파일을 찾기 위한 경로이다.
    @Column(
            name = "storage_path",
            nullable = false,
            length = 500
    )
    private String storagePath;

    // 업로드 파일의 MIME Type이다.
    @Column(
            name = "content_type",
            length = 100
    )
    private String contentType;

    // 업로드 파일 크기(byte)이다.
    @Column(
            name = "file_size",
            nullable = false
    )
    private long fileSize;

    // 문서의 현재 처리 상태이다.
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private Status status;

    // 문서 처리 실패 시 내부 오류 내용을 저장한다.
    @Column(
            name = "error_message",
            length = 1000
    )
    private String errorMessage;

    // 문서 관리 정보 생성 시간이다.
    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    // 문서 관리 정보의 마지막 변경 시간이다.
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    // JPA가 Entity 객체를 생성할 때 사용하는 기본 생성자이다.
    protected VectorDocument() {
    }

    // 새 업로드 문서를 PENDING 상태로 생성한다.
    public static VectorDocument create(
            Organization organization,
            Department department,
            AppUser owner,
            int securityLevel,
            String originalFileName,
            String storedFileName,
            String storagePath,
            String contentType,
            long fileSize
    ) {
        VectorDocument document = new VectorDocument();

        document.organization = organization;
        document.department = department;
        document.owner = owner;
        document.securityLevel = securityLevel;
        document.originalFileName = originalFileName;
        document.storedFileName = storedFileName;
        document.storagePath = storagePath;
        document.contentType = contentType;
        document.fileSize = fileSize;

        // 업로드 직후에는 아직 Parser/Chunk/Embedding 처리를 하지 않은 상태이다.
        document.status = Status.PENDING;

        LocalDateTime now = LocalDateTime.now();
        document.createdAt = now;
        document.updatedAt = now;

        return document;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Department getDepartment() {
        return department;
    }

    public AppUser getOwner() {
        return owner;
    }

    public int getSecurityLevel() {
        return securityLevel;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Status getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}