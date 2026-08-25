/*
=============================================================================
파일명 : V17__create_sales.sql
=============================================================================
목적
 - 가상 기존 시스템의 매출 데이터를 저장한다.
 - 조직/부서 범위 검증이 가능하도록 구성한다.
=============================================================================
*/

CREATE TABLE dbo.sales
(
    sale_id BIGINT IDENTITY(1,1) NOT NULL,

    organization_id BIGINT NOT NULL,

    department_id BIGINT NOT NULL,

    product_id BIGINT NOT NULL,

    sale_date DATE NOT NULL,

    quantity INT NOT NULL,

    unit_price DECIMAL(18,2) NOT NULL,

    total_amount DECIMAL(18,2) NOT NULL,

    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_sales_created_at
        DEFAULT SYSDATETIME(),

    CONSTRAINT PK_sales
        PRIMARY KEY (sale_id),

    CONSTRAINT FK_sales_organization
        FOREIGN KEY (organization_id)
            REFERENCES dbo.organizations (organization_id),

    CONSTRAINT FK_sales_department
        FOREIGN KEY (department_id)
            REFERENCES dbo.departments (department_id),

    CONSTRAINT FK_sales_product
        FOREIGN KEY (product_id)
            REFERENCES dbo.products (product_id),

    CONSTRAINT CK_sales_quantity
        CHECK (quantity > 0),

    CONSTRAINT CK_sales_unit_price
        CHECK (unit_price >= 0),

    CONSTRAINT CK_sales_total_amount
        CHECK (total_amount >= 0)
);

CREATE INDEX IX_sales_scope_date
    ON dbo.sales
        (
         organization_id,
         department_id,
         sale_date
            );

CREATE INDEX IX_sales_product
    ON dbo.sales
        (
         product_id,
         sale_date
            );