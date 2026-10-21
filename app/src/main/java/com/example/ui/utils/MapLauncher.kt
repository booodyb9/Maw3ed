package com.example.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object MapLauncher {
    fun openInMaps(context: Context, lat: Double?, lng: Double?, query: String?) {
        val uri: Uri = when {
            lat != null && lng != null -> {
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            }
            !query.isNullOrBlank() -> {
                Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query.trim())}")
            }
            else -> {
                Toast.makeText(context, "لا يوجد موقع محدد لهذا الموعد", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الخرائط", Toast.LENGTH_SHORT).show()
        }
    }
}
