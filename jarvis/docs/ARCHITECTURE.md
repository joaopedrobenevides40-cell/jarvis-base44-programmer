# J.A.R.V.I.S — Just A Rather Very Intelligent System
## Arquitetura Completa para Android

### Visão Geral
Assistente virtual de programação em nuvem com overlay fixo no Android,
memória de contexto vetorial, execução de código em sandbox e acesso
profundo ao sistema Android via permissões especiais.

---

## Stack Técnica

### Frontend (Android App)
- Linguagem: Kotlin
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- UI: Jetpack Compose + Material 3
- Overlay: WindowManager (SYSTEM_ALERT_WINDOW)
- Integração sistema: AccessibilityService + DeviceAdminReceiver

### Backend (Cloud)
- Runtime: Deno (TypeScript)
- IA: OpenAI GPT-4o / Gemini Pro
- Memória: Supabase pgvector
- Execução de código: e2b.dev sandboxes
- Arquivos: Google Drive API
- Deploy: Railway / Fly.io

---

## Permissões Android Necessárias
- SYSTEM_ALERT_WINDOW — overlay flutuante
- WRITE_SETTINGS — editar configurações do sistema
- MANAGE_EXTERNAL_STORAGE — sistema de arquivos completo
- PACKAGE_USAGE_STATS — acessar info dos apps
- MODIFY_AUDIO_SETTINGS — controle de áudio
- INSTALL_PACKAGES — instalar APKs
- WRITE_SECURE_SETTINGS — opções de desenvolvedor (root ou ADB)
- ACCESSIBILITY_SERVICE — interagir com outros apps
- FOREGROUND_SERVICE — rodar em background
- RECEIVE_BOOT_COMPLETED — iniciar com o sistema
