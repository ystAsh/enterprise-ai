/*
 * =============================================================================
 * 클래스명 : DatabaseResultReferenceStore
 * =============================================================================
 * 목적
 *  - 검증 완료된 대량 Database Query 결과를 서버 메모리에 임시 보관한다.
 *  - 외부에 노출 가능한 opaque resultReference를 발급한다.
 *  - reference만으로 결과에 접근하지 못하도록 소유 사용자 정보를 서버 내부에서 관리한다.
 *  - 특정 회사나 업무 도메인에 종속되지 않는다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.dto.DatabaseResultReference;
import com.example.enterpriseai.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DatabaseResultReferenceStore {

    private final Map<String, StoredResult> results =
            new ConcurrentHashMap<>();

    /*
     * 검증 완료된 대량 결과를 저장하고
     * 외부 노출용 opaque reference를 반환한다.
     */
    public String save(
            DatabaseQueryResult queryResult,
            CurrentUser currentUser
    ) {

        if (queryResult == null) {
            throw new IllegalArgumentException(
                    "저장할 Database 결과가 없습니다."
            );
        }

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

        String referenceId =
                UUID.randomUUID().toString();

        DatabaseResultReference reference =
                new DatabaseResultReference(
                        referenceId,
                        queryResult.data(),
                        queryResult.metadata(),
                        Instant.now()
                );

        StoredResult storedResult =
                new StoredResult(
                        reference,
                        currentUser.getUserId()
                );

        results.put(
                referenceId,
                storedResult
        );

        return referenceId;
    }

    /*
     * 현재 로그인 사용자가 생성한 결과만 반환한다.
     */
    public DatabaseResultReference getRequired(
            String referenceId,
            CurrentUser currentUser
    ) {

        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException(
                    "결과 참조 정보가 없습니다."
            );
        }

        if (currentUser == null
                || currentUser.getUserId() == null) {

            throw new SecurityException(
                    "인증된 사용자가 없습니다."
            );
        }

        StoredResult storedResult =
                results.get(referenceId);

        if (storedResult == null) {
            throw new IllegalArgumentException(
                    "조회 가능한 결과가 없습니다."
            );
        }

        if (!storedResult.ownerUserId()
                .equals(currentUser.getUserId())) {

            throw new SecurityException(
                    "조회 가능한 결과가 없습니다."
            );
        }

        return storedResult.reference();
    }

    /*
     * 사용자 소유정보는 외부 반환 객체에 포함하지 않고
     * 서버 저장소 내부에서만 관리한다.
     */
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