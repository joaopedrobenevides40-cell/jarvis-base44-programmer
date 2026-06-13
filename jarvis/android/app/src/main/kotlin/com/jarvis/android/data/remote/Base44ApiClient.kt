package com.jarvis.android.data.remote

import com.jarvis.android.core.ActionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Base44ApiClient
 *
 * Cliente HTTP que conecta o J.A.R.V.I.S ao backend Base44.
 * O backend (main.ts) expõe todos os endpoints que espelham
 * as ferramentas do Superagent Base44:
 *   - manage_entity_schemas
 *   - create/read/update/delete entities
 *   - create_automation
 *   - deploy_backend_function
 *   - request_oauth_authorization
 *   - get_connector_token + chamar APIs OAuth
 *   - generate_image
 *   - upload_file
 *   - web_search
 */
class Base44ApiClient private constructor() {

    companion object {
        private var instance: Base44ApiClient? = null
        // 🔧 Configure após deploy — URL do seu backend
        private const val BASE_URL = "https://jarvis-backend.fly.dev"
        private const val API_KEY  = "sua_chave_aqui" // BuildConfig.JARVIS_API_KEY

        fun getInstance() = instance ?: Base44ApiClient().also { instance = it }
    }

    private val JSON_MT = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // funções pesadas precisam de mais tempo
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun authHeaders() = mapOf("Authorization" to "Bearer $API_KEY")

    private suspend fun post(endpoint: String, body: JSONObject): JSONObject {
        return withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("$BASE_URL$endpoint")
                .post(body.toString().toRequestBody(JSON_MT))
                .apply { authHeaders().forEach { (k, v) -> addHeader(k, v) } }
                .build()
            client.newCall(req).execute().use { res ->
                val text = res.body?.string() ?: "{}"
                if (res.isSuccessful) JSONObject(text)
                else JSONObject().put("error", text).put("status", res.code)
            }
        }
    }

    private suspend fun get(endpoint: String): JSONObject {
        return withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("$BASE_URL$endpoint")
                .get()
                .apply { authHeaders().forEach { (k, v) -> addHeader(k, v) } }
                .build()
            client.newCall(req).execute().use { res ->
                val text = res.body?.string() ?: "{}"
                if (res.isSuccessful) JSONObject(text)
                else JSONObject().put("error", text)
            }
        }
    }

    private fun ok(msg: String, data: Any? = null) = ActionResult(true, msg, data)
    private fun err(msg: String) = ActionResult(false, msg)
    private fun JSONObject.hasError() = has("error")

    // ── Apps Base44 ───────────────────────────────────────────────────────────

    suspend fun createApp(name: String, description: String): ActionResult {
        val r = post("/api/base44/apps/create", JSONObject()
            .put("name", name)
            .put("description", description))
        return if (r.hasError()) err(r.getString("error"))
        else ok("App '$name' criado! ID: ${r.optString("app_id")}", r)
    }

    suspend fun editApp(appId: String, message: String): ActionResult {
        val r = post("/api/base44/apps/edit", JSONObject()
            .put("app_id", appId)
            .put("message", message))
        return if (r.hasError()) err(r.getString("error"))
        else ok("App atualizado com sucesso.", r)
    }

    // ── Entidades ─────────────────────────────────────────────────────────────

    suspend fun createEntity(entityName: String, schema: JSONObject): ActionResult {
        val r = post("/api/base44/entities/schema", JSONObject()
            .put("action", "create")
            .put("entity_name", entityName)
            .put("schema", schema))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Entidade '$entityName' criada com sucesso.", r)
    }

    suspend fun updateEntity(entityName: String, schema: JSONObject): ActionResult {
        val r = post("/api/base44/entities/schema", JSONObject()
            .put("action", "update")
            .put("entity_name", entityName)
            .put("schema", schema))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Entidade '$entityName' atualizada.", r)
    }

    suspend fun readEntities(entityName: String, query: JSONObject?, limit: Int): ActionResult {
        val r = post("/api/base44/entities/read", JSONObject()
            .put("entity_name", entityName)
            .put("query", query ?: JSONObject())
            .put("limit", limit))
        return if (r.hasError()) err(r.getString("error"))
        else ok("${r.optJSONArray("records")?.length() ?: 0} registros encontrados.", r)
    }

    suspend fun createRecord(entityName: String, data: JSONObject): ActionResult {
        val r = post("/api/base44/entities/create", JSONObject()
            .put("entity_name", entityName)
            .put("data", data))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Registro criado. ID: ${r.optString("id")}", r)
    }

    suspend fun updateRecord(entityName: String, query: JSONObject, data: JSONObject): ActionResult {
        val r = post("/api/base44/entities/update", JSONObject()
            .put("entity_name", entityName)
            .put("query", query)
            .put("data", data))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Registros atualizados: ${r.optInt("updated", 0)}", r)
    }

    suspend fun deleteRecords(entityName: String, query: JSONObject): ActionResult {
        val r = post("/api/base44/entities/delete", JSONObject()
            .put("entity_name", entityName)
            .put("query", query))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Registros deletados: ${r.optInt("deleted", 0)}", r)
    }

    // ── Automações ────────────────────────────────────────────────────────────

    suspend fun createAutomation(
        name: String, type: String, taskName: String, description: String,
        schedule: JSONObject?, entityConfig: JSONObject?, connectorConfig: JSONObject?
    ): ActionResult {
        val body = JSONObject()
            .put("name", name)
            .put("automation_type", type)
            .put("task_name", taskName)
            .put("description", description)
        schedule?.let { body.put("schedule", it) }
        entityConfig?.let { body.put("entity_config", it) }
        connectorConfig?.let { body.put("connector_config", it) }

        val r = post("/api/base44/automations/create", body)
        return if (r.hasError()) err(r.getString("error"))
        else ok("Automação '$name' criada com sucesso.", r)
    }

    // ── Funções Backend ───────────────────────────────────────────────────────

    suspend fun deployFunction(functionName: String, code: String): ActionResult {
        val r = post("/api/base44/functions/deploy", JSONObject()
            .put("function_name", functionName)
            .put("code", code))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Função '$functionName' deployada em: ${r.optString("url")}", r)
    }

    suspend fun callFunction(functionName: String, payload: JSONObject): ActionResult {
        val r = post("/api/base44/functions/call", JSONObject()
            .put("function_name", functionName)
            .put("payload", payload))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Função executada.", r)
    }

    // ── OAuth ─────────────────────────────────────────────────────────────────

    suspend fun connectOAuth(integrationType: String, reason: String, scopes: JSONArray?): ActionResult {
        val body = JSONObject()
            .put("integration_type", integrationType)
            .put("reason", reason)
        scopes?.let { body.put("scopes", it) }

        val r = post("/api/base44/oauth/connect", body)
        return if (r.hasError()) err(r.getString("error"))
        else ok("OAuth '$integrationType': ${r.optString("status")}", r)
    }

    // ── Gmail ─────────────────────────────────────────────────────────────────

    suspend fun gmailSend(to: String, subject: String, body: String, isHtml: Boolean): ActionResult {
        val r = post("/api/integrations/gmail/send", JSONObject()
            .put("to", to)
            .put("subject", subject)
            .put("body", body)
            .put("html", isHtml))
        return if (r.hasError()) err(r.getString("error"))
        else ok("E-mail enviado para $to.", r)
    }

    suspend fun gmailRead(query: String, maxResults: Int): ActionResult {
        val r = post("/api/integrations/gmail/read", JSONObject()
            .put("query", query)
            .put("max_results", maxResults))
        return if (r.hasError()) err(r.getString("error"))
        else ok("${r.optJSONArray("messages")?.length() ?: 0} e-mails encontrados.", r)
    }

    // ── Google Calendar ───────────────────────────────────────────────────────

    suspend fun calendarCreateEvent(title: String, start: String, end: String, description: String): ActionResult {
        val r = post("/api/integrations/calendar/create", JSONObject()
            .put("title", title)
            .put("start", start)
            .put("end", end)
            .put("description", description))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Evento '$title' criado.", r)
    }

    suspend fun calendarListEvents(timeMin: String, timeMax: String): ActionResult {
        val r = post("/api/integrations/calendar/list", JSONObject()
            .put("time_min", timeMin)
            .put("time_max", timeMax))
        return if (r.hasError()) err(r.getString("error"))
        else ok("${r.optJSONArray("events")?.length() ?: 0} eventos encontrados.", r)
    }

    // ── Google Sheets ─────────────────────────────────────────────────────────

    suspend fun sheetsRead(spreadsheetId: String, range: String): ActionResult {
        val r = post("/api/integrations/sheets/read", JSONObject()
            .put("spreadsheet_id", spreadsheetId)
            .put("range", range))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Dados lidos da planilha.", r)
    }

    suspend fun sheetsWrite(spreadsheetId: String, range: String, values: JSONArray): ActionResult {
        val r = post("/api/integrations/sheets/write", JSONObject()
            .put("spreadsheet_id", spreadsheetId)
            .put("range", range)
            .put("values", values))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Planilha atualizada.", r)
    }

    // ── GitHub ────────────────────────────────────────────────────────────────

    suspend fun githubCreateRepo(name: String, description: String, isPrivate: Boolean): ActionResult {
        val r = post("/api/integrations/github/create-repo", JSONObject()
            .put("name", name)
            .put("description", description)
            .put("private", isPrivate))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Repositório '$name' criado: ${r.optString("url")}", r)
    }

    suspend fun githubPush(repo: String, path: String, content: String, message: String): ActionResult {
        val r = post("/api/integrations/github/push", JSONObject()
            .put("repo", repo)
            .put("path", path)
            .put("content", content)
            .put("commit_message", message))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Commit feito: $message", r)
    }

    // ── Slack ─────────────────────────────────────────────────────────────────

    suspend fun slackSend(channel: String, message: String): ActionResult {
        val r = post("/api/integrations/slack/send", JSONObject()
            .put("channel", channel)
            .put("message", message))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Mensagem enviada para #$channel", r)
    }

    // ── Notion ────────────────────────────────────────────────────────────────

    suspend fun notionCreatePage(title: String, content: String, parentId: String): ActionResult {
        val r = post("/api/integrations/notion/create", JSONObject()
            .put("title", title)
            .put("content", content)
            .put("parent_id", parentId))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Página '$title' criada no Notion.", r)
    }

    // ── Arquivos ──────────────────────────────────────────────────────────────

    suspend fun uploadFile(filePath: String, isPrivate: Boolean): ActionResult {
        val r = post("/api/files/upload", JSONObject()
            .put("file_path", filePath)
            .put("private", isPrivate))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Arquivo enviado: ${r.optString("url")}", r)
    }

    suspend fun writeLocalFile(path: String, content: String): ActionResult {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            ok("Arquivo salvo em $path")
        } catch (e: Exception) {
            err("Erro ao salvar arquivo: ${e.message}")
        }
    }

    suspend fun readLocalFile(path: String): ActionResult {
        return try {
            val content = File(path).readText()
            ok("Arquivo lido.", content)
        } catch (e: Exception) {
            err("Erro ao ler arquivo: ${e.message}")
        }
    }

    // ── Web Search ────────────────────────────────────────────────────────────

    suspend fun webSearch(query: String, type: String): ActionResult {
        val r = post("/api/tools/search", JSONObject()
            .put("query", query)
            .put("type", type))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Resultados encontrados.", r)
    }

    // ── Geração de Imagem ─────────────────────────────────────────────────────

    suspend fun generateImage(prompt: String): ActionResult {
        val r = post("/api/tools/image", JSONObject().put("prompt", prompt))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Imagem gerada: ${r.optString("url")}", r)
    }

    // ── Código ────────────────────────────────────────────────────────────────

    suspend fun generateCode(description: String, language: String, context: String): ActionResult {
        val r = post("/api/code/generate", JSONObject()
            .put("description", description)
            .put("language", language)
            .put("context", context))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Código gerado.", r.optString("code"))
    }

    suspend fun runCode(code: String, language: String): ActionResult {
        val r = post("/api/code/run", JSONObject()
            .put("code", code)
            .put("language", language))
        return if (r.hasError()) err(r.getString("error"))
        else ok("Código executado.", r.optString("output"))
    }
}
