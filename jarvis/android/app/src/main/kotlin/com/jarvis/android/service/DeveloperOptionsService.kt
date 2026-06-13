package com.jarvis.android.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import com.jarvis.android.JarvisApp

/**
 * DeveloperOptionsService
 * Gerencia acesso e modificação das Opções do Desenvolvedor do Android.
 * NOTA: Settings.Global/Secure requerem WRITE_SECURE_SETTINGS
 * que pode ser concedido via ADB sem root:
 * adb shell pm grant com.jarvis.android android.permission.WRITE_SECURE_SETTINGS
 */
class DeveloperOptionsService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Habilita modo desenvolvedor
     */
    fun enableDeveloperMode() {
        Settings.Global.putInt(
            contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            1
        )
    }

    /**
     * Habilita/desabilita depuração USB
     */
    fun setUsbDebugging(enabled: Boolean) {
        Settings.Global.putInt(
            contentResolver,
            Settings.Global.ADB_ENABLED,
            if (enabled) 1 else 0
        )
    }

    /**
     * Habilita/desabilita animações (0 = sem animação, mais rápido)
     */
    fun setAnimations(scale: Float = 0f) {
        Settings.Global.putFloat(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, scale)
        Settings.Global.putFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, scale)
        Settings.Global.putFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
    }

    /**
     * Mantém a tela ligada enquanto carregando
     */
    fun setStayAwake(enabled: Boolean) {
        Settings.Global.putInt(
            contentResolver,
            Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
            if (enabled) 3 else 0
        )
    }

    /**
     * Habilita layout bounds para debug de UI
     */
    fun setShowLayoutBounds(enabled: Boolean) {
        Settings.Global.putInt(
            contentResolver,
            Settings.Global.SHOW_LAYOUT_BOUNDS,
            if (enabled) 1 else 0
        )
    }

    /**
     * Configura limite de processos em background
     */
    fun setBackgroundProcessLimit(limit: Int) {
        Settings.Global.putInt(
            contentResolver,
            Settings.Global.BACKGROUND_PROCESS_LIMIT,
            limit
        )
    }

    /**
     * Habilita perfil GPU
     */
    fun setGpuProfiling(enabled: Boolean) {
        Settings.Global.putString(
            contentResolver,
            "profile_gpu_rendering",
            if (enabled) "visual_bars" else "false"
        )
    }

    /**
     * Retorna todas as configurações de desenvolvedor atuais
     */
    fun getDeveloperSettings(): Map<String, Any?> {
        return mapOf(
            "developer_mode" to Settings.Global.getInt(
                contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0),
            "usb_debugging" to Settings.Global.getInt(
                contentResolver, Settings.Global.ADB_ENABLED, 0),
            "window_animation" to Settings.Global.getFloat(
                contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1f),
            "stay_awake" to Settings.Global.getInt(
                contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0),
            "show_layout_bounds" to Settings.Global.getInt(
                contentResolver, Settings.Global.SHOW_LAYOUT_BOUNDS, 0),
            "background_limit" to Settings.Global.getInt(
                contentResolver, Settings.Global.BACKGROUND_PROCESS_LIMIT, -1)
        )
    }
}
