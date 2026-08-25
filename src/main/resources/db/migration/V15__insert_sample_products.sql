/*
=============================================================================
파일명 : V15__insert_sample_products.sql
=============================================================================
목적
 - AC / BC 시리즈의 학습용 제품 데이터를 생성한다.
=============================================================================
*/

INSERT INTO dbo.products
(
    organization_id,
    series_id,
    product_code,
    product_name,
    unit_price,
    active
)
SELECT
    o.organization_id,
    s.series_id,
    v.product_code,
    v.product_name,
    v.unit_price,
    1
FROM
    (
        VALUES
            ('AC', 'AC-100', N'AC 기본형', 100000.00),
            ('AC', 'AC-200', N'AC 고급형', 180000.00),
            ('BC', 'BC-100', N'BC 기본형', 120000.00),
            ('BC', 'BC-200', N'BC 고급형', 220000.00)
    ) v(series_code, product_code, product_name, unit_price)
        JOIN dbo.series s
             ON s.series_code = v.series_code
        JOIN dbo.organizations o
             ON o.organization_code = 'ORG001';