/*
 * =============================================================================
 * 파일명 : main.tsx
 * =============================================================================
 * 목적
 *  - React 애플리케이션의 시작점이다.
 *  - index.html의 root 영역에 App 컴포넌트를 렌더링한다.
 */

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import './index.css'

// index.html의 root 영역을 찾는다.
const rootElement = document.getElementById('root')

// root가 존재하지 않으면 애플리케이션을 시작하지 않는다.
if (!rootElement) {
    throw new Error('root element를 찾을 수 없습니다.')
}

// React 애플리케이션을 시작한다.
createRoot(rootElement).render(
    <StrictMode>
        <App />
    </StrictMode>
)