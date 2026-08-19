/*
 * =============================================================================
 * 클래스명 : DocumentFileStorageService
 * =============================================================================
 * 목적
 *  - 사용자가 업로드한 문서를 서버 파일 저장소에 안전하게 저장한다.
 *  - 사용자 파일명을 직접 저장 파일명으로 사용하지 않고 UUID 기반 이름을 생성한다.
 *  - 파일 저장과 관련된 경로 처리 책임을 한 곳에서 관리한다.
 */

package com.example.enterpriseai.service.document;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class DocumentFileStorageService {

    /*
     * 현재 단계에서는 별도 설정 파일을 만들지 않고
     * 프로젝트 실행 디렉터리 아래 data/documents 폴더를 사용한다.
     *
     * 이후 운영 환경에서는 application.yml 또는 환경변수 기반으로
     * 외부 저장 경로를 주입하도록 변경할 수 있다.
     */
    private final Path storageRoot = Path.of("data", "documents")
            .toAbsolutePath()
            .normalize();

    public DocumentFileStorageService() {
        initializeStorage();
    }

    /*
     * 파일 저장소 디렉터리가 없으면 생성한다.
     */
    private void initializeStorage() {
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "문서 저장 디렉터리를 생성할 수 없습니다: " + storageRoot,
                    e
            );
        }
    }

    /*
     * MultipartFile을 서버 저장소에 저장한다.
     *
     * 반환값:
     *  - DB에 저장할 실제 저장 파일 정보
     */
    public StoredFile store(MultipartFile file) {

        validateFile(file);

        String originalFileName = extractOriginalFileName(file);
        String extension = extractExtension(originalFileName);

        String storedFileName =
                UUID.randomUUID() + extension;

        Path targetPath = storageRoot
                .resolve(storedFileName)
                .normalize();

        // storageRoot 외부로 경로가 벗어나는 것을 방지한다.
        if (!targetPath.startsWith(storageRoot)) {
            throw new IllegalArgumentException("잘못된 파일 저장 경로입니다.");
        }

        try {
            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "문서 파일 저장에 실패했습니다.",
                    e
            );
        }

        return new StoredFile(
                originalFileName,
                storedFileName,
                targetPath.toString(),
                file.getContentType(),
                file.getSize()
        );
    }

    /*
     * 기본적인 업로드 파일 유효성을 검사한다.
     *
     * 아직 PDF/Word 등의 허용 확장자 검증은 하지 않는다.
     * Parser 단계에서 지원 문서 형식 정책이 확정되면 추가한다.
     */
    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        if (file.getOriginalFilename() == null
                || file.getOriginalFilename().isBlank()) {

            throw new IllegalArgumentException("파일명이 없습니다.");
        }
    }

    /*
     * 브라우저가 전달한 전체 경로 형태의 파일명을 제거하고
     * 실제 파일명만 사용한다.
     */
    private String extractOriginalFileName(MultipartFile file) {

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }

        String normalized = originalFileName
                .replace("\\", "/");

        int lastSlashIndex = normalized.lastIndexOf('/');

        if (lastSlashIndex >= 0) {
            normalized = normalized.substring(lastSlashIndex + 1);
        }

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("잘못된 파일명입니다.");
        }

        return normalized;
    }

    /*
     * 원본 파일명에서 확장자만 추출한다.
     *
     * 예:
     * security-policy.pdf
     * → .pdf
     */
    private String extractExtension(String fileName) {

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex).toLowerCase();
    }

    /*
     * 저장 완료 후 Document Upload Service에 전달할 최소 정보이다.
     *
     * 별도 DTO 파일을 만들지 않고 현재 클래스 내부 record로 제한한다.
     */
    public record StoredFile(
            String originalFileName,
            String storedFileName,
            String storagePath,
            String contentType,
            long fileSize
    ) {
    }
}