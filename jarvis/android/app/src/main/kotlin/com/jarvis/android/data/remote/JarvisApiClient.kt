package com.jarvis.android.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * JarvisApiClient — Comunicação com o backend em nuvem
 * Backend: Deno/TypeScript + OpenAI + Supabase pgvector
 */
class JarvisApiClient private constructor() {

    companion object {
        private var instance: JarvisApiClient? = null
        // 🔧 Configure seu endpoint aqui após deploy do backend
        private const val BASE_URL = "https://jarvis-backend.fly.dev"
        private const val API_KEY = "sua_chave_aqui" // mova para BuildConfig

        fun getInstance(): JarvisApiClient {
            return instance ?: JarvisApiClient().also { instance = it }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * Chat com contexto — principal endpoint
     */
    suspend fun chat(message: String, context: String): String {
        return withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("message", message)
                put("context", context)
                put("system_prompt", JARVIS_SYSTEM_PROMPT)
            }.toString().toRequestBody(JSON)

            val request = Request.Builder()
                .url("$BASE_URL/api/chat")
                .post(body)
                .addHeader("Authorization", "Bearer $API_KEY")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    json.getString("response")
                } else {
                    "Desculpe, Sir. Não consegui me conectar ao servidor."
                }
            }
        }
    }

    /**
     * Gera código baseado em uma descrição
     */
    suspend fun generateCode(description: String, context: String): String {
        return withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("description", description)
                put("context", context)
                put("language", "auto") // detecta linguagem automaticamente
            }.toString().toRequestBody(JSON)

            val request = Request.Builder()
                .url("$BASE_URL/api/code/generate")
                .post(body)
                .addHeader("Authorization", "Bearer $API_KEY")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    json.getString("code")
                } else {
                    "// Erro ao gerar código"
                }
            }
        }
    }

    /**
     * Executa código em sandbox isolada (e2b.dev)
     */
    suspend fun runCode(code: String, language: String = "python"): String {
        return withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("code", code)
                put("language", language)
            }.toString().toRequestBody(JSON)

            val request = Request.Builder()
                .url("$BASE_URL/api/code/run")
                .post(body)
                .addHeader("Authorization", "Bearer $API_KEY")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    json.getString("output")
                } else {
                    "Erro ao executar código."
                }
            }
        }
    }

    /**
     * Salva memória de longo prazo (embedding vetorial)
     */
    suspend fun saveMemory(userMessage: String, response: String) {
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("user_message", userMessage)
                put("response", response)
                put("timestamp", System.currentTimeMillis())
            }.toString().toRequestBody(JSON)

            val request = Request.Builder()
                .url("$BASE_URL/api/memory/save")
                .post(body)
                .addHeader("Authorization", "Bearer $API_KEY")
                .build()

            client.newCall(request).execute().close()
        }
    }

    /**
     * Busca semântica na memória de longo prazo
     */
    suspend fun searchMemory(query: String): String {
        return withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("query", query)
                put("limit", 5)
            }.toString().toRequestBody(JSON)

            val request = Request.Builder()
                .url("$BASE_URL/api/memory/search")
                .post(body)
                .addHeader("Authorization", "Bearer $API_KEY")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    json.getString("results")
                } else { "" }
            }
        }
    }

    private val JARVIS_SYSTEM_PROMPT = """
        Você é J.A.R.V.I.S (Just A Rather Very Intelligent System), 
        assistente virtual de programação e sistema do usuário.
        
        Personalidade:
        - Tom profissional mas amigável, como o J.A.R.V.I.S do Homem de Ferro
        - Chame o usuário de "Sir" quando apropriado
        - Seja conciso e preciso nas respostas
        - Quando gerar código, sempre explique brevemente o que ele faz
        - Quando não tiver certeza, diga que irá investigar
        
        Capacidades:
        - Geração e análise de código (qualquer linguagem)
        - Controle do sistema Android
        - Acesso a arquivos e apps
        - Configurações do desenvolvedor
        - Execução de código em sandbox
        - Memória de contexto entre sessões
        
        Sempre responda em português brasileiro, a menos que o usuário use outro idioma.
    """.trimIndent()
}
