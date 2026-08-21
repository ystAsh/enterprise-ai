/*
 * =============================================================================
 * 클래스명 : DatabaseQueryService
 * =============================================================================
 * 목적
 *  - 로그인 사용자의 조직/부서 범위를 기준으로 MSSQL 업무 데이터를 조회한다.
 *  - 검증된 Repository Query를 우선 사용한다.
 *  - 조회 결과를 공통 DatabaseQueryResult 형태로 반환한다.
 *  - Gemini 답변용 data와 사용자에게 제공할 안전한 evidence를 함께 구성한다.
 */

package com.example.enterpriseai.service.database;

import com.example.enterpriseai.dto.DatabaseQueryResult;
import com.example.enterpriseai.repository.EmployeeRepository;
import com.example.enterpriseai.security.CurrentUser;
import com.example.enterpriseai.service.security.DatabaseResultValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class DatabaseQueryService {

    private static final String QUERY_TYPE_EMPLOYEE_COUNT =
            "EMPLOYEE_COUNT";

    private static final String QUERY_KEY_EMPLOYEE_COUNT =
            "EMPLOYEE_COUNT_BY_CURRENT_SCOPE";

    private final EmployeeRepository employeeRepository;
    private final DatabaseResultValidator resultValidator;

    public DatabaseQueryService(
            EmployeeRepository employeeRepository,
            DatabaseResultValidator resultValidator
    ) {
        this.employeeRepository = employeeRepository;
        this.resultValidator = resultValidator;
    }

    /*
     * 현재 로그인 사용자의 조직/부서에 속한
     * 재직 직원 수를 조회하고 검증된 공통 결과로 반환한다.
     */
    @Transactional(readOnly = true)
    public DatabaseQueryResult getCurrentDepartmentEmployeeCount(
            CurrentUser currentUser
    ) {

        long employeeCount =
                employeeRepository
                        .countByOrganization_OrganizationIdAndDepartment_DepartmentIdAndActiveTrue(
                                currentUser.getOrganizationId(),
                                currentUser.getDepartmentId()
                        );

        long validatedCount =
                resultValidator.validateEmployeeCount(
                        employeeCount
                );

        DatabaseQueryResult.Evidence evidence =
                new DatabaseQueryResult.Evidence(
                        "MSSQL",
                        QUERY_KEY_EMPLOYEE_COUNT,
                        "현재 부서 재직 직원 수 조회",
                        "JPA_REPOSITORY",
                        true
                );

        return new DatabaseQueryResult(
                QUERY_TYPE_EMPLOYEE_COUNT,
                Map.of(
                        "employeeCount",
                        validatedCount
                ),
                evidence
        );
    }
}