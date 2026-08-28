/*
 * =============================================================================
 * 클래스명 : ApiExceptionHandler
 * =============================================================================
 * 목적
 *  - REST API에서 발생한 예외를 사용자에게 안전한 응답으로 변환한다.
 *  - 내부 구현 정보와 Stack Trace가 브라우저에 노출되지 않도록 한다.
 *  - 상세 예외는 서버 로그에만 기록한다.
 */

package com.example.enterpriseai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

    // 잘못된 요청 값이나 서버 검증 실패를 안전한 400 응답으로 변환한다.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        log.warn("잘못된 API 요청", exception);

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "요청을 처리할 수 없습니다."
        );
    }

    // 서버 보안 검증에서 거부된 요청은 내부 사유를 노출하지 않는다.
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(
            SecurityException exception
    ) {
        log.warn("API 보안 검증 실패", exception);

        return errorResponse(
                HttpStatus.FORBIDDEN,
                "요청한 작업을 수행할 권한이 없습니다."
        );
    }

    // 현재 단계에서 지원하지 않는 기능은 안전한 400 응답으로 변환한다.
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, String>> handleUnsupportedOperation(
            UnsupportedOperationException exception
    ) {
        log.warn("지원하지 않는 API 요청", exception);

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "현재 지원하지 않는 요청입니다."
        );
    }

    // 분류되지 않은 내부 오류는 상세 내용을 외부에 노출하지 않는다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(
            Exception exception
    ) {
        log.error("API 처리 중 내부 오류 발생", exception);

        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "요청 처리 중 오류가 발생했습니다."
        );
    }

    private ResponseEntity<Map<String, String>> errorResponse(
            HttpStatus status,
            String message
    ) {
        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "message", message
                ));
    }
}