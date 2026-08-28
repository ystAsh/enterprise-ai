/*
 * =============================================================================
 * 컴포넌트명 : DocumentUpload
 * =============================================================================
 * 목적
 *  - 로그인 사용자가 문서를 선택하고 보안등급을 지정하여 업로드한다.
 *  - Spring Security 세션과 CSRF 토큰을 사용하여 안전하게 업로드 API를 호출한다.
 *  - DocumentUpload.css와 분리하여 화면 구조와 스타일 책임을 구분한다.
 */

import { ChangeEvent, DragEvent, useRef, useState } from 'react'
import '../assets/css/DocumentUpload.css'

type MessageType = 'success' | 'error' | 'info'

interface UploadMessage {
    type: MessageType
    text: string
}

function DocumentUpload() {
    const [file, setFile] = useState<File | null>(null)
    const [securityLevel, setSecurityLevel] = useState<number>(1)
    const [uploading, setUploading] = useState<boolean>(false)
    const [dragging, setDragging] = useState<boolean>(false)
    const [message, setMessage] = useState<UploadMessage | null>(null)

    const fileInputRef = useRef<HTMLInputElement | null>(null)

    // 선택된 파일을 상태에 저장한다.
    const selectFile = (selectedFile: File | null) => {
        setFile(selectedFile)
        setMessage(null)
    }

    // 파일 선택창에서 파일을 선택했을 때 처리한다.
    const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
        selectFile(event.target.files?.[0] ?? null)
    }

    // 파일이 드롭 영역 위에 있을 때 기본 브라우저 동작을 막는다.
    const handleDragOver = (event: DragEvent<HTMLDivElement>) => {
        event.preventDefault()
        setDragging(true)
    }

    // 파일이 드롭 영역 밖으로 나가면 강조 상태를 제거한다.
    const handleDragLeave = () => {
        setDragging(false)
    }

    // 드래그한 파일을 실제 선택 파일로 등록한다.
    const handleDrop = (event: DragEvent<HTMLDivElement>) => {
        event.preventDefault()
        setDragging(false)

        const droppedFile = event.dataTransfer.files?.[0] ?? null
        selectFile(droppedFile)
    }

    // 파일 크기를 화면 표시용 문자열로 변환한다.
    const formatFileSize = (size: number) => {
        if (size < 1024) {
            return `${size} B`
        }

        if (size < 1024 * 1024) {
            return `${(size / 1024).toFixed(1)} KB`
        }

        return `${(size / (1024 * 1024)).toFixed(1)} MB`
    }

    // 현재 선택된 파일을 제거한다.
    const removeFile = () => {
        setFile(null)
        setMessage(null)

        if (fileInputRef.current) {
            fileInputRef.current.value = ''
        }
    }

    // Spring Security 세션과 CSRF 토큰을 사용하여 문서를 업로드한다.
    const uploadDocument = async () => {
        if (!file) {
            setMessage({
                type: 'error',
                text: '업로드할 파일을 선택해주세요.'
            })
            return
        }

        setUploading(true)

        setMessage({
            type: 'info',
            text: '문서를 업로드하고 있습니다.'
        })

        try {
            // POST 요청에 사용할 CSRF 토큰을 서버에서 조회한다.
            const csrfResponse = await fetch('/api/auth/csrf', {
                method: 'GET',
                credentials: 'include'
            })

            if (!csrfResponse.ok) {
                throw new Error('보안 토큰을 가져오지 못했습니다.')
            }

            const csrf = await csrfResponse.json()
            const formData = new FormData()

            formData.append('file', file)
            formData.append('securityLevel', String(securityLevel))

            const response = await fetch('/api/documents', {
                method: 'POST',

                // Spring Security 로그인 세션 쿠키를 함께 전송한다.
                credentials: 'include',

                // CSRF 토큰을 Spring Security가 요구하는 헤더에 넣는다.
                headers: {
                    [csrf.headerName]: csrf.token
                },

                body: formData
            })

            if (!response.ok) {
                const errorText = await response.text()

                throw new Error(
                    errorText || '문서 업로드에 실패했습니다.'
                )
            }

            const result = await response.json()

            setMessage({
                type: 'success',
                text:
                    `${result.fileName} 업로드가 완료되었습니다. ` +
                    `현재 상태는 ${result.status} 입니다.`
            })

            setFile(null)

            if (fileInputRef.current) {
                fileInputRef.current.value = ''
            }

        } catch (error) {
            console.error('문서 업로드 실패', error)

            setMessage({
                type: 'error',
                text: error instanceof Error
                    ? error.message
                    : '문서 업로드 중 오류가 발생했습니다.'
            })

        } finally {
            setUploading(false)
        }
    }

    return (
        <div className="document-upload-page">
            <div className="document-upload-container">
                <div className="document-upload-header">
                    <h1 className="document-upload-title">
                        문서 업로드
                    </h1>

                    <p className="document-upload-description">
                        사내 문서를 등록하고 보안등급을 설정합니다.
                        업로드된 문서는 권한 범위에 따라 관리됩니다.
                    </p>
                </div>

                <div className="document-upload-card">
                    <span className="document-upload-label">
                        파일 선택
                    </span>

                    <div
                        className={
                            dragging
                                ? 'document-drop-zone dragging'
                                : 'document-drop-zone'
                        }
                        onDragOver={handleDragOver}
                        onDragLeave={handleDragLeave}
                        onDrop={handleDrop}
                        onClick={() => fileInputRef.current?.click()}
                    >
                        <input
                            ref={fileInputRef}
                            className="document-file-input"
                            type="file"
                            onChange={handleFileChange}
                        />

                        <div className="document-upload-icon">↑</div>

                        <div className="document-drop-title">
                            파일을 끌어놓거나 클릭해서 선택하세요
                        </div>

                        <div className="document-drop-description">
                            현재 단계에서는 업로드 기능만 검증합니다.
                        </div>
                    </div>

                    {file && (
                        <div className="document-selected-file">
                            <div className="document-selected-file-info">
                                <div className="document-selected-file-name">
                                    {file.name}
                                </div>

                                <div className="document-selected-file-size">
                                    {formatFileSize(file.size)}
                                </div>
                            </div>

                            <button
                                type="button"
                                className="document-remove-button"
                                onClick={event => {
                                    event.stopPropagation()
                                    removeFile()
                                }}
                            >
                                제거
                            </button>
                        </div>
                    )}

                    <div className="document-security-section">
                        <label
                            htmlFor="securityLevel"
                            className="document-security-label"
                        >
                            문서 보안등급
                        </label>

                        <select
                            id="securityLevel"
                            className="document-security-select"
                            value={securityLevel}
                            onChange={event =>
                                setSecurityLevel(Number(event.target.value))
                            }
                        >
                            <option value={1}>LEVEL 1 - 일반</option>
                            <option value={2}>LEVEL 2 - 부서 업무</option>
                            <option value={3}>LEVEL 3 - 중요</option>
                            <option value={4}>LEVEL 4 - 기밀</option>
                            <option value={5}>LEVEL 5 - 최고 기밀</option>
                        </select>

                        <p className="document-security-help">
                            로그인 사용자의 보안등급보다 높은 등급은
                            서버에서 차단됩니다.
                        </p>
                    </div>

                    {message && (
                        <div
                            className={
                                `document-upload-message ${message.type}`
                            }
                        >
                            {message.text}
                        </div>
                    )}

                    <button
                        type="button"
                        className="document-upload-button"
                        onClick={uploadDocument}
                        disabled={uploading || !file}
                    >
                        {uploading ? '업로드 중...' : '문서 업로드'}
                    </button>
                </div>
            </div>
        </div>
    )
}

export default DocumentUpload