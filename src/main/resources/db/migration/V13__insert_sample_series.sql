/*
=============================================================================
파일명 : V13__insert_sample_series.sql
=============================================================================
목적
 - 가상의 기존 업무 시스템에서 사용할 학습용 시리즈 데이터를 등록한다.
 - AC / BC는 Java 코드가 아닌 업무 데이터로 관리한다.
=============================================================================
*/

INSERT INTO dbo.series
(
    series_code,
    series_name,
    description,
    active
)
VALUES
    (
        'AC',
        N'AC 시리즈',
        N'학습용 AC 계열 시리즈',
        1
    );

INSERT INTO dbo.series
(
    series_code,
    series_name,
    description,
    active
)
VALUES
    (
        'BC',
        N'BC 시리즈',
        N'학습용 BC 계열 시리즈',
        1
    );