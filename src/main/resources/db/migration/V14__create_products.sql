/*
=============================================================================
파일명 : V14__create_products.sql
=============================================================================
목적
 - 가상 기존 시스템의 제품 테이블을 생성한다.
 - 제품은 조직 및 시리즈와 연결한다.
=============================================================================
*/

CREATE TABLE dbo.products
(
    product_id BIGINT IDENTITY(1,1) NOT NULL,

    organization_id BIGINT NOT NULL,

    series_id BIGINT NOT NULL,

    product_code VARCHAR(30) NOT NULL,

    product_name NVARCHAR(100) NOT NULL,

    unit_price DECIMAL(18,2) NOT NULL,

    active BIT NOT NULL
        CONSTRAINT DF_products_active
        DEFAULT 1,

    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_products_created_at
        DEFAULT SYSDATETIME(),

    updated_at DATETIME2 NOT NULL
        CONSTRAINT DF_products_updated_at
        DEFAULT SYSDATETIME(),

    CONSTRAINT PK_products
        PRIMARY KEY (product_id),

    CONSTRAINT UK_products_product_code
        UNIQUE (product_code),

    CONSTRAINT FK_products_organization
        FOREIGN KEY (organization_id)
            REFERENCES dbo.organizations (organization_id),

    CONSTRAINT FK_products_series
        FOREIGN KEY (series_id)
            REFERENCES dbo.series (series_id),

    CONSTRAINT CK_products_unit_price
        CHECK (unit_price >= 0)
);

CREATE INDEX IX_products_scope_series
    ON dbo.products
        (
         organization_id,
         series_id,
         active
            );