/*
=============================================================================
파일명 : V20__insert_sample_inventory.sql
=============================================================================
목적
 - AC / BC 제품의 학습용 현재 재고 데이터를 생성한다.
 - 정상 재고와 부족 재고를 함께 구성한다.
=============================================================================
*/

INSERT INTO dbo.inventory
(
    organization_id,
    product_id,
    quantity,
    safety_stock_quantity
)
SELECT
    o.organization_id,
    p.product_id,
    v.quantity,
    v.safety_stock_quantity
FROM
    (
        VALUES
            ('AC-100', 50, 20),
            ('AC-200', 8, 15),
            ('BC-100', 30, 10),
            ('BC-200', 4, 12)
    ) v(product_code, quantity, safety_stock_quantity)
        JOIN dbo.organizations o
             ON o.organization_code = 'ORG001'
        JOIN dbo.products p
             ON p.organization_id = o.organization_id
                 AND p.product_code = v.product_code;