/*
 * =============================================================================
 * 클래스명 : VectorDocumentRepository
 * =============================================================================
 * 목적
 *  - MSSQL vector_documents 테이블의 문서 관리 정보를 JPA로 저장하고 조회한다.
 *  - Document Upload 단계에서 VectorDocument Entity의 영속성 처리를 담당한다.
 */

package com.example.enterpriseai.repository;

import com.example.enterpriseai.entity.VectorDocument;
import org.springframework.data.jpa.repository.JpaRepository;

// MSSQL Primary DataSource의 JPA Repository로 동작한다.
public interface VectorDocumentRepository
        extends JpaRepository<VectorDocument, Long> {
}