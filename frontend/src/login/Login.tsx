/*
 * =============================================================================
 * 컴포넌트명 : Login
 * =============================================================================
 * 목적
 *  - Enterprise AI의 로그인 화면을 제공한다.
 *  - Spring Security CSRF 토큰을 발급받아 안전하게 로그인 요청을 전송한다.
 *  - 로그인 성공 시 상위 App 컴포넌트에 성공 사실을 전달한다.
 */

import { useState } from 'react'
import type { FormEventHandler } from 'react'
import '../assets/css/login/Login.css'

// Spring Security가 반환하는 CSRF 정보의 타입이다.
type CsrfResponse = {
    token: string
    headerName: string
    parameterName: string
}

// Login 컴포넌트가 상위 컴포넌트에서 받을 기능을 정의한다.
type LoginProps = {
    onLoginSuccess: () => void
}

function Login({
                   onLoginSuccess
               }: LoginProps) {

    // 사용자가 입력한 로그인 아이디를 관리한다.
    const [username, setUsername] = useState<string>('')

    // 사용자가 입력한 비밀번호를 관리한다.
    const [password, setPassword] = useState<string>('')

    // 로그인 실패 메시지를 관리한다.
    const [errorMessage, setErrorMessage] = useState<string>('')

    // 로그인 요청 중 버튼 중복 클릭을 방지한다.
    const [loading, setLoading] = useState<boolean>(false)

    // 로그인 Form 제출 시 Spring Security 인증을 요청한다.
    const handleLogin: FormEventHandler<HTMLFormElement> = async (event) => {

        event.preventDefault()

        setErrorMessage('')
        setLoading(true)

        try {

            // 로그인 요청 전에 CSRF 토큰을 발급받는다.
            const csrfResponse =
                await fetch('/api/auth/csrf', {
                    method: 'GET',
                    credentials: 'include'
                })

            if (!csrfResponse.ok) {
                throw new Error(
                    '보안 토큰을 발급받지 못했습니다.'
                )
            }

            const csrf: CsrfResponse =
                await csrfResponse.json()

            // 사용자 아이디와 비밀번호만 서버로 전달한다.
            const loginResponse =
                await fetch('/api/auth/login', {
                    method: 'POST',

                    headers: {
                        'Content-Type': 'application/json',
                        [csrf.headerName]: csrf.token
                    },

                    credentials: 'include',

                    body: JSON.stringify({
                        username,
                        password
                    })
                })

            if (!loginResponse.ok) {
                setErrorMessage(
                    '아이디 또는 비밀번호를 확인해주세요.'
                )
                return
            }

            // 로그인 성공 후 비밀번호 값을 React 상태에서 제거한다.
            setPassword('')

            // 상위 App 컴포넌트에 로그인 성공 사실을 전달한다.
            onLoginSuccess()

        } catch (error) {

            console.error(
                '로그인 요청 실패',
                error
            )

            setErrorMessage(
                '로그인 처리 중 오류가 발생했습니다.'
            )

        } finally {
            setLoading(false)
        }
    }

    return (
        <main className="login-page">

            <section className="login-box">

                <h1>Enterprise AI</h1>

                <p className="login-description">
                    사내 AI 시스템
                </p>

                {errorMessage && (
                    <div className="login-error">
                        {errorMessage}
                    </div>
                )}

                <form onSubmit={handleLogin}>

                    <div className="form-group">

                        <label htmlFor="username" className="login-label">
                            아이디
                        </label>

                        <input
                            id="username"
                            name="username"
                            type="text"
                            value={username}
                            onChange={(event) =>
                                setUsername(event.target.value)
                            }
                            autoComplete="username"
                            required
                        />

                    </div>

                    <div className="form-group">

                        <label htmlFor="password" className="login-label">
                            비밀번호
                        </label>

                        <input
                            id="password"
                            name="password"
                            type="password"
                            value={password}
                            onChange={(event) =>
                                setPassword(event.target.value)
                            }
                            autoComplete="current-password"
                            required
                        />

                    </div>

                    <button
                        type="submit"
                        className="login-button"
                        disabled={loading}
                    >
                        {loading
                            ? '로그인 중...'
                            : '로그인'}
                    </button>

                </form>

            </section>

        </main>
    )
}

export default Login