package com.dmjobs.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WhatsAppAccessibilityService : AccessibilityService() {

    companion object {
        var isEnabled = false
        // Set to true by MessagingService when it's waiting for send button to be tapped
        var waitingForSend = false
        // Callback invoked after send is tapped
        var onSendTapped: (() -> Unit)? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isEnabled = true
    }

    override fun onInterrupt() {
        isEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isEnabled = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!waitingForSend) return
        if (event?.packageName != "com.whatsapp") return

        val root = rootInActiveWindow ?: return
        val tapped = trySendButton(root)
        if (tapped) {
            waitingForSend = false
            onSendTapped?.invoke()
        }
    }

    private fun trySendButton(node: AccessibilityNodeInfo): Boolean {
        // WhatsApp send button resource IDs (may vary by WA version)
        val sendIds = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp:id/send_btn"
        )

        for (id in sendIds) {
            val nodes = node.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) {
                val sendNode = nodes[0]
                if (sendNode.isEnabled && sendNode.isClickable) {
                    sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
            }
        }

        // Fallback: look for a node with "Send" content description
        return findAndClickSend(node)
    }

    private fun findAndClickSend(node: AccessibilityNodeInfo): Boolean {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        if ((desc.contains("send") || text.contains("send")) && node.isClickable && node.isEnabled) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickSend(child)) return true
        }
        return false
    }
}

