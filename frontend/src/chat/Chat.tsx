/*
 * =============================================================================
 * 컴포넌트명 : Chat
 * =============================================================================
 * 목적
 *  - 로그인한 사용자에게 Enterprise AI 채팅 화면을 제공한다.
 *  - 사용자 질문을 Spring Boot /api/chat API로 전달한다.
 *  - CSRF 토큰을 포함하여 세션 기반 요청을 안전하게 처리한다.
 *  - Gemini 답변과 검증 완료 구조화 데이터를 화면에 표시한다.
 *  - 특정 업무 필드에 종속되지 않는 동적 결과 테이블을 제공한다.
 *  - 대량 결과는 resultReference를 이용하여 전체조회 및 CSV 다운로드를 제공한다.
 */

import { useState } from 'react'
import type { FormEventHandler } from 'react'
import ReactMarkdown from 'react-markdown'
import '../assets/css/Chat.css'

type CsrfResponse = {
    token: string
    headerName: string
    parameterName: string
}

type ChatResponse = {
    answer: string
    data: Record<string, unknown> | null
    totalCount: number | null
    returnedCount: number | null
    hasMore: boolean
    resultReference: string | null
    downloadAvailable: boolean
}

type DatabaseResultReferenceResponse = {
    referenceId: string
    data: Record<string, unknown>
    metadata: {
        returnedCount: number
        totalCount: number | null
    }
    createdAt: string
}

type TableRow = Record<string, unknown>

function Chat() {
    const [question, setQuestion] = useState('')
    const [answer, setAnswer] = useState('')
    const [resultData, setResultData] =
        useState<Record<string, unknown> | null>(null)
    const [resultReference, setResultReference] =
        useState<string | null>(null)

    const [downloadAvailable, setDownloadAvailable] = useState(false)
    const [loading, setLoading] = useState(false)
    const [resultLoading, setResultLoading] = useState(false)
    const [errorMessage, setErrorMessage] = useState('')

    /*
     * 공통 AI 계층이 업무 필드명을 알지 않도록
     * data 내부의 객체 배열을 동적으로 찾는다.
     */
    const findTableRows = (
        data: Record<string, unknown> | null
    ): TableRow[] => {
        if (!data) {
            return []
        }

        for (const value of Object.values(data)) {
            if (!Array.isArray(value)) {
                continue
            }

            const isTableRows = value.every(
                item =>
                    item !== null
                    && typeof item === 'object'
                    && !Array.isArray(item)
            )

            if (isTableRows) {
                return value as TableRow[]
            }
        }

        return []
    }

    /*
     * 모든 행에 존재하는 key를 모아
     * 동적으로 테이블 컬럼을 구성한다.
     */
    const findTableColumns = (rows: TableRow[]): string[] => {
        const columns = new Set<string>()

        for (const row of rows) {
            Object.keys(row).forEach(key => columns.add(key))
        }

        return Array.from(columns)
    }

    /*
     * React 화면에 안전하게 표시할 수 있는
     * 단순 문자열 값으로 변환한다.
     */
    const formatCellValue = (value: unknown): string => {
        if (value === null || value === undefined) {
            return ''
        }

        if (
            typeof value === 'string'
            || typeof value === 'number'
            || typeof value === 'boolean'
        ) {
            return String(value)
        }

        return '[구조화 데이터]'
    }

    const tableRows = findTableRows(resultData)
    const tableColumns = findTableColumns(tableRows)

    const handleLogout = async () => {
        try {
            const csrfResponse = await fetch('/api/auth/csrf', {
                method: 'GET',
                credentials: 'include'
            })

            if (!csrfResponse.ok) {
                throw new Error('CSRF 토큰을 발급받지 못했습니다.')
            }

            const csrf: CsrfResponse = await csrfResponse.json()

            const logoutResponse = await fetch('/api/auth/logout', {
                method: 'POST',
                headers: {
                    [csrf.headerName]: csrf.token
                },
                credentials: 'include'
            })

            if (!logoutResponse.ok) {
                throw new Error(
                    `로그아웃 요청 실패: ${logoutResponse.status}`
                )
            }

            window.location.reload()
        } catch (error) {
            console.error('로그아웃 실패', error)
        }
    }

    /*
     * 대량 결과의 전체 데이터를 사용자가 명시적으로 요청했을 때만 조회한다.
     */
    const handleViewFullResult = async () => {
        if (!resultReference || resultLoading) {
            return
        }

        setResultLoading(true)
        setErrorMessage('')

        try {
            const url =
                `/api/database/results/${encodeURIComponent(resultReference)}`

            const response = await fetch(url, {
                method: 'GET',
                credentials: 'include',
                headers: {
                    Accept: 'application/json'
                }
            })

            if (!response.ok) {
                throw new Error(
                    `전체 결과 조회 실패: ${response.status}`
                )
            }

            const result: DatabaseResultReferenceResponse =
                await response.json()

            setResultData(result.data)
        } catch (error) {
            console.error('전체 결과 조회 실패', error)
            setErrorMessage('전체 결과를 가져오지 못했습니다.')
        } finally {
            setResultLoading(false)
        }
    }

    /*
     * 인증 Session을 유지한 상태로 CSV를 조회하고
     * 브라우저 파일 다운로드로 연결한다.
     */
    const handleDownload = async () => {
        if (!resultReference || !downloadAvailable) {
            return
        }

        setErrorMessage('')

        try {
            const reference =
                encodeURIComponent(resultReference)

            const url =
                `/api/database/results/${reference}/download`

            const response = await fetch(url, {
                method: 'GET',
                credentials: 'include',
                headers: {
                    Accept: 'text/csv'
                }
            })

            if (!response.ok) {
                throw new Error(
                    `CSV 다운로드 실패: ${response.status}`
                )
            }

            const blob = await response.blob()
            const downloadUrl = window.URL.createObjectURL(blob)
            const link = document.createElement('a')

            link.href = downloadUrl
            link.download = 'database-result.csv'

            document.body.appendChild(link)
            link.click()
            link.remove()

            window.URL.revokeObjectURL(downloadUrl)
        } catch (error) {
            console.error('CSV 다운로드 실패', error)
            setErrorMessage('결과 파일을 다운로드하지 못했습니다.')
        }
    }

    const handleSubmit: FormEventHandler<HTMLFormElement> = async event => {
        event.preventDefault()

        const trimmedQuestion = question.trim()

        if (!trimmedQuestion || loading) {
            return
        }

        setLoading(true)
        setErrorMessage('')
        setResultData(null)
        setResultReference(null)
        setDownloadAvailable(false)

        try {
            const csrfResponse = await fetch('/api/auth/csrf', {
                method: 'GET',
                credentials: 'include'
            })

            if (!csrfResponse.ok) {
                throw new Error('CSRF 토큰을 발급받지 못했습니다.')
            }

            const csrf: CsrfResponse = await csrfResponse.json()

            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrf.headerName]: csrf.token
                },
                credentials: 'include',
                body: JSON.stringify({
                    question: trimmedQuestion
                })
            })

            if (!response.ok) {
                throw new Error(
                    `채팅 요청 실패: ${response.status}`
                )
            }

            const result: ChatResponse = await response.json()

            setAnswer(result.answer)
            setResultData(result.data)
            setResultReference(result.resultReference)
            setDownloadAvailable(result.downloadAvailable)
            setQuestion('')
        } catch (error) {
            console.error('채팅 요청 실패', error)
            setErrorMessage('답변을 가져오지 못했습니다.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <main className="chat-page">
            <header className="chat-header">
                <div className="chat-brand">
                    <div className="chat-logo">AI</div>

                    <div>
                        <h1>Enterprise AI</h1>
                        <p>Secure Enterprise Assistant</p>
                    </div>
                </div>

                <div className="chat-user-area">
                    <span className="chat-user-status">
                        ● 로그인됨
                    </span>

                    <button
                        type="button"
                        className="chat-logout-button"
                        onClick={handleLogout}
                    >
                        로그아웃
                    </button>
                </div>
            </header>

            <section className="chat-container">
                <div className="chat-welcome">
                    <h2>무엇을 도와드릴까요?</h2>
                    <p>
                        사내 문서와 업무 데이터를 기반으로
                        질문에 답변합니다.
                    </p>
                </div>

                <div className="chat-messages">
                    {!answer && !loading && (
                        <div className="assistant-message">
                            <div className="message-avatar">AI</div>

                            <div className="message-content">
                                <strong>Enterprise AI</strong>
                                <p>
                                    안녕하세요. 업무와 관련된 질문을
                                    입력해주세요.
                                </p>
                            </div>
                        </div>
                    )}

                    {loading && (
                        <div className="assistant-message">
                            <div className="message-avatar">AI</div>

                            <div className="message-content">
                                <p>답변을 생성하고 있습니다...</p>
                            </div>
                        </div>
                    )}

                    {answer && (
                        <div className="assistant-message">
                            <div className="message-avatar">AI</div>

                            <div className="message-content">
                                <strong>Enterprise AI</strong>

                                <div className="message-markdown">
                                    <ReactMarkdown>{answer}</ReactMarkdown>
                                </div>

                                {resultReference && (
                                    <div className="result-actions">
                                        <button
                                            type="button"
                                            onClick={handleViewFullResult}
                                            disabled={resultLoading}
                                        >
                                            {resultLoading
                                                ? '전체 결과 조회 중...'
                                                : '전체 결과 보기'}
                                        </button>

                                        {downloadAvailable && (
                                            <button
                                                type="button"
                                                onClick={handleDownload}
                                            >
                                                CSV 다운로드
                                            </button>
                                        )}
                                    </div>
                                )}

                                {tableRows.length > 0
                                    && tableColumns.length > 0 && (
                                        <div className="result-table-wrapper">
                                            <table className="result-table">
                                                <thead>
                                                <tr>
                                                    {tableColumns.map(column => (
                                                        <th key={column}>
                                                            {column}
                                                        </th>
                                                    ))}
                                                </tr>
                                                </thead>

                                                <tbody>
                                                {tableRows.map(
                                                    (row, rowIndex) => (
                                                        <tr key={rowIndex}>
                                                            {tableColumns.map(
                                                                column => (
                                                                    <td key={column}>
                                                                        {formatCellValue(
                                                                            row[column]
                                                                        )}
                                                                    </td>
                                                                )
                                                            )}
                                                        </tr>
                                                    )
                                                )}
                                                </tbody>
                                            </table>
                                        </div>
                                    )}
                            </div>
                        </div>
                    )}

                    {errorMessage && (
                        <div className="message-content">
                            <p>{errorMessage}</p>
                        </div>
                    )}
                </div>

                <form
                    className="chat-input-area"
                    onSubmit={handleSubmit}
                >
                    <textarea
                        className="chat-input"
                        value={question}
                        onChange={event => setQuestion(event.target.value)}
                        placeholder="질문을 입력하세요."
                        rows={3}
                        disabled={loading}
                    />

                    <div className="chat-input-footer">
                        <span className="chat-input-guide">
                            업무와 관련된 질문을 입력하세요.
                        </span>

                        <button
                            type="submit"
                            className="chat-send-button"
                            disabled={!question.trim() || loading}
                        >
                            {loading ? '답변 생성 중...' : '질문하기'}
                        </button>
                    </div>
                </form>
            </section>
        </main>
    )
}

export default Chat