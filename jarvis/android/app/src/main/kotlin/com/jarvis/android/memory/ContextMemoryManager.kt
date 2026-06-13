package com.jarvis.android.memory

import com.jarvis.android.data.local.JarvisDatabase
import com.jarvis.android.data.remote.JarvisApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ContextMemoryManager — Sistema de memória do J.A.R.V.I.S
 *
 * Dois níveis de memória:
 * 1. Memória de curto prazo: últimas N mensagens (in-memory + Room DB local)
 * 2. Memória de longo prazo: embeddings vetoriais no backend em nuvem
 *    (busca semântica — acha contexto relevante mesmo de meses atrás)
 */
class ContextMemoryManager(
    private val database: JarvisDatabase,
    private val apiClient: JarvisApiClient
) {
    private val shortTermBuffer = ArrayDeque<MemoryEntry>(maxSize = 20)
    private val contextStore = mutableMapOf<String, String>()

    suspend fun initialize() {
        // Carrega últimas interações do banco local
        withContext(Dispatchers.IO) {
            val recent = database.memoryDao().getRecent(20)
            recent.forEach { shortTermBuffer.addLast(it.toMemoryEntry()) }
        }
    }

    /**
     * Busca contexto relevante para uma mensagem
     * Combina memória curta + busca vetorial na nuvem
     */
    suspend fun getRelevantContext(query: String): String {
        return withContext(Dispatchers.IO) {
            val shortTerm = shortTermBuffer.takeLast(8).joinToString("\n") {
                "User: ${it.userMessage}\nJARVIS: ${it.response}"
            }

            // Busca semântica no backend
            val longTerm = try {
                apiClient.searchMemory(query)
            } catch (e: Exception) { "" }

            """
            === Histórico Recente ===
            $shortTerm
            
            === Contexto Relevante (Longo Prazo) ===
            $longTerm
            
            === Estado Atual ===
            App em uso: ${contextStore["current_app"] ?: "Desconhecido"}
            """.trimIndent()
        }
    }

    /**
     * Salva uma interação na memória (curto e longo prazo)
     */
    suspend fun saveInteraction(userMessage: String, response: String) {
        withContext(Dispatchers.IO) {
            val entry = MemoryEntry(
                userMessage = userMessage,
                response = response,
                timestamp = System.currentTimeMillis()
            )

            // Curto prazo
            shortTermBuffer.addLast(entry)
            database.memoryDao().insert(entry.toEntity())

            // Longo prazo (envia ao backend para gerar embedding)
            try {
                apiClient.saveMemory(userMessage, response)
            } catch (e: Exception) {
                // Falha silenciosa — memória local ainda funciona
            }
        }
    }

    fun saveContext(key: String, value: String) {
        contextStore[key] = value
    }

    private fun <T> ArrayDeque<T>.addLast(element: T) {
        if (size >= 20) removeFirst()
        addLast(element)
    }
}

data class MemoryEntry(
    val userMessage: String,
    val response: String,
    val timestamp: Long,
    val id: Long = 0
)
