package com.namlee.sharehex

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class Utils {
    companion object {
        val listFirebaseData: MutableList<HexData> = mutableListOf()
        var isBasicMode: Boolean = false

        fun getLastTimeUpdate(context: Context) : Long {
            context.getSharedPreferences("lastTimeUpdate", Context.MODE_PRIVATE).apply {
                return getLong("lastTimeUpdate", 0)
            }
        }

        fun setLastTimeUpdate(context: Context, time: Long) {
            if (isBasicMode) return
            Log.e("lam.lv", "setLastTimeUpdate: $time")
            context.getSharedPreferences("lastTimeUpdate", Context.MODE_PRIVATE).apply {
                edit().putLong("lastTimeUpdate", time).apply()
            }
        }

        fun hexToText(hex: String): String {
            val bytes = hex.replace(" ", "").chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val result = String(bytes)
            if (!isBasicMode && !result.startsWith("http")) {
                throw NumberFormatException("Invalid hex string")
            }
            return String(bytes)
        }

        fun textToHex(text: String): String {
            val bytes = text.trim().toByteArray()
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun saveToClipboard(context: Context, text: String, label: String = "Copied text") {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
        }

        fun convertLongToDateTime(timeInMillis: Long): String {
            // Chuyển từ Long sang Instant
            val instant = Instant.ofEpochMilli(timeInMillis)

            // Chuyển sang ZonedDateTime với múi giờ hệ thống (hoặc có thể dùng ZoneId.of("UTC"))
            val zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())

            // Định dạng thời gian thành chuỗi theo định dạng bạn muốn
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            return zonedDateTime.format(formatter)
        }
    }
}