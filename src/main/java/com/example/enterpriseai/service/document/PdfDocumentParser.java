/*
 * =============================================================================
 * 클래스명 : PdfDocumentParser
 * =============================================================================
 * 목적
 *  - 서버 저장소에 저장된 PDF 파일에서 텍스트를 추출한다.
 *  - 추출된 텍스트를 이후 Chunking 단계에서 사용할 수 있도록 반환한다.
 */

package com.example.enterpriseai.service.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PdfDocumentParser {

    // 저장된 PDF 파일을 읽어서 전체 텍스트를 반환한다.
    public String parse(Path filePath) {

        validateFile(filePath);

        try (PDDocument document =
                     Loader.loadPDF(filePath.toFile())) {

            PDFTextStripper textStripper =
                    new PDFTextStripper();

            String text =
                    textStripper.getText(document);

            if (text == null || text.isBlank()) {
                throw new IllegalStateException(
                        "PDF에서 추출된 텍스트가 없습니다."
                );
            }

            return text.trim();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "PDF 문서 파싱에 실패했습니다: " + filePath,
                    e
            );
        }
    }

    // 실제 PDF 파싱 전에 파일 상태를 확인한다.
    private void validateFile(Path filePath) {

        if (filePath == null) {
            throw new IllegalArgumentException(
                    "PDF 파일 경로가 없습니다."
            );
        }

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException(
                    "PDF 파일이 존재하지 않습니다: " + filePath
            );
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException(
                    "정상적인 파일이 아닙니다: " + filePath
            );
        }

        String fileName =
                filePath.getFileName()
                        .toString()
                        .toLowerCase();

        if (!fileName.endsWith(".pdf")) {
            throw new IllegalArgumentException(
                    "PDF 파일만 파싱할 수 있습니다."
            );
        }
    }
}