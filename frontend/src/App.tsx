/*
 * =============================================================================
 * 컴포넌트명 : App
 * =============================================================================
 * 목적
 *  - React 애플리케이션의 최상위 컴포넌트이다.
 *  - Spring Security 세션을 확인하여 Login 또는 인증 화면을 표시한다.
 *  - 로그인 후 Chat과 Document Upload 화면을 전환한다.
 *  - 브라우저 새로고침 후에도 실제 서버 세션을 기준으로 로그인 상태를 복구한다.
 */

import { useEffect, useState } from 'react'
import Login from './login/Login'
import Chat from './chat/Chat'
import DocumentUpload from './document/DocumentUpload'

type ViewType = 'CHAT' | 'DOCUMENT_UPLOAD'

function App() {
    // Spring Security 세션의 실제 로그인 상태를 관리한다.
    const [authenticated, setAuthenticated] =
        useState<boolean>(false)

    // 최초 세션 확인이 끝났는지 관리한다.
    const [checkingSession, setCheckingSession] =
        useState<boolean>(true)

    // 로그인 후 표시할 현재 화면을 관리한다.
    const [currentView, setCurrentView] =
        useState<ViewType>('CHAT')

    // React가 처음 실행될 때 Spring Security 세션을 확인한다.
    useEffect(() => {

        const checkSession = async () => {

            try {
                const response =
                    await fetch('/api/auth/me', {
                        method: 'GET',
                        credentials: 'include'
                    })

                // /api/auth/me가 200이면 실제 로그인 세션이 존재한다.
                setAuthenticated(response.ok)

            } catch (error) {

                console.error(
                    '로그인 세션 확인 실패',
                    error
                )

                setAuthenticated(false)

            } finally {
                // 세션 확인이 끝난 후 실제 화면을 표시한다.
                setCheckingSession(false)
            }
        }

        checkSession()

    }, [])

    // 서버 세션 확인 전에는 Login 화면을 먼저 보여주지 않는다.
    if (checkingSession) {
        return (
            <div>
                로그인 상태 확인 중...
            </div>
        )
    }

    // 인증되지 않은 경우 로그인 화면을 표시한다.
    if (!authenticated) {
        return (
            <Login
                onLoginSuccess={() => {
                    setAuthenticated(true)
                    setCurrentView('CHAT')
                }}
            />
        )
    }

    return (
        <div>
            <nav>
                <button
                    type="button"
                    onClick={() =>
                        setCurrentView('CHAT')
                    }
                    disabled={currentView === 'CHAT'}
                >
                    채팅
                </button>

                <button
                    type="button"
                    onClick={() =>
                        setCurrentView('DOCUMENT_UPLOAD')
                    }
                    disabled={
                        currentView === 'DOCUMENT_UPLOAD'
                    }
                >
                    문서 업로드
                </button>
            </nav>

            {currentView === 'CHAT' && (
                <Chat />
            )}

            {currentView === 'DOCUMENT_UPLOAD' && (
                <DocumentUpload />
            )}
        </div>
    )
}

export default App