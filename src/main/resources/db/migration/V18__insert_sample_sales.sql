/*
=============================================================================
파일명 : V18__insert_sample_sales.sql
=============================================================================
목적
 - AC / BC 제품의 학습용 매출 데이터를 생성한다.
 - DEPT001 / DEPT002 데이터를 함께 구성하여 권한 범위 테스트에 사용한다.
=============================================================================
*/

INSERT INTO dbo.sales
(
    organization_id,
    department_id,
    product_id,
    sale_date,
    quantity,
    unit_price,
    total_amount
)
SELECT
    o.organization_id,
    d.department_id,
    p.product_id,
    v.sale_date,
    v.quantity,
    p.unit_price,
    p.unit_price * v.quantity
FROM
    (
        VALUES
            ('DEPT001', 'AC-100', CAST('2026-07-05' AS DATE), 10),
            ('DEPT001', 'AC-200', CAST('2026-07-15' AS DATE), 5),
            ('DEPT001', 'BC-100', CAST('2026-07-20' AS DATE), 8),
            ('DEPT001', 'BC-200', CAST('2026-08-10' AS DATE), 4),

            ('DEPT002', 'AC-100', CAST('2026-07-07' AS DATE), 20),
            ('DEPT002', 'BC-200', CAST('2026-07-22' AS DATE), 12)
    ) v(department_code, product_code, sale_date, quantity)
        JOIN dbo.organizations o
             ON o.organization_code = 'ORG001'
        JOIN dbo.departments d
             ON d.organization_id = o.organization_id
                 AND d.department_code = v.department_code
        JOIN dbo.products p
             ON p.organization_id = o.organization_id
                 AND p.product_code = v.product_code;