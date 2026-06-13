# J.A.R.V.I.S — Guia de Instalação Completo

## Pré-requisitos
- Android Studio Hedgehog (2023.1.1) ou superior
- JDK 17
- Dispositivo Android 8.0+ (API 26+)
- Conta Supabase (grátis)
- Conta Fly.io ou Railway (grátis)
- Chave OpenAI API

---

## PASSO 1 — Backend (Fly.io)

```bash
# Instalar Fly CLI
curl -L https://fly.io/install.sh | sh

# Login
fly auth login

# Criar app
fly apps create jarvis-backend

# Configurar variáveis de ambiente
fly secrets set OPENAI_API_KEY="sk-..."
fly secrets set SUPABASE_URL="https://xxx.supabase.co"
fly secrets set SUPABASE_SERVICE_KEY="eyJ..."
fly secrets set E2B_API_KEY="e2b_..."
fly secrets set JARVIS_API_KEY="sua_chave_secreta_aqui"

# Deploy
cd jarvis/backend
fly deploy
```

---

## PASSO 2 — Banco de Dados (Supabase)

1. Acesse supabase.com → Novo projeto
2. Vá em **SQL Editor**
3. Cole e execute o conteúdo de `supabase_schema.sql`
4. Copie a **URL** e **Service Key** das configurações

---

## PASSO 3 — App Android

1. Abra o Android Studio
2. Importe o projeto `jarvis/android/`
3. No `local.properties`, adicione:
   ```
   JARVIS_API_KEY=sua_chave_secreta_aqui
   JARVIS_BASE_URL=https://jarvis-backend.fly.dev
   ```
4. Build → Run no seu dispositivo

---

## PASSO 4 — Permissões (OBRIGATÓRIO)

Após instalar, conceda via ADB (uma vez só):

```bash
# Conecte o celular via USB com depuração USB habilitada
adb devices

# Permissão de configurações seguras (opções do desenvolvedor)
adb shell pm grant com.jarvis.android android.permission.WRITE_SECURE_SETTINGS

# Permissão de overlay (será solicitada na primeira abertura também)
adb shell appops set com.jarvis.android SYSTEM_ALERT_WINDOW allow

# Permissão de armazenamento
adb shell pm grant com.jarvis.android android.permission.MANAGE_EXTERNAL_STORAGE
```

---

## PASSO 5 — Configuração no App

1. Abra o J.A.R.V.I.S
2. Vá em **Configurações** → **Serviços**
3. Ative o **Serviço de Acessibilidade** nas configurações do Android
4. Ative o **Overlay** quando solicitado
5. (Opcional) Adicione como **Administrador do Dispositivo** para controle avançado

---

## Estrutura de Arquivos

```
jarvis/
├── android/                    ← Projeto Android (Kotlin + Compose)
│   └── app/src/main/kotlin/
│       ├── JarvisApp.kt        ← Application class
│       ├── core/
│       │   └── JarvisCore.kt   ← Cérebro — processa intenções
│       ├── service/
│       │   ├── JarvisOverlayService.kt    ← Overlay fixo na tela
│       │   ├── JarvisAccessibilityService.kt ← Interage com apps
│       │   └── DeveloperOptionsService.kt ← Opções dev
│       ├── memory/
│       │   └── ContextMemoryManager.kt   ← Memória curta + longa
│       ├── data/
│       │   └── remote/JarvisApiClient.kt ← HTTP para backend
│       └── ui/overlay/
│           └── JarvisOverlayUI.kt        ← UI Compose do overlay
├── backend/                    ← Backend Deno (TypeScript)
│   ├── main.ts                 ← API REST + OpenAI + Supabase
│   ├── Dockerfile
│   ├── fly.toml
│   └── supabase_schema.sql     ← Schema pgvector
└── docs/
    ├── ARCHITECTURE.md
    └── SETUP_GUIDE.md          ← Este arquivo
```

---

## Funcionalidades por Tab

| Tab | O que faz |
|-----|-----------|
| 💬 Chat | Linguagem natural, memória de contexto, IA |
| 💻 Código | Editor + geração de código + execução |
| 📁 Arquivos | Navegar, criar, editar arquivos do dispositivo |
| 🔧 Dev | Todas as opções do desenvolvedor com toggle |

---

## Comandos de Voz / Chat (exemplos)

- *"Gera uma função Python que lê um CSV"*
- *"Abre o Chrome"*
- *"O que está na tela agora?"*
- *"Habilita depuração USB"*
- *"Desativa todas as animações"*
- *"Cria um arquivo teste.kt em Downloads"*
- *"Executa esse código: print('Hello')"*

---

## Segurança

- A chave API nunca fica no código — está em `BuildConfig` via variável de ambiente
- Comunicação HTTPS obrigatória em produção
- Execução de código em sandbox isolada (e2b.dev)
- Memória vetorial armazenada com RLS no Supabase
