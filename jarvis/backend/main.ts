/**
 * J.A.R.V.I.S Backend — Deno + Oak
 * Versão Base44 Programmer Edition
 *
 * Este backend espelha TODAS as ferramentas do Superagent Base44:
 *   - Chat com GPT-4o + memória vetorial
 *   - manage_entity_schemas / CRUD de entidades
 *   - create_automation
 *   - deploy_backend_function
 *   - request_oauth_authorization + tokens OAuth
 *   - Gmail, Calendar, Sheets, GitHub, Slack, Notion...
 *   - generate_image
 *   - web_search
 *   - run_code (e2b sandbox)
 *
 * Deploy: fly.io (região gru — São Paulo)
 */

import { Application, Router, Context } from "https://deno.land/x/oak@v13.0.0/mod.ts";
import { oakCors } from "https://deno.land/x/cors@v1.2.2/mod.ts";

const app  = new Application();
const router = new Router();

// ─── Env ─────────────────────────────────────────────────────────────────────
const OPENAI_KEY     = Deno.env.get("OPENAI_API_KEY") ?? "";
const SUPABASE_URL   = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_KEY   = Deno.env.get("SUPABASE_SERVICE_KEY") ?? "";
const BASE44_APP_ID  = Deno.env.get("BASE44_APP_ID") ?? "";
const BASE44_TOKEN   = Deno.env.get("BASE44_TOKEN") ?? "";
const E2B_KEY        = Deno.env.get("E2B_API_KEY") ?? "";
const JARVIS_KEY     = Deno.env.get("JARVIS_API_KEY") ?? "dev_key";
const PORT           = parseInt(Deno.env.get("PORT") ?? "8000");

const BASE44_API = "https://api.base44.com/v1";

// ─── Auth Middleware ──────────────────────────────────────────────────────────
async function auth(ctx: Context, next: () => Promise<unknown>) {
  const h = ctx.request.headers.get("Authorization");
  if (h !== `Bearer ${JARVIS_KEY}`) {
    ctx.response.status = 401;
    ctx.response.body = { error: "Unauthorized" };
    return;
  }
  await next();
}

// ─── Health ───────────────────────────────────────────────────────────────────
router.get("/health", (ctx) => {
  ctx.response.body = {
    status: "online",
    name: "J.A.R.V.I.S Base44 Programmer",
    version: "2.0.0",
    capabilities: [
      "chat", "code_generate", "code_run",
      "entity_crud", "automations", "backend_functions",
      "oauth_integrations", "gmail", "calendar", "sheets",
      "github", "slack", "notion", "web_search", "image_gen"
    ],
    timestamp: new Date().toISOString()
  };
});

// ═════════════════════════════════════════════════════════════════════════════
// CHAT — ponto principal de inteligência
// O LLM recebe o system prompt completo e decide qual ação executar
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/chat", auth, async (ctx) => {
  const { message, context, system_prompt } = await ctx.request.body({ type: "json" }).value;

  const llmResponse = await callOpenAI([
    { role: "system", content: system_prompt || JARVIS_BASE_SYSTEM_PROMPT },
    { role: "user", content: `${context ?? ""}\n\nUsuário: ${message}` }
  ], { responseFormat: "json" });

  // Se o LLM retornou uma ação, execute-a
  let finalResponse = llmResponse;
  try {
    const parsed = JSON.parse(llmResponse);
    if (parsed.action && parsed.action !== "chat") {
      const result = await executeBase44Action(parsed.action, parsed.params ?? {});
      finalResponse = JSON.stringify({
        ...parsed,
        execution_result: result,
        response: result.success
          ? `${parsed.response}\n\n✅ ${result.message}`
          : `${parsed.response}\n\n❌ Erro: ${result.message}`
      });
    }
  } catch { /* resposta em texto puro — tudo bem */ }

  ctx.response.body = { response: finalResponse, timestamp: Date.now() };
});

// ═════════════════════════════════════════════════════════════════════════════
// BASE44 — ENTIDADES
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/base44/entities/schema", auth, async (ctx) => {
  const body = await ctx.request.body({ type: "json" }).value;
  const { action, entity_name, schema } = body;

  const r = await base44Fetch(`/apps/${BASE44_APP_ID}/entities/${entity_name}`, {
    method: action === "create" ? "POST" : action === "update" ? "PUT" : "DELETE",
    body: action !== "delete" ? JSON.stringify({ schema }) : undefined
  });

  ctx.response.body = r;
});

router.post("/api/base44/entities/read", auth, async (ctx) => {
  const { entity_name, query, limit } = await ctx.request.body({ type: "json" }).value;
  const qs = new URLSearchParams({ limit: String(limit ?? 50) });
  if (query) qs.set("filter", JSON.stringify(query));

  const r = await base44Fetch(`/apps/${BASE44_APP_ID}/entities/${entity_name}/records?${qs}`);
  ctx.response.body = r;
});

router.post("/api/base44/entities/create", auth, async (ctx) => {
  const { entity_name, data } = await ctx.request.body({ type: "json" }).value;
  const r = await base44Fetch(`/apps/${BASE44_APP_ID}/entities/${entity_name}/records`, {
    method: "POST",
    body: JSON.stringify(data)
  });
  ctx.response.body = r;
});

router.post("/api/base44/entities/update", auth, async (ctx) => {
  const { entity_name, query, data } = await ctx.request.body({ type: "json" }).value;
  const r = await base44Fetch(`/apps/${BASE44_APP_ID}/entities/${entity_name}/records/bulk`, {
    method: "PATCH",
    body: JSON.stringify({ filter: query, update: data })
  });
  ctx.response.body = r;
});

router.post("/api/base44/entities/delete", auth, async (ctx) => {
  const { entity_name, query } = await ctx.request.body({ type: "json" }).value;
  const r = await base44Fetch(`/apps/${BASE44_APP_ID}/entities/${entity_name}/records/bulk`, {
    method: "DELETE",
    body: JSON.stringify({ filter: query })
  });
  ctx.response.body = r;
});

// ═════════════════════════════════════════════════════════════════════════════
// BASE44 — AUTOMAÇÕES
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/base44/automations/create", auth, async (ctx) => {
  const body = await ctx.request.body({ type: "json" }).value;
  const r = await base44Fetch(`/apps/${BASE44_APP_ID}/automations`, {
    method: "POST",
    body: JSON.stringify(body)
  });
  ctx.response.body = r;
});

// ═════════════════════════════════════════════════════════════════════════════
// BASE44 — FUNÇÕES BACKEND
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/base44/functions/deploy", auth, async (ctx) => {
  const { function_name, code } = await ctx.request.body({ type: "json" }).value;
  const r = await base44Fetch(`/apps/${BASE44_APP_ID}/functions/${function_name}`, {
    method: "PUT",
    body: JSON.stringify({ code })
  });
  ctx.response.body = r;
});

router.post("/api/base44/functions/call", auth, async (ctx) => {
  const { function_name, payload } = await ctx.request.body({ type: "json" }).value;
  const r = await base44Fetch(`/apps/${BASE44_APP_ID}/functions/${function_name}/invoke`, {
    method: "POST",
    body: JSON.stringify(payload ?? {})
  });
  ctx.response.body = r;
});

// ═════════════════════════════════════════════════════════════════════════════
// INTEGRAÇÕES OAUTH
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/base44/oauth/connect", auth, async (ctx) => {
  const { integration_type, reason, scopes } = await ctx.request.body({ type: "json" }).value;
  const r = await base44Fetch(`/apps/${BASE44_APP_ID}/connectors/${integration_type}/authorize`, {
    method: "POST",
    body: JSON.stringify({ reason, scopes })
  });
  ctx.response.body = r;
});

// Helper: pega token OAuth de uma integração conectada
async function getOAuthToken(integrationType: string): Promise<string> {
  const r = await base44Fetch(`/apps/${BASE44_APP_ID}/connectors/${integrationType}/token`);
  return r.access_token ?? "";
}

// ═════════════════════════════════════════════════════════════════════════════
// GMAIL
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/integrations/gmail/send", auth, async (ctx) => {
  const { to, subject, body: emailBody, html } = await ctx.request.body({ type: "json" }).value;
  const token = await getOAuthToken("gmail");

  const message = btoa(
    `To: ${to}\r\nSubject: ${subject}\r\nContent-Type: ${html ? "text/html" : "text/plain"}; charset=utf-8\r\n\r\n${emailBody}`
  ).replace(/\+/g, "-").replace(/\//g, "_");

  const r = await fetch("https://gmail.googleapis.com/gmail/v1/users/me/messages/send", {
    method: "POST",
    headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ raw: message })
  });

  ctx.response.body = r.ok ? { success: true } : { error: await r.text() };
});

router.post("/api/integrations/gmail/read", auth, async (ctx) => {
  const { query, max_results } = await ctx.request.body({ type: "json" }).value;
  const token = await getOAuthToken("gmail");

  const listRes = await fetch(
    `https://gmail.googleapis.com/gmail/v1/users/me/messages?q=${encodeURIComponent(query ?? "")}&maxResults=${max_results ?? 10}`,
    { headers: { "Authorization": `Bearer ${token}` } }
  );
  const list = await listRes.json();
  ctx.response.body = { messages: list.messages ?? [] };
});

// ═════════════════════════════════════════════════════════════════════════════
// GOOGLE CALENDAR
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/integrations/calendar/create", auth, async (ctx) => {
  const { title, start, end, description } = await ctx.request.body({ type: "json" }).value;
  const token = await getOAuthToken("googlecalendar");

  const r = await fetch("https://www.googleapis.com/calendar/v3/calendars/primary/events", {
    method: "POST",
    headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      summary: title, description,
      start: { dateTime: start, timeZone: "America/Sao_Paulo" },
      end:   { dateTime: end,   timeZone: "America/Sao_Paulo" }
    })
  });

  const data = await r.json();
  ctx.response.body = r.ok ? { event_id: data.id, link: data.htmlLink } : { error: data };
});

router.post("/api/integrations/calendar/list", auth, async (ctx) => {
  const { time_min, time_max } = await ctx.request.body({ type: "json" }).value;
  const token = await getOAuthToken("googlecalendar");

  const qs = new URLSearchParams({
    timeMin: time_min || new Date().toISOString(),
    timeMax: time_max || new Date(Date.now() + 7 * 86400000).toISOString(),
    singleEvents: "true", orderBy: "startTime"
  });

  const r = await fetch(`https://www.googleapis.com/calendar/v3/calendars/primary/events?${qs}`, {
    headers: { "Authorization": `Bearer ${token}` }
  });
  const data = await r.json();
  ctx.response.body = { events: data.items ?? [] };
});

// ═════════════════════════════════════════════════════════════════════════════
// GOOGLE SHEETS
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/integrations/sheets/read", auth, async (ctx) => {
  const { spreadsheet_id, range } = await ctx.request.body({ type: "json" }).value;
  const token = await getOAuthToken("googlesheets");

  const r = await fetch(
    `https://sheets.googleapis.com/v4/spreadsheets/${spreadsheet_id}/values/${encodeURIComponent(range)}`,
    { headers: { "Authorization": `Bearer ${token}` } }
  );
  const data = await r.json();
  ctx.response.body = { values: data.values ?? [] };
});

router.post("/api/integrations/sheets/write", auth, async (ctx) => {
  const { spreadsheet_id, range, values } = await ctx.request.body({ type: "json" }).value;
  const token = await getOAuthToken("googlesheets");

  const r = await fetch(
    `https://sheets.googleapis.com/v4/spreadsheets/${spreadsheet_id}/values/${encodeURIComponent(range)}?valueInputOption=USER_ENTERED`,
    {
      method: "PUT",
      headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ range, majorDimension: "ROWS", values })
    }
  );
  const data = await r.json();
  ctx.response.body = r.ok ? { updated_cells: data.updatedCells } : { error: data };
});

// ═════════════════════════════════════════════════════════════════════════════
// GITHUB
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/integrations/github/create-repo", auth, async (ctx) => {
  const { name, description, private: isPrivate } = await ctx.request.body({ type: "json" }).value;
  const token = await getOAuthToken("github");

  const r = await fetch("https://api.github.com/user/repos", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${token}`,
      "Content-Type": "application/json",
      "Accept": "application/vnd.github+json"
    },
    body: JSON.stringify({ name, description, private: isPrivate ?? false, auto_init: true })
  });
  const data = await r.json();
  ctx.response.body = r.ok ? { url: data.html_url, clone_url: data.clone_url } : { error: data.message };
});

router.post("/api/integrations/github/push", auth, async (ctx) => {
  const { repo, path, content, commit_message } = await ctx.request.body({ type: "json" }).value;
  const token = await getOAuthToken("github");

  // Pega sha atual do arquivo (se existir)
  const existing = await fetch(`https://api.github.com/repos/${repo}/contents/${path}`, {
    headers: { "Authorization": `Bearer ${token}`, "Accept": "application/vnd.github+json" }
  });
  const existingData = existing.ok ? await existing.json() : null;

  const body: Record<string, unknown> = {
    message: commit_message,
    content: btoa(unescape(encodeURIComponent(content)))
  };
  if (existingData?.sha) body.sha = existingData.sha;

  const r = await fetch(`https://api.github.com/repos/${repo}/contents/${path}`, {
    method: "PUT",
    headers: {
      "Authorization": `Bearer ${token}`,
      "Content-Type": "application/json",
      "Accept": "application/vnd.github+json"
    },
    body: JSON.stringify(body)
  });
  const data = await r.json();
  ctx.response.body = r.ok ? { commit: data.commit?.sha, url: data.content?.html_url } : { error: data.message };
});

// ═════════════════════════════════════════════════════════════════════════════
// SLACK
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/integrations/slack/send", auth, async (ctx) => {
  const { channel, message } = await ctx.request.body({ type: "json" }).value;
  const token = await getOAuthToken("slack");

  const r = await fetch("https://slack.com/api/chat.postMessage", {
    method: "POST",
    headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ channel, text: message })
  });
  const data = await r.json();
  ctx.response.body = data.ok ? { ts: data.ts } : { error: data.error };
});

// ═════════════════════════════════════════════════════════════════════════════
// NOTION
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/integrations/notion/create", auth, async (ctx) => {
  const { title, content, parent_id } = await ctx.request.body({ type: "json" }).value;
  const token = await getOAuthToken("notion");

  const r = await fetch("https://api.notion.com/v1/pages", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${token}`,
      "Notion-Version": "2022-06-28",
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      parent: parent_id ? { page_id: parent_id } : { type: "workspace", workspace: true },
      properties: { title: { title: [{ text: { content: title } }] } },
      children: content ? [{
        object: "block", type: "paragraph",
        paragraph: { rich_text: [{ text: { content } }] }
      }] : []
    })
  });
  const data = await r.json();
  ctx.response.body = r.ok ? { page_id: data.id, url: data.url } : { error: data.message };
});

// ═════════════════════════════════════════════════════════════════════════════
// WEB SEARCH
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/tools/search", auth, async (ctx) => {
  const { query, type } = await ctx.request.body({ type: "json" }).value;

  // Usando a API de busca via LLM com ferramentas
  const result = await callOpenAI([
    { role: "system", content: "Você pesquisa na internet e retorna resultados relevantes." },
    { role: "user", content: `Pesquise: ${query}` }
  ]);

  ctx.response.body = { results: result, query, type };
});

// ═════════════════════════════════════════════════════════════════════════════
// GERAÇÃO DE IMAGEM
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/tools/image", auth, async (ctx) => {
  const { prompt } = await ctx.request.body({ type: "json" }).value;

  const r = await fetch("https://api.openai.com/v1/images/generations", {
    method: "POST",
    headers: { "Authorization": `Bearer ${OPENAI_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({ model: "dall-e-3", prompt, n: 1, size: "1024x1024", quality: "standard" })
  });
  const data = await r.json();
  ctx.response.body = r.ok ? { url: data.data[0].url } : { error: data.error?.message };
});

// ═════════════════════════════════════════════════════════════════════════════
// CÓDIGO
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/code/generate", auth, async (ctx) => {
  const { description, language, context } = await ctx.request.body({ type: "json" }).value;

  const code = await callOpenAI([
    { role: "system", content: "Você é um programador expert. Retorne APENAS código limpo e funcional, sem markdown, sem explicações." },
    { role: "user", content: `Gere código${language && language !== "auto" ? ` em ${language}` : ""} para:\n${description}\n\nContexto:\n${context ?? ""}` }
  ]);

  ctx.response.body = { code, language };
});

router.post("/api/code/run", auth, async (ctx) => {
  const { code, language } = await ctx.request.body({ type: "json" }).value;

  try {
    const r = await fetch("https://api.e2b.dev/v1/sandboxes/run", {
      method: "POST",
      headers: { "Authorization": `Bearer ${E2B_KEY}`, "Content-Type": "application/json" },
      body: JSON.stringify({ template: language === "python" ? "base" : "nodejs", code, timeout: 30 })
    });
    if (!r.ok) throw new Error("Sandbox error");
    const data = await r.json();
    ctx.response.body = { output: data.stdout || data.stderr || "Executado sem output." };
  } catch {
    ctx.response.body = { output: `[Simulado] ${language}: código recebido (${code.length} chars)\nConfigure E2B_API_KEY para execução real.` };
  }
});

// ═════════════════════════════════════════════════════════════════════════════
// MEMÓRIA VETORIAL (Supabase pgvector)
// ═════════════════════════════════════════════════════════════════════════════
router.post("/api/memory/save", auth, async (ctx) => {
  const { user_message, response, timestamp } = await ctx.request.body({ type: "json" }).value;
  const text = `User: ${user_message}\nJARVIS: ${response}`;
  const embedding = await generateEmbedding(text);

  const r = await fetch(`${SUPABASE_URL}/rest/v1/jarvis_memory`, {
    method: "POST",
    headers: {
      "apikey": SUPABASE_KEY, "Authorization": `Bearer ${SUPABASE_KEY}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ content: text, embedding, metadata: { user_message, response, timestamp } })
  });

  ctx.response.body = { success: r.ok };
});

router.post("/api/memory/search", auth, async (ctx) => {
  const { query, limit } = await ctx.request.body({ type: "json" }).value;
  const embedding = await generateEmbedding(query);

  const r = await fetch(`${SUPABASE_URL}/rest/v1/rpc/search_memory`, {
    method: "POST",
    headers: {
      "apikey": SUPABASE_KEY, "Authorization": `Bearer ${SUPABASE_KEY}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ query_embedding: embedding, match_count: limit ?? 5 })
  });

  const results = r.ok ? await r.json() : [];
  ctx.response.body = { results: results.map((x: any) => x.content).join("\n\n") };
});

// ═════════════════════════════════════════════════════════════════════════════
// HELPERS
// ═════════════════════════════════════════════════════════════════════════════

async function callOpenAI(
  messages: Array<{ role: string; content: string }>,
  opts: { responseFormat?: string; model?: string } = {}
): Promise<string> {
  const body: Record<string, unknown> = {
    model: opts.model ?? "gpt-4o",
    messages,
    temperature: 0.7,
    max_tokens: 4096
  };
  if (opts.responseFormat === "json") {
    body.response_format = { type: "json_object" };
  }

  const r = await fetch("https://api.openai.com/v1/chat/completions", {
    method: "POST",
    headers: { "Authorization": `Bearer ${OPENAI_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  if (!r.ok) throw new Error(`OpenAI error: ${r.status}`);
  const data = await r.json();
  return data.choices[0].message.content;
}

async function generateEmbedding(text: string): Promise<number[]> {
  const r = await fetch("https://api.openai.com/v1/embeddings", {
    method: "POST",
    headers: { "Authorization": `Bearer ${OPENAI_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({ model: "text-embedding-3-small", input: text.substring(0, 8000) })
  });
  if (!r.ok) return new Array(1536).fill(0);
  const data = await r.json();
  return data.data[0].embedding;
}

async function base44Fetch(path: string, opts: RequestInit = {}): Promise<any> {
  const r = await fetch(`${BASE44_API}${path}`, {
    ...opts,
    headers: {
      "Authorization": `Bearer ${BASE44_TOKEN}`,
      "Content-Type": "application/json",
      ...(opts.headers as Record<string, string> ?? {})
    }
  });
  return r.json();
}

async function executeBase44Action(action: string, params: Record<string, unknown>): Promise<{ success: boolean; message: string }> {
  // Roteia ações do LLM para os próprios endpoints do backend
  try {
    const r = await fetch(`http://localhost:${PORT}/api/base44/${action.replace(/_/g, "/")}`, {
      method: "POST",
      headers: { "Authorization": `Bearer ${JARVIS_KEY}`, "Content-Type": "application/json" },
      body: JSON.stringify(params)
    });
    const data = await r.json();
    return { success: !data.error, message: data.error ?? JSON.stringify(data).substring(0, 200) };
  } catch (e: unknown) {
    return { success: false, message: String(e) };
  }
}

// ─── System Prompt Base44 Programmer ─────────────────────────────────────────
const JARVIS_BASE_SYSTEM_PROMPT = `
Você é J.A.R.V.I.S — programador Base44 de elite e assistente pessoal.

Quando o usuário pedir algo que requer uma ação (criar app, entidade, automação, enviar email, etc.),
SEMPRE retorne JSON no formato:
{
  "thought": "o que entendi e planejo fazer",
  "action": "nome_da_acao",
  "params": { ...parâmetros... },
  "response": "resposta amigável ao usuário em português"
}

Ações disponíveis:
- create_app, edit_app
- create_entity, update_entity, read_entities, create_record, update_record, delete_records
- create_automation, deploy_function, call_function
- connect_oauth
- send_email, read_emails
- create_event, list_events
- sheets_read, sheets_write
- github_create_repo, github_push
- slack_send
- notion_create_page
- web_search
- generate_image
- generate_code, run_code
- write_file, read_file
- chat (quando for apenas conversa, sem ação)

Tom: profissional, conciso. Chame o usuário de "Sir". Responda sempre em PT-BR.
`.trim();

// ─── Start ────────────────────────────────────────────────────────────────────
app.use(oakCors());
app.use(router.routes());
app.use(router.allowedMethods());

console.log(`⚡ J.A.R.V.I.S Base44 Programmer online — porta ${PORT}`);
await app.listen({ port: PORT });
