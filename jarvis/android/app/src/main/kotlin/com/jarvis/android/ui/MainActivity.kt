package com.jarvis.android.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.android.service.JarvisOverlayService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisSetupScreen(
                onRequestOverlay = { requestOverlayPermission() },
                onRequestAccessibility = { requestAccessibilityPermission() },
                onStartJarvis = { startJarvisOverlay() }
            )
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun requestAccessibilityPermission() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun startJarvisOverlay() {
        if (Settings.canDrawOverlays(this)) {
            startForegroundService(Intent(this, JarvisOverlayService::class.java))
            finish() // fecha a activity, overlay fica na tela
        }
    }
}

@Composable
fun JarvisSetupScreen(
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onStartJarvis: () -> Unit
) {
    val JarvisBlue = Color(0xFF00D4FF)
    val JarvisDark = Color(0xFF0A0E1A)
    val JarvisCard = Color(0xFF111827)
    val JarvisAccent = Color(0xFF1E40AF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Logo
            Text(
                "J.A.R.V.I.S",
                color = JarvisBlue,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )
            Text(
                "Just A Rather Very Intelligent System",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Passo 1
            SetupCard(
                step = "1",
                title = "Permissão de Overlay",
                description = "Permite que o J.A.R.V.I.S fique fixo sobre outros apps",
                buttonText = "Conceder",
                onClick = onRequestOverlay,
                cardColor = JarvisCard,
                accentColor = JarvisBlue
            )

            // Passo 2
            SetupCard(
                step = "2",
                title = "Serviço de Acessibilidade",
                description = "Permite interagir com a tela e outros aplicativos",
                buttonText = "Ativar",
                onClick = onRequestAccessibility,
                cardColor = JarvisCard,
                accentColor = JarvisBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Botão iniciar
            Button(
                onClick = onStartJarvis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "⚡  INICIAR J.A.R.V.I.S",
                    color = Color(0xFF0A0E1A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun SetupCard(
    step: String,
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
    cardColor: Color,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor.copy(0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(step, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(description, color = Color.Gray, fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
            ) {
                Text(buttonText, fontSize = 12.sp)
            }
        }
    }
}
