package com.example.data.remote

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

internal object AlmacenCuentasLocales {

    fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getStoredAccounts(context: Context): MutableList<StoredAccount> {
        val prefs = AlmacenCifradoSesion.preferences(context)
        val json = prefs.getString(AlmacenCifradoSesion.LOCAL_USERS_KEY, null)
            ?: return mutableListOf()
        val list = mutableListOf<StoredAccount>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StoredAccount(
                        id = obj.getString("id"),
                        email = obj.getString("email"),
                        name = obj.optString("name", "Usuario"),
                        passwordHash = obj.optString("passwordHash", ""),
                        role = obj.optString("role", "customer"),
                        photoUrl = obj.optString("photoUrl", "")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("MobileSession", "Error al leer cuentas locales", e)
        }
        return list
    }

    fun saveStoredAccounts(context: Context, accounts: List<StoredAccount>) {
        try {
            val array = JSONArray()
            for (acc in accounts) {
                val obj = JSONObject().apply {
                    put("id", acc.id)
                    put("email", acc.email)
                    put("name", acc.name)
                    put("passwordHash", acc.passwordHash)
                    put("role", acc.role)
                    put("photoUrl", acc.photoUrl)
                }
                array.put(obj)
            }
            AlmacenCifradoSesion.preferences(context)
                .edit()
                .putString(AlmacenCifradoSesion.LOCAL_USERS_KEY, array.toString())
                .apply()
        } catch (e: Exception) {
            Log.e("MobileSession", "Error al guardar cuentas locales", e)
        }
    }

    fun deleteCurrentAccount(context: Context) {
        val currentUser = AlmacenCifradoSesion.user(context)
        if (currentUser != null) {
            val currentEmail = currentUser.email?.trim().orEmpty()
            val currentId = currentUser.effectiveId.trim()
            val remaining = getStoredAccounts(context).filterNot { acc ->
                (currentEmail.isNotBlank() && acc.email.equals(currentEmail, ignoreCase = true)) ||
                    (currentId.isNotBlank() && acc.id == currentId)
            }
            saveStoredAccounts(context, remaining)
        }
        AlmacenCifradoSesion.clear(context)
    }
}
