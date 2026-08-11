/*
 * =============================================================================
 * 컴포넌트명 : Chat
 * =============================================================================
 * 목적
 *  - 로그인한 사용자에게 Enterprise AI 채팅 화면을 제공한다.
 *  - 사용자 질문을 Spring Boot /api/chat API로 전달한다.
 *  - CSRF 토큰을 포함하여 세션 기반 요청을 안전하게 처리한다.
 *  - Gemini 응답을 채팅 화면에 표시한다.
 */

import { useState } from 'react'
import type { FormEventHandler } from 'react'
import '../assets/css/chat/Chat.css'

// Spring Security가 반환하는 CSRF 정보의 타입이다.
type CsrfResponse = {
    token: string
    headerName: string
    parameterName: string
}

// 채팅 API가 반환하는 응답 형식이다.
type ChatResponse = {
    answer: string
}

function Chat() {

    // 사용자가 입력한 질문을 관리한다.
    const [question, setQuestion] =
        useState<string>('')

    // AI가 반환한 답변을 관리한다.
    const [answer, setAnswer] =
        useState<string>('')

    // 질문 처리 중 상태를 관리한다.
    const [loading, setLoading] =
        useState<boolean>(false)

    // API 호출 오류 메시지를 관리한다.
    const [errorMessage, setErrorMessage] =
        useState<string>('')

    // Spring Security 로그아웃 API를 호출한다.
    const handleLogout = async () => {

        try {

            // 로그아웃 POST 요청 전에 CSRF 토큰을 발급받는다.
            const csrfResponse =
                await fetch('/api/auth/csrf', {
                    method: 'GET',
                    credentials: 'include'
                })

            if (!csrfResponse.ok) {
                throw new Error(
                    'CSRF 토큰을 발급받지 못했습니다.'
                )
            }

            const csrf: CsrfResponse =
                await csrfResponse.json()

            // Spring Security 세션 로그아웃을 요청한다.
            const logoutResponse =
                await fetch('/api/auth/logout', {
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

            // 서버 세션 제거 후 React 로그인 화면으로 이동한다.
            window.location.reload()

        } catch (error) {

            console.error(
                '로그아웃 실패',
                error
            )
        }
    }
    // 사용자의 질문을 Spring Boot 채팅 API로 전달한다.
    const handleSubmit: FormEventHandler<HTMLFormElement> =
        async (event) => {

            event.preventDefault()

            const trimmedQuestion =
                question.trim()

            if (!trimmedQuestion || loading) {
                return
            }

            setLoading(true)
            setErrorMessage('')

            try {

                // POST 요청 전에 Spring Security CSRF 토큰을 발급받는다.
                const csrfResponse =
                    await fetch('/api/auth/csrf', {
                        method: 'GET',
                        credentials: 'include'
                    })

                if (!csrfResponse.ok) {
                    throw new Error(
                        'CSRF 토큰을 발급받지 못했습니다.'
                    )
                }

                const csrf: CsrfResponse =
                    await csrfResponse.json()

                // 로그인 세션과 CSRF 토큰을 포함하여 채팅 API를 호출한다.
                const response =
                    await fetch('/api/chat', {
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

                const result: ChatResponse =
                    await response.json()

                setAnswer(result.answer)
                setQuestion('')

            } catch (error) {

                console.error(
                    '채팅 요청 실패',
                    error
                )

                setErrorMessage(
                    '답변을 가져오지 못했습니다.'
                )

            } finally {
                setLoading(false)
            }
        }

    return (
        <main className="chat-page">

            <header className="chat-header">

                <div className="chat-brand">

                    <div className="chat-logo">
                        AI
                    </div>

                    <div>
                        <h1>Enterprise AI</h1>

                        <p>
                            Secure Enterprise Assistant
                        </p>
                    </div>

                </div>

                <div className="chat-user-area">

                    <span className="chat-user-status">
                        ● 로그인됨
                    </span>
                    &nbsp;&nbsp;
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

                    <h2>
                        무엇을 도와드릴까요?
                    </h2>

                    <p>
                        사내 문서와 업무 데이터를 기반으로
                        질문에 답변합니다.
                    </p>

                </div>

                <div className="chat-messages">

                    {!answer && !loading && (
                        <div className="assistant-message">

                            <div className="message-avatar">
                                AI
                            </div>

                            <div className="message-content">

                                <strong>
                                    Enterprise AI
                                </strong>

                                <p>
                                    안녕하세요.
                                    업무와 관련된 질문을 입력해주세요.
                                </p>

                            </div>

                        </div>
                    )}

                    {loading && (
                        <div className="assistant-message">

                            <div className="message-avatar">
                                AI
                            </div>

                            <div className="message-content">
                                <p>
                                    답변을 생성하고 있습니다...
                                </p>
                            </div>

                        </div>
                    )}

                    {answer && (
                        <div className="assistant-message">

                            <div className="message-avatar">
                                AI
                            </div>

                            <div className="message-content">

                                <strong>
                                    Enterprise AI
                                </strong>

                                <p>
                                    {answer}
                                </p>

                            </div>

                        </div>
                    )}

                    {errorMessage && (
                        <div className="message-content">
                            <p>
                                {errorMessage}
                            </p>
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
                        onChange={(event) =>
                            setQuestion(event.target.value)
                        }
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
                            disabled={
                                question.trim().length === 0
                                || loading
                            }
                        >
                            {loading
                                ? '답변 생성 중...'
                                : '질문하기'}
                        </button>

                    </div>

                </form>

            </section>

        </main>
    )
}

export default Chat