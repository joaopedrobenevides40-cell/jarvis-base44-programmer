package com.jarvis.android.ui.overlay

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta J.A.R.V.I.S — azul holográfico + fundo escuro
private val JarvisBlue = Color(0xFF00D4FF)
private val JarvisDark = Color(0xFF0A0E1A)
private val JarvisCard = Color(0xFF111827)
private val JarvisAccent = Color(0xFF1E40AF)
private val JarvisGold = Color(0xFFFFB800)

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val code: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun JarvisOverlayUI(
    onClose: () -> Unit,
    onMessage: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf(
        ChatMessage(
            "Online e operacional, Sir. Como posso auxiliá-lo?",
            isUser = false
        )
    ))}
    var isLoading by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0=Chat 1=Code 2=Files 3=Dev

    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.65f)
            .background(JarvisDark, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(JarvisBlue.copy(0.3f), JarvisAccent.copy(0.3f))),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header J.A.R.V.I.S
            JarvisHeader(onClose = onClose)

            // Tabs
            JarvisTabs(activeTab = activeTab, onTabSelected = { activeTab = it })

            // Conteúdo
            when (activeTab) {
                0 -> ChatTab(
                    messages = messages,
                    inputText = inputText,
                    isLoading = isLoading,
                    listState = listState,
                    onInputChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            messages = messages + ChatMessage(inputText, isUser = true)
                            onMessage(inputText)
                            inputText = ""
                            isLoading = true
                        }
                    }
                )
                1 -> CodeEditorTab()
                2 -> FilesTab()
                3 -> DevOptionsTab()
            }
        }
    }
}

@Composable
fun JarvisHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(JarvisAccent.copy(0.3f), JarvisBlue.copy(0.1f))
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo / Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(JarvisBlue, JarvisAccent))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("J", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "J.A.R.V.I.S",
                color = JarvisBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )
            Text(
                "● Online",
                color = Color(0xFF22C55E),
                fontSize = 11.sp
            )
        }

        // Botão fechar
        IconButton(onClick = onClose) {
            Icon(Icons.Default.KeyboardArrowDown, "Minimizar", tint = JarvisBlue)
        }
    }
}

@Composable
fun JarvisTabs(activeTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(
        Pair(Icons.Default.Chat, "Chat"),
        Pair(Icons.Default.Code, "Código"),
        Pair(Icons.Default.Folder, "Arquivos"),
        Pair(Icons.Default.DeveloperMode, "Dev")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisCard)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        tabs.forEachIndexed { index, (icon, label) ->
            val selected = activeTab == index
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(index) }
                    .background(if (selected) JarvisBlue.copy(0.15f) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    icon, label,
                    tint = if (selected) JarvisBlue else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    label,
                    fontSize = 10.sp,
                    color = if (selected) JarvisBlue else Color.Gray
                )
            }
        }
    }
}

@Composable
fun ChatTab(
    messages: List<ChatMessage>,
    inputText: String,
    isLoading: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Lista de mensagens
        LazyColumn(
            modifier = Modifier.weight(1f).padding(12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
            if (isLoading) {
                item { LoadingBubble() }
            }
        }

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(JarvisCard)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Diga algo, Sir...", color = Color.Gray, fontSize = 13.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1F2937),
                    unfocusedContainerColor = Color(0xFF1F2937),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = JarvisBlue,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 3,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Brush.radialGradient(listOf(JarvisBlue, JarvisAccent)),
                        CircleShape
                    )
            ) {
                Icon(Icons.Default.Send, "Enviar", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) JarvisAccent else JarvisCard
    val shape = if (message.isUser)
        RoundedCornerShape(12.dp, 2.dp, 12.dp, 12.dp)
    else
        RoundedCornerShape(2.dp, 12.dp, 12.dp, 12.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!message.isUser) {
            Text("J.A.R.V.I.S", color = JarvisBlue, fontSize = 10.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(2.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bgColor, shape)
                .padding(10.dp, 8.dp)
        ) {
            Text(
                message.text,
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        // Bloco de código se existir
        message.code?.let { code ->
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .border(1.dp, JarvisBlue.copy(0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    code,
                    color = Color(0xFF7DD3FC),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun LoadingBubble() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .background(JarvisCard, RoundedCornerShape(12.dp))
                .padding(12.dp, 10.dp)
        ) {
            Text("J.A.R.V.I.S está pensando...", color = JarvisBlue, fontSize = 12.sp)
        }
    }
}

@Composable
fun CodeEditorTab() {
    var code by remember { mutableStateOf("// Escreva seu código aqui, Sir\nfun main() {\n    println(\"J.A.R.V.I.S online\")\n}") }
    var language by remember { mutableStateOf("Kotlin") }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Editor de Código", color = JarvisBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row {
                OutlinedButton(
                    onClick = {},
                    border = BorderStroke(1.dp, JarvisBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(language, color = JarvisBlue, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, "Executar", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rodar", fontSize = 11.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = code,
            onValueChange = { code = it },
            modifier = Modifier.fillMaxSize(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A),
                focusedTextColor = Color(0xFF7DD3FC),
                unfocusedTextColor = Color(0xFF7DD3FC)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        )
    }
}

@Composable
fun FilesTab() {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Gerenciador de Arquivos", color = JarvisBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))
        listOf(
            "/sdcard/Documents", "/sdcard/Downloads",
            "/sdcard/DCIM", "/sdcard/Android/data"
        ).forEach { path ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {}
                    .background(JarvisCard)
                    .padding(12.dp, 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Folder, path, tint = JarvisGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(path, color = Color.White, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun DevOptionsTab() {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Opções do Desenvolvedor", color = JarvisBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))

        val options = listOf(
            "Depuração USB" to false,
            "Desativar Animações" to false,
            "Mostrar Layout Bounds" to false,
            "Tela sempre ativa" to true,
            "GPU Profiling" to false,
            "Mock Location" to false
        )

        options.forEach { (label, default) ->
            var checked by remember { mutableStateOf(default) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JarvisCard, RoundedCornerShape(8.dp))
                    .padding(12.dp, 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = Color.White, fontSize = 13.sp)
                Switch(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisDark,
                        checkedTrackColor = JarvisBlue
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = JarvisAccent)
        ) {
            Icon(Icons.Default.DeveloperMode, "Dev Options", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Abrir Opções do Desenvolvedor")
        }
    }
}
