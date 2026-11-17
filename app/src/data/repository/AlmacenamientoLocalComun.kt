package com.example.data.repository

import android.content.Context
import com.example.data.remote.MobileSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal fun optionalUserId(appContext: Context): String? = MobileSession.userId(appContext)

internal fun nowIsoUtc(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())
