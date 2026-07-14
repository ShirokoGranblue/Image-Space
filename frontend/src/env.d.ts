/// <reference types="vite/client" />

declare module '*.vue?raw' {
  const source: string
  export default source
}

declare global {
  interface Window {
    __turnstileLoading?: Promise<void>
  }
}

export {}
