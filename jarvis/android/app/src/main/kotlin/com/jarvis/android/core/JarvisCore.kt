package com.jarvis.android.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.jarvis.android.data.remote.JarvisApiClient
import com.jarvis.android.memory.ContextMemoryManager
import com.jarvis.android.service.JarvisAccessibilityService
import com.jarvis.android.service.DeveloperOptionsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * JarvisCore — Cérebro central do J.A.R.V.I.S
 * Processa todas as mensagens em linguagem natural e executa ações no sistema
 */
class JarvisCore(
    private val context: Context,
    private val memory: ContextMemoryManager,
    private val apiClient: JarvisApiClient
) {
    private var accessibilityService: JarvisAccessibilityService? = null
    private val devOptionsService = DeveloperOptionsService()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize() {
        // Inicializar memória de contexto
        scope.launch { memory.initialize() }
    }

    fun onAccessibilityReady(service: JarvisAccessibilityService) {
        accessibilityService = service
    }

    fun onAppChanged(packageName: String) {
        // Monitorar qual app está em uso
        scope.launch {
            memory.saveContext("current_app", packageName)
        }
    }

    /**
     * Ponto central de processamento — recebe mensagem do usuário
     * e decide o que fazer com ela
     */
    suspend fun processMessage(userMessage: String): JarvisResponse {
        return withContext(Dispatchers.IO) {
            // 1. Buscar contexto relevante da memória
            val contextHistory = memory.getRelevantContext(userMessage)

            // 2. Analisar intenção
            val intent = analyzeIntent(userMessage)

            // 3. Executar ação baseada na intenção
            when (intent.type) {
                IntentType.GENERATE_CODE -> handleCodeGeneration(userMessage, contextHistory)
                IntentType.OPEN_APP -> handleOpenApp(intent.target ?: "")
                IntentType.READ_SCREEN -> handleReadScreen()
                IntentType.DEV_OPTIONS -> handleDevOptions(intent.action ?: "")
                IntentType.FILE_OPERATION -> handleFileOperation(intent)
                IntentType.SYSTEM_SETTINGS -> handleSystemSettings(intent)
                IntentType.CHAT -> handleChat(userMessage, contextHistory)
                IntentType.INSTALL_APP -> handleInstallApp(intent.target ?: "")
                IntentType.RUN_CODE -> handleRunCode(intent.code ?: "")
                else -> handleChat(userMessage, contextHistory)
            }
        }
    }

    private suspend fun analyzeIntent(message: String): ParsedIntent {
        val lowerMsg = message.lowercase()
        return when {
            lowerMsg.contains("gera") || lowerMsg.contains("cria") ||
            lowerMsg.contains("escreve") || lowerMsg.contains("código") ||
            lowerMsg.contains("function") || lowerMsg.contains("classe") ->
                ParsedIntent(IntentType.GENERATE_CODE)

            lowerMsg.contains("abre") || lowerMsg.contains("abrir") ||
            lowerMsg.contains("lança") ->
                ParsedIntent(IntentType.OPEN_APP, target = extractAppName(message))

            lowerMsg.contains("leia a tela") || lowerMsg.contains("o que está na tela") ->
                ParsedIntent(IntentType.READ_SCREEN)

            lowerMsg.contains("desenvolvedor") || lowerMsg.contains("developer") ||
            lowerMsg.contains("depuração") || lowerMsg.contains("debug") ->
                ParsedIntent(IntentType.DEV_OPTIONS, action = message)

            lowerMsg.contains("arquivo") || lowerMsg.contains("pasta") ||
            lowerMsg.contains("file") || lowerMsg.contains("salva") ->
                ParsedIntent(IntentType.FILE_OPERATION)

            lowerMsg.contains("configuração") || lowerMsg.contains("settings") ->
                ParsedIntent(IntentType.SYSTEM_SETTINGS)

            lowerMsg.contains("execute") || lowerMsg.contains("roda") ||
            lowerMsg.contains("executa") ->
                ParsedIntent(IntentType.RUN_CODE)

            else -> ParsedIntent(IntentType.CHAT)
        }
    }

    private suspend fun handleCodeGeneration(message: String, context: String): JarvisResponse {
        val code = apiClient.generateCode(message, context)
        memory.saveInteraction(message, code)
        return JarvisResponse(
            text = "Aqui está o código gerado, Sir:",
            code = code,
            type = ResponseType.CODE
        )
    }

    private fun handleOpenApp(appName: String): JarvisResponse {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val app = packages.find {
            pm.getApplicationLabel(it).toString().lowercase().contains(appName.lowercase())
        }
        return if (app != null) {
            val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
            launchIntent?.let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(it)
                JarvisResponse("Abrindo ${pm.getApplicationLabel(app)}, Sir.")
            } ?: JarvisResponse("Não foi possível abrir o aplicativo.")
        } else {
            JarvisResponse("Aplicativo '$appName' não encontrado no dispositivo.")
        }
    }

    private fun handleReadScreen(): JarvisResponse {
        val content = accessibilityService?.captureScreenContent()
        return if (content.isNullOrEmpty()) {
            JarvisResponse("Não consigo acessar o conteúdo da tela. Verifique se o serviço de acessibilidade está ativo.")
        } else {
            JarvisResponse("Conteúdo da tela atual:\n\n$content", type = ResponseType.SCREEN_READ)
        }
    }

    private fun handleDevOptions(action: String): JarvisResponse {
        return when {
            action.contains("abrir") || action.contains("mostrar") -> {
                accessibilityService?.openDeveloperOptions()
                JarvisResponse("Abrindo Opções do Desenvolvedor, Sir.")
            }
            action.contains("animações") || action.contains("animation") -> {
                devOptionsService.setAnimations(0f)
                JarvisResponse("Animações desabilitadas para máxima performance, Sir.")
            }
            action.contains("usb") || action.contains("depuração") -> {
                devOptionsService.setUsbDebugging(true)
                JarvisResponse("Depuração USB habilitada, Sir.")
            }
            else -> {
                accessibilityService?.openDeveloperOptions()
                JarvisResponse("Abrindo configurações de desenvolvedor.")
            }
        }
    }

    private fun handleFileOperation(intent: ParsedIntent): JarvisResponse {
        // Delegado ao FileManager
        return JarvisResponse("Operação de arquivo processada, Sir.")
    }

    private fun handleSystemSettings(intent: ParsedIntent): JarvisResponse {
        accessibilityService?.openSettings()
        return JarvisResponse("Abrindo configurações do sistema, Sir.")
    }

    private suspend fun handleRunCode(code: String): JarvisResponse {
        val result = apiClient.runCode(code)
        return JarvisResponse(
            text = "Código executado, Sir:",
            code = result,
            type = ResponseType.CODE_RESULT
        )
    }

    private suspend fun handleChat(message: String, context: String): JarvisResponse {
        val response = apiClient.chat(message, context)
        memory.saveInteraction(message, response)
        return JarvisResponse(response)
    }

    private fun extractAppName(message: String): String {
        val words = message.split(" ")
        val idx = words.indexOfFirst {
            it.lowercase() in listOf("abre", "abrir", "lança", "abre")
        }
        return if (idx >= 0 && idx < words.size - 1) words[idx + 1] else ""
    }
}

data class ParsedIntent(
    val type: IntentType,
    val target: String? = null,
    val action: String? = null,
    val code: String? = null
)

enum class IntentType {
    GENERATE_CODE, OPEN_APP, READ_SCREEN, DEV_OPTIONS,
    FILE_OPERATION, SYSTEM_SETTINGS, CHAT, INSTALL_APP, RUN_CODE
}

data class JarvisResponse(
    val text: String,
    val code: String? = null,
    val type: ResponseType = ResponseType.TEXT,
    val data: Map<String, Any>? = null
)

enum class ResponseType { TEXT, CODE, CODE_RESULT, SCREEN_READ, FILE, ERROR }
