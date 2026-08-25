/*
=============================================================================
파일명 : V12__create_series.sql
=============================================================================
목적
 - 가상의 기존 업무 시스템에서 사용할 시리즈 기준 테이블을 생성한다.
 - AC / BC 등의 시리즈는 Java 코드에 하드코딩하지 않고 데이터로 관리한다.
 - AI/RAG 전용 테이블이 아니라 기존 업무 시스템 역할의 일반 업무 테이블이다.
=============================================================================
*/

CREATE TABLE dbo.series
(
    series_id BIGINT IDENTITY(1,1) NOT NULL,

    series_code VARCHAR(30) NOT NULL,

    series_name NVARCHAR(100) NOT NULL,

    description NVARCHAR(500) NULL,

    active BIT NOT NULL
        CONSTRAINT DF_series_active
        DEFAULT 1,

    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_series_created_at
        DEFAULT SYSDATETIME(),

    updated_at DATETIME2 NOT NULL
        CONSTRAINT DF_series_updated_at
        DEFAULT SYSDATETIME(),

    CONSTRAINT PK_series
        PRIMARY KEY (series_id),

    CONSTRAINT UK_series_code
        UNIQUE (series_code)
);