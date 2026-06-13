package com.jarvis.android.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.compose.ui.platform.ComposeView
import com.jarvis.android.JarvisApp
import com.jarvis.android.R
import com.jarvis.android.ui.overlay.JarvisOverlayUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * JarvisOverlayService
 * Serviço que mantém o overlay flutuante do J.A.R.V.I.S fixo na tela
 * mesmo quando outros apps estão abertos.
 */
class JarvisOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var chatPanelView: View
    private var isChatOpen = false

    // Posição do botão flutuante
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupOverlayButton()
        setupChatPanel()
        startForeground()
    }

    private fun setupOverlayButton() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_button, null)
        val jarvisIcon = overlayView.findViewById<ImageView>(R.id.jarvis_icon)

        // Arrastar o botão flutuante
        jarvisIcon.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = Math.abs(event.rawX - initialTouchX) > 10 ||
                                Math.abs(event.rawY - initialTouchY) > 10
                    if (!moved) toggleChat()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(overlayView, params)
    }

    private fun setupChatPanel() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.65).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        // Compose UI para o painel de chat
        chatPanelView = ComposeView(this).apply {
            setContent {
                JarvisOverlayUI(
                    onClose = { toggleChat() },
                    onMessage = { message -> handleMessage(message) }
                )
            }
        }
        chatPanelView.visibility = View.GONE
        windowManager.addView(chatPanelView, params)
    }

    private fun toggleChat() {
        isChatOpen = !isChatOpen
        chatPanelView.visibility = if (isChatOpen) View.VISIBLE else View.GONE
    }

    private fun handleMessage(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = JarvisApp.instance.core.processMessage(message)
            // Atualizar UI com resposta
        }
    }

    private fun startForeground() {
        val notification = createForegroundNotification()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) windowManager.removeView(overlayView)
        if (::chatPanelView.isInitialized) windowManager.removeView(chatPanelView)
    }
}
