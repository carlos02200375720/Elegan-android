package com.example.data.remote

import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.util.UUID

internal object AccesosRapidosYGoogle {

    fun loginAdminLocally(context: Context): AuthResponse {
        val accounts = AlmacenCuentasLocales.getStoredAccounts(context)
        val existing = accounts.find {
            it.email.equals(MobileSession.SUPER_ADMIN_EMAIL, ignoreCase = true)
        }
        val adminUser = AuthUser(
            id = existing?.id ?: "local_admin_elegan",
            email = MobileSession.SUPER_ADMIN_EMAIL,
            name = existing?.name ?: "Carlos García (Administrador)",
            photoUrl = existing?.photoUrl ?: "",
            role = "admin",
            provider = "admin_quick"
        )
        val token = "local_session_" + UUID.randomUUID().toString()
        val response = AuthResponse(token = token, user = adminUser)
        AlmacenCifradoSesion.save(context, response)
        return response
    }

    fun loginClientLocally(context: Context): AuthResponse {
        val clientUser = AuthUser(
            id = "local_client_elegan",
            email = "cliente.elegan@gmail.com",
            name = "Cliente Elegan",
            photoUrl = "",
            role = "customer",
            provider = "client_quick"
        )
        val token = "local_session_" + UUID.randomUUID().toString()
        val response = AuthResponse(token = token, user = clientUser)
        AlmacenCifradoSesion.save(context, response)
        return response
    }

    fun loginWithGoogleTokenLocally(context: Context, googleIdToken: String): AuthResponse {
        var email = ""
        var name = "Usuario Google"
        var picture = ""
        var sub = UUID.randomUUID().toString()

        try {
            val parts = googleIdToken.split(".")
            if (parts.size >= 2) {
                val payloadJson = String(
                    Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP),
                    Charsets.UTF_8
                )
                val obj = JSONObject(payloadJson)
                email = obj.optString("email", "")
                name = obj.optString("name", "Usuario Google")
                picture = obj.optString("picture", "")
                sub = obj.optString("sub", sub)
            }
        } catch (e: Exception) {
            Log.w(
                "MobileSession",
                "No se pudo decodificar el payload del token de Google, usando identificador único",
                e
            )
        }

        val role = if (email.equals(MobileSession.SUPER_ADMIN_EMAIL, ignoreCase = true)) {
            "admin"
        } else {
            "customer"
        }
        val authUser = AuthUser(
            id = sub,
            email = email.ifBlank { null },
            name = name,
            photoUrl = picture,
            role = role,
            provider = "google"
        )
        val token = "local_session_" + UUID.randomUUID().toString()
        val response = AuthResponse(token = token, user = authUser)
        AlmacenCifradoSesion.save(context, response)
        return response
    }
}
