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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VectorDocumentService {

    private final DocumentFileStorageService fileStorageService;
    private final VectorDocumentRepository vectorDocumentRepository;
    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;

    public VectorDocumentService(
            DocumentFileStorageService fileStorageService,
            VectorDocumentRepository vectorDocumentRepository,
            AppUserRepository appUserRepository,
            OrganizationRepository organizationRepository,
            DepartmentRepository departmentRepository
    ) {
        this.fileStorageService = fileStorageService;
        this.vectorDocumentRepository = vectorDocumentRepository;
        this.appUserRepository = appUserRepository;
        this.organizationRepository = organizationRepository;
        this.departmentRepository = departmentRepository;
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
}