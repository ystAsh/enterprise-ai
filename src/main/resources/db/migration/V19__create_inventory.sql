/*
=============================================================================
파일명 : V19__create_inventory.sql
=============================================================================
목적
 - 가상 기존 시스템의 현재 재고 데이터를 저장한다.
=============================================================================
*/

CREATE TABLE dbo.inventory
(
    inventory_id BIGINT IDENTITY(1,1) NOT NULL,

    organization_id BIGINT NOT NULL,

    product_id BIGINT NOT NULL,

    quantity INT NOT NULL,

    safety_stock_quantity INT NOT NULL,

    updated_at DATETIME2 NOT NULL
        CONSTRAINT DF_inventory_updated_at
        DEFAULT SYSDATETIME(),

    CONSTRAINT PK_inventory
        PRIMARY KEY (inventory_id),

    CONSTRAINT UK_inventory_org_product
        UNIQUE (organization_id, product_id),

    CONSTRAINT FK_inventory_organization
        FOREIGN KEY (organization_id)
            REFERENCES dbo.organizations (organization_id),

    CONSTRAINT FK_inventory_product
        FOREIGN KEY (product_id)
            REFERENCES dbo.products (product_id),

    CONSTRAINT CK_inventory_quantity
        CHECK (quantity >= 0),

    CONSTRAINT CK_inventory_safety_stock
        CHECK (safety_stock_quantity >= 0)
);

CREATE INDEX IX_inventory_scope
    ON dbo.inventory
        (
         organization_id,
         quantity
            );