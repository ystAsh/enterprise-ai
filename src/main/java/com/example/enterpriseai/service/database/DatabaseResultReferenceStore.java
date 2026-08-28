/*
 * =============================================================================
 * 클래스명 : DatabaseResultReferenceStore
 * =============================================================================
 * 목적
 *  - 검증 완료된 대량 Database Query 결과를 서버 메모리에 임시 보관한다.
 *  - 외부에 노출할 opaque resultReference를 발급한다.
 *  - 현재 로그인 사용자의 결과만 다시 조회할 수 있도록 소유권을 검증한다.
 *  - 일정 시간이 지난 결과를 만료시켜 장시간 메모리에 보관하지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.dto.DatabaseResultReference;
import com.example.enterpriseai.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DatabaseResultReferenceStore {

    private static final Duration RESULT_TTL = Duration.ofMinutes(30);

    private final Map<String, StoredResult> results = new ConcurrentHashMap<>();

    // 검증 완료된 결과를 현재 사용자 소유의 임시 결과로 저장한다.
    public String save(
            DatabaseQueryResult queryResult,
            CurrentUser currentUser
    ) {
        if (queryResult == null) {
            throw new IllegalArgumentException(
                    "저장할 Database 결과가 없습니다."
            );
        }

        validateCurrentUser(currentUser);

        String referenceId = UUID.randomUUID().toString();

        DatabaseResultReference reference = new DatabaseResultReference(
                referenceId,
                queryResult.data(),
                queryResult.metadata(),
                Instant.now()
        );

        StoredResult storedResult = new StoredResult(
                reference,
                currentUser.getUserId()
        );

        results.put(referenceId, storedResult);

        return referenceId;
    }

    // resultReference의 존재 여부, 만료 여부, 사용자 소유권을 검증한다.
    public DatabaseResultReference getRequired(
            String referenceId,
            CurrentUser currentUser
    ) {
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException(
                    "결과 참조 정보가 없습니다."
            );
        }

        validateCurrentUser(currentUser);

        StoredResult storedResult = results.get(referenceId);

        if (storedResult == null) {
            throw new IllegalArgumentException(
                    "조회 가능한 결과가 없습니다."
            );
        }

        // 만료된 결과는 즉시 메모리에서 제거하고 접근을 차단한다.
        if (isExpired(storedResult.reference())) {
            results.remove(referenceId, storedResult);

            throw new IllegalArgumentException(
                    "조회 가능한 결과가 없습니다."
            );
        }

        if (!storedResult.ownerUserId().equals(currentUser.getUserId())) {
            throw new SecurityException(
                    "조회 가능한 결과가 없습니다."
            );
        }

        return storedResult.reference();
    }

    // 결과 생성 시각으로부터 허용된 TTL이 지났는지 확인한다.
    private boolean isExpired(DatabaseResultReference reference) {
        Instant expiresAt = reference.createdAt().plus(RESULT_TTL);
        return !Instant.now().isBefore(expiresAt);
    }

    // 사용자 식별정보는 브라우저 값이 아닌 Spring Security CurrentUser만 사용한다.
    private void validateCurrentUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new SecurityException(
                    "인증된 사용자가 없습니다."
            );
        }

        if (currentUser.getUserId() == null) {
            throw new SecurityException(
                    "인증된 사용자 식별정보가 없습니다."
            );
        }
    }

    private record StoredResult(
            DatabaseResultReference reference,
            Long ownerUserId
    ) {
        private StoredResult {
            if (reference == null) {
                throw new IllegalArgumentException(
                        "저장할 결과 정보가 없습니다."
                );
            }

            if (ownerUserId == null) {
                throw new IllegalArgumentException(
                        "결과 소유 사용자 정보가 없습니다."
                );
            }
        }
    }
}