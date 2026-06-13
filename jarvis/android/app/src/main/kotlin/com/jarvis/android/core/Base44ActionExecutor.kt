package com.jarvis.android.core

import com.jarvis.android.data.remote.Base44ApiClient
import org.json.JSONObject

/**
 * Base44ActionExecutor
 *
 * Executa ações Base44 reais a partir do JSON retornado pelo LLM.
 * Cada action mapeia para uma chamada real à API Base44.
 */
class Base44ActionExecutor(
    private val base44Api: Base44ApiClient
) {

    suspend fun execute(action: String, params: JSONObject): ActionResult {
        return try {
            when (action) {

                // ── Apps ──────────────────────────────────────────────────
                "create_app" -> base44Api.createApp(
                    name = params.getString("name"),
                    description = params.optString("description", "")
                )

                "edit_app" -> base44Api.editApp(
                    appId = params.getString("app_id"),
                    message = params.getString("message")
                )

                // ── Entidades (banco de dados) ─────────────────────────
                "create_entity" -> base44Api.createEntity(
                    entityName = params.getString("entity_name"),
                    schema = params.getJSONObject("schema")
                )

                "update_entity" -> base44Api.updateEntity(
                    entityName = params.getString("entity_name"),
                    schema = params.getJSONObject("schema")
                )

                "read_entities" -> base44Api.readEntities(
                    entityName = params.getString("entity_name"),
                    query = params.optJSONObject("query"),
                    limit = params.optInt("limit", 50)
                )

                "create_record" -> base44Api.createRecord(
                    entityName = params.getString("entity_name"),
                    data = params.getJSONObject("data")
                )

                "update_record" -> base44Api.updateRecord(
                    entityName = params.getString("entity_name"),
                    query = params.getJSONObject("query"),
                    data = params.getJSONObject("data")
                )

                "delete_records" -> base44Api.deleteRecords(
                    entityName = params.getString("entity_name"),
                    query = params.getJSONObject("query")
                )

                // ── Automações ────────────────────────────────────────
                "create_automation" -> base44Api.createAutomation(
                    name = params.getString("name"),
                    type = params.getString("type"), // scheduled | entity | connector
                    taskName = params.getString("task_name"),
                    description = params.getString("description"),
                    schedule = params.optJSONObject("schedule"),
                    entityConfig = params.optJSONObject("entity_config"),
                    connectorConfig = params.optJSONObject("connector_config")
                )

                // ── Funções Backend ───────────────────────────────────
                "deploy_function" -> base44Api.deployFunction(
                    functionName = params.getString("function_name"),
                    code = params.getString("code")
                )

                "call_function" -> base44Api.callFunction(
                    functionName = params.getString("function_name"),
                    payload = params.optJSONObject("payload") ?: JSONObject()
                )

                // ── Integrações OAuth ─────────────────────────────────
                "connect_oauth" -> base44Api.connectOAuth(
                    integrationType = params.getString("integration_type"),
                    reason = params.getString("reason"),
                    scopes = params.optJSONArray("scopes")
                )

                // ── Gmail ─────────────────────────────────────────────
                "send_email" -> base44Api.gmailSend(
                    to = params.getString("to"),
                    subject = params.getString("subject"),
                    body = params.getString("body"),
                    isHtml = params.optBoolean("html", false)
                )

                "read_emails" -> base44Api.gmailRead(
                    query = params.optString("query", ""),
                    maxResults = params.optInt("max_results", 10)
                )

                // ── Google Calendar ───────────────────────────────────
                "create_event" -> base44Api.calendarCreateEvent(
                    title = params.getString("title"),
                    start = params.getString("start"),
                    end = params.getString("end"),
                    description = params.optString("description", "")
                )

                "list_events" -> base44Api.calendarListEvents(
                    timeMin = params.optString("time_min", ""),
                    timeMax = params.optString("time_max", "")
                )

                // ── Google Sheets ─────────────────────────────────────
                "sheets_read" -> base44Api.sheetsRead(
                    spreadsheetId = params.getString("spreadsheet_id"),
                    range = params.getString("range")
                )

                "sheets_write" -> base44Api.sheetsWrite(
                    spreadsheetId = params.getString("spreadsheet_id"),
                    range = params.getString("range"),
                    values = params.getJSONArray("values")
                )

                // ── GitHub ────────────────────────────────────────────
                "github_create_repo" -> base44Api.githubCreateRepo(
                    name = params.getString("name"),
                    description = params.optString("description", ""),
                    isPrivate = params.optBoolean("private", false)
                )

                "github_push" -> base44Api.githubPush(
                    repo = params.getString("repo"),
                    path = params.getString("path"),
                    content = params.getString("content"),
                    message = params.getString("commit_message")
                )

                // ── Slack ─────────────────────────────────────────────
                "slack_send" -> base44Api.slackSend(
                    channel = params.getString("channel"),
                    message = params.getString("message")
                )

                // ── Notion ────────────────────────────────────────────
                "notion_create_page" -> base44Api.notionCreatePage(
                    title = params.getString("title"),
                    content = params.optString("content", ""),
                    parentId = params.optString("parent_id", "")
                )

                // ── Arquivos ──────────────────────────────────────────
                "upload_file" -> base44Api.uploadFile(
                    filePath = params.getString("file_path"),
                    isPrivate = params.optBoolean("private", false)
                )

                "write_file" -> base44Api.writeLocalFile(
                    path = params.getString("path"),
                    content = params.getString("content")
                )

                "read_file" -> base44Api.readLocalFile(
                    path = params.getString("path")
                )

                // ── Web Search ────────────────────────────────────────
                "web_search" -> base44Api.webSearch(
                    query = params.getString("query"),
                    type = params.optString("type", "google_search")
                )

                // ── Geração de Imagem ─────────────────────────────────
                "generate_image" -> base44Api.generateImage(
                    prompt = params.getString("prompt")
                )

                // ── Código ────────────────────────────────────────────
                "generate_code" -> base44Api.generateCode(
                    description = params.getString("description"),
                    language = params.optString("language", "auto"),
                    context = params.optString("context", "")
                )

                "run_code" -> base44Api.runCode(
                    code = params.getString("code"),
                    language = params.optString("language", "python")
                )

                else -> ActionResult(
                    success = false,
                    message = "Ação desconhecida: $action"
                )
            }
        } catch (e: Exception) {
            ActionResult(success = false, message = "Erro: ${e.message}")
        }
    }
}

data class ActionResult(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)
