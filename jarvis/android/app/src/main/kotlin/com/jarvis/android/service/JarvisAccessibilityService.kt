package com.jarvis.android.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.jarvis.android.JarvisApp

/**
 * JarvisAccessibilityService
 * Permite ao J.A.R.V.I.S interagir com qualquer app aberto:
 * - Ler conteúdo da tela
 * - Clicar em elementos
 * - Preencher campos de texto
 * - Navegar entre apps
 * - Monitorar o que está na tela
 */
class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set
    }

    private var currentPackage = ""
    private var screenContent = StringBuilder()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        configureService()
        JarvisApp.instance.core.onAccessibilityReady(this)
    }

    private fun configureService() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                currentPackage = event.packageName?.toString() ?: ""
                notifyAppChange(currentPackage)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                // Monitorar digitação se necessário
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                captureScreenContent()
            }
        }
    }

    /**
     * Captura todo o conteúdo textual da tela atual
     */
    fun captureScreenContent(): String {
        screenContent.clear()
        rootInActiveWindow?.let { root ->
            extractText(root, screenContent)
        }
        return screenContent.toString()
    }

    private fun extractText(node: AccessibilityNodeInfo, builder: StringBuilder) {
        if (!node.text.isNullOrEmpty()) {
            builder.append(node.text).append("\n")
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { extractText(it, builder) }
        }
    }

    /**
     * Clica em um elemento pelo texto
     */
    fun clickByText(text: String): Boolean {
        val node = rootInActiveWindow?.findAccessibilityNodeInfosByText(text)?.firstOrNull()
        return node?.let {
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            true
        } ?: false
    }

    /**
     * Digita texto em um campo focado
     */
    fun typeText(text: String): Boolean {
        val node = findFocusedEditText()
        return node?.let {
            val args = android.os.Bundle()
            args.putString(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
            it.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            true
        } ?: false
    }

    /**
     * Abre as Opções do Desenvolvedor
     */
    fun openDeveloperOptions() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        applicationContext.startActivity(intent)
    }

    /**
     * Abre as Configurações do sistema
     */
    fun openSettings(action: String = android.provider.Settings.ACTION_SETTINGS) {
        val intent = Intent(action)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        applicationContext.startActivity(intent)
    }

    private fun findFocusedEditText(): AccessibilityNodeInfo? {
        return rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
    }

    private fun notifyAppChange(packageName: String) {
        JarvisApp.instance.core.onAppChanged(packageName)
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
