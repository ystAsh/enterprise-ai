/*
 * =============================================================================
 * 클래스명 : VectorDocumentService
 * =============================================================================
 * 목적
 *  - 업로드 문서를 서버 파일 저장소에 저장한다.
 *  - 로그인 사용자의 조직, 부서, 사용자 정보를 기준으로 문서 관리 정보를 생성한다.
 *  - 생성된 문서 관리 정보를 MSSQL vector_documents 테이블에 저장한다.
 */

package com.example.enterpriseai.service.document;

import com.example.enterpriseai.entity.AppUser;
import com.example.enterpriseai.entity.Department;
import com.example.enterpriseai.entity.Organization;
import com.example.enterpriseai.entity.VectorDocument;
import com.example.enterpriseai.repository.AppUserRepository;
import com.example.enterpriseai.repository.DepartmentRepository;
import com.example.enterpriseai.repository.OrganizationRepository;
import com.example.enterpriseai.repository.VectorDocumentRepository;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.vector.DocumentEmbeddingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.util.List;
import org.springframework.ai.document.Document;

@Service
public class VectorDocumentService {

    private final DocumentFileStorageService fileStorageService;
    private final VectorDocumentRepository vectorDocumentRepository;
    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final PdfDocumentParser pdfDocumentParser;
    private final DocumentChunkService documentChunkService;
    private final DocumentEmbeddingService documentEmbeddingService;

    public VectorDocumentService(
            DocumentFileStorageService fileStorageService,
            VectorDocumentRepository vectorDocumentRepository,
            AppUserRepository appUserRepository,
            OrganizationRepository organizationRepository,
            DepartmentRepository departmentRepository,
            PdfDocumentParser pdfDocumentParser,
            DocumentChunkService documentChunkService,
            DocumentEmbeddingService documentEmbeddingService
    ) {
        this.fileStorageService = fileStorageService;
        this.vectorDocumentRepository = vectorDocumentRepository;
        this.appUserRepository = appUserRepository;
        this.organizationRepository = organizationRepository;
        this.departmentRepository = departmentRepository;
        this.pdfDocumentParser = pdfDocumentParser;
        this.documentChunkService = documentChunkService;
        this.documentEmbeddingService = documentEmbeddingService;
    }

    /*
     * 로그인 사용자의 권한 범위를 기준으로 문서를 업로드한다.
     *
     * organizationId, departmentId, ownerId는
     * 클라이언트 입력을 사용하지 않고 CurrentUser에서 가져온다.
     */
    @Transactional
    public VectorDocument upload(
            MultipartFile file,
            int documentSecurityLevel,
            CurrentUser currentUser
    ) {

        validateSecurityLevel(
                documentSecurityLevel,
                currentUser
        );

        AppUser owner = appUserRepository.findById(
                currentUser.getUserId()
        ).orElseThrow(
                () -> new IllegalStateException("로그인 사용자 정보를 찾을 수 없습니다.")
        );

        Organization organization = organizationRepository.findById(
                currentUser.getOrganizationId()
        ).orElseThrow(
                () -> new IllegalStateException("사용자 조직 정보를 찾을 수 없습니다.")
        );

        Department department = departmentRepository.findById(
                currentUser.getDepartmentId()
        ).orElseThrow(
                () -> new IllegalStateException("사용자 부서 정보를 찾을 수 없습니다.")
        );

        DocumentFileStorageService.StoredFile storedFile =
                fileStorageService.store(file);

        VectorDocument document = VectorDocument.create(
                organization,
                department,
                owner,
                documentSecurityLevel,
                storedFile.originalFileName(),
                storedFile.storedFileName(),
                storedFile.storagePath(),
                storedFile.contentType(),
                storedFile.fileSize()
        );

        return vectorDocumentRepository.save(document);
    }

    /*
     * 업로드된 PDF 문서를 읽어 텍스트를 추출한다.
     *
     * 문서를 읽기 전에 로그인 사용자의 조직, 부서,
     * 보안등급 범위를 서버에서 다시 검증한다.
     */
    @Transactional(readOnly = true)
    public String parsePdf(
            Long documentId,
            CurrentUser currentUser
    ) {

        VectorDocument document =
                vectorDocumentRepository.findById(documentId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "문서를 찾을 수 없습니다."
                                )
                        );

        validateDocumentAccess(
                document,
                currentUser
        );

        return pdfDocumentParser.parse(
                Path.of(document.getStoragePath())
        );
    }

    /*
     * 업로드된 PDF 문서를 읽고 Chunk 단위로 분할한다.
     *
     * 문서 접근 권한을 먼저 검증한 뒤
     * Parser → Chunk → Metadata 구성 순서로 처리한다.
     */
    @Transactional(readOnly = true)
    public List<org.springframework.ai.document.Document> chunkPdf(
            Long documentId,
            CurrentUser currentUser
    ) {

        VectorDocument document =
                vectorDocumentRepository.findById(documentId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "문서를 찾을 수 없습니다."
                                )
                        );

        // 문서를 읽기 전에 서버에서 접근 권한을 검증한다.
        validateDocumentAccess(
                document,
                currentUser
        );

        // 실제 저장된 PDF에서 전체 텍스트를 추출한다.
        String text =
                pdfDocumentParser.parse(
                        Path.of(document.getStoragePath())
                );

        // MSSQL 문서 관리 정보를 Chunk Metadata에 연결한다.
        return documentChunkService.split(
                text,
                document.getDocumentId(),
                document.getOrganization().getOrganizationId(),
                document.getDepartment().getDepartmentId(),
                document.getSecurityLevel(),
                document.getOriginalFileName()
        );
    }

    /*
     * 업로드된 PDF 문서를 파싱하고 Chunk로 분할한 뒤
     * Spring AI VectorStore를 통해 Embedding 및 PGVector 저장을 수행한다.
     *
     * 권한 검증을 통과한 문서만 Embedding 대상으로 사용한다.
     */
    @Transactional(readOnly = true)
    public int embedPdf(
            Long documentId,
            CurrentUser currentUser
    ) {

        VectorDocument document =
                vectorDocumentRepository.findById(documentId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "문서를 찾을 수 없습니다."
                                )
                        );

        // 권한 없는 문서는 Parser/Embedding 단계로 전달하지 않는다.
        validateDocumentAccess(
                document,
                currentUser
        );

        // 저장된 PDF에서 전체 텍스트를 추출한다.
        String text =
                pdfDocumentParser.parse(
                        Path.of(document.getStoragePath())
                );

        // MSSQL의 검증된 문서정보를 Metadata로 사용하여 Chunk를 생성한다.
        List<Document> chunks =
                documentChunkService.split(
                        text,
                        document.getDocumentId(),
                        document.getOrganization().getOrganizationId(),
                        document.getDepartment().getDepartmentId(),
                        document.getSecurityLevel(),
                        document.getOriginalFileName()
                );

        // VectorStore.add()를 통해 Embedding 생성 및 PGVector 저장을 수행한다.
        documentEmbeddingService.store(chunks);

        return chunks.size();
    }

    /*
     * 사용자는 자신의 Security Level보다 높은 등급으로
     * 문서를 등록할 수 없도록 제한한다.
     */
    private void validateSecurityLevel(
            int documentSecurityLevel,
            CurrentUser currentUser
    ) {

        if (documentSecurityLevel < 1 || documentSecurityLevel > 5) {
            throw new IllegalArgumentException(
                    "문서 보안등급은 1부터 5 사이여야 합니다."
            );
        }

        if (documentSecurityLevel > currentUser.getSecurityLevel()) {
            throw new IllegalArgumentException(
                    "사용자 보안등급보다 높은 문서를 등록할 수 없습니다."
            );
        }
    }

    /*
     * 문서를 읽거나 처리하기 전에
     * 로그인 사용자의 문서 접근 권한을 검증한다.
     *
     * organizationId, departmentId, securityLevel은
     * 클라이언트나 LLM의 값을 사용하지 않는다.
     */
    private void validateDocumentAccess(
            VectorDocument document,
            CurrentUser currentUser
    ) {

        // 다른 조직의 문서는 접근할 수 없다.
        if (!document.getOrganization()
                .getOrganizationId()
                .equals(currentUser.getOrganizationId())) {

            throw new IllegalArgumentException(
                    "해당 문서에 접근할 수 없습니다."
            );
        }

        // 다른 부서의 문서는 접근할 수 없다.
        if (!document.getDepartment()
                .getDepartmentId()
                .equals(currentUser.getDepartmentId())) {

            throw new IllegalArgumentException(
                    "해당 문서에 접근할 수 없습니다."
            );
        }

        // 자신의 Security Level보다 높은 문서는 접근할 수 없다.
        if (document.getSecurityLevel()
                > currentUser.getSecurityLevel()) {

            throw new IllegalArgumentException(
                    "해당 문서에 접근할 수 없습니다."
            );
        }
    }
}