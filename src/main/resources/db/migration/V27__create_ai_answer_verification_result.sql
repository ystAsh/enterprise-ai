/*
 * =============================================================================
 * Migration : V27__create_ai_answer_verification_result.sql
 * =============================================================================
 * 목적
 *  - Answer Verification의 질문 단위 평가 결과를 구조화하여 저장한다.
 *  - Secure Verification Evidence와 Evaluation 결과를 분리한다.
 *  - Fact Count와 Match Rate를 함께 저장하여 평가 결과를 재검증할 수 있게 한다.
 *  - Raw SQL, Raw Result, 실제 Parameter 값은 저장하지 않는다.
 */

CREATE TABLE dbo.ai_answer_verification_result (
                                                   answer_verification_result_id BIGINT IDENTITY(1,1) NOT NULL,

                                                   verification_evidence_id BIGINT NOT NULL,

                                                   overall_status VARCHAR(20) NOT NULL,

                                                   matched_fact_count INT NOT NULL,
                                                   mismatch_fact_count INT NOT NULL,
                                                   unsupported_fact_count INT NOT NULL,
                                                   missing_fact_count INT NOT NULL,
                                                   evaluated_fact_count INT NOT NULL,

                                                   match_rate DECIMAL(5,2) NULL,

                                                   verifier_version VARCHAR(100) NOT NULL,
                                                   verified_at DATETIME2(3) NOT NULL,

                                                   CONSTRAINT pk_ai_answer_verification_result
                                                       PRIMARY KEY (answer_verification_result_id),

                                                   CONSTRAINT fk_answer_verification_evidence
                                                       FOREIGN KEY (verification_evidence_id)
                                                           REFERENCES dbo.ai_secure_verification_evidence (verification_evidence_id),

                                                   CONSTRAINT ck_answer_verification_overall_status
                                                       CHECK (
                                                           overall_status IN (
                                                                              'MATCH',
                                                                              'MISMATCH',
                                                                              'UNSUPPORTED',
                                                                              'MISSING'
                                                               )
                                                           ),

                                                   CONSTRAINT ck_answer_verification_fact_counts
                                                       CHECK (
                                                           matched_fact_count >= 0
                                                               AND mismatch_fact_count >= 0
                                                               AND unsupported_fact_count >= 0
                                                               AND missing_fact_count >= 0
                                                               AND evaluated_fact_count >= 0
                                                           ),

                                                   CONSTRAINT ck_answer_verification_evaluated_count
                                                       CHECK (
                                                           evaluated_fact_count =
                                                           matched_fact_count
                                                               + mismatch_fact_count
                                                               + unsupported_fact_count
                                                               + missing_fact_count
                                                           ),

                                                   CONSTRAINT ck_answer_verification_match_rate
                                                       CHECK (
                                                           (matched_fact_count + mismatch_fact_count = 0 AND match_rate IS NULL)
                                                               OR
                                                           (
                                                               matched_fact_count + mismatch_fact_count > 0
                                                                   AND match_rate IS NOT NULL
                                                                   AND match_rate >= 0
                                                                   AND match_rate <= 100
                                                               )
                                                           )
);

CREATE INDEX ix_answer_verification_evidence
    ON dbo.ai_answer_verification_result (
                                          verification_evidence_id
        );

CREATE INDEX ix_answer_verification_verified_at
    ON dbo.ai_answer_verification_result (
                                          verified_at
        );

CREATE INDEX ix_answer_verification_status
    ON dbo.ai_answer_verification_result (
                                          overall_status
        );

CREATE INDEX ix_answer_verification_version
    ON dbo.ai_answer_verification_result (
                                          verifier_version
        );