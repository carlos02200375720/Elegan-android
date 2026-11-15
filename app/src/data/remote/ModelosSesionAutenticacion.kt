package com.example.data.remote

data class AuthUser(
    val id: String = "",
    val originalId: String? = null,
    val email: String? = null,
    val name: String = "",
    val photoUrl: String = "",
    val avatar: String? = null,
    val role: String = "customer",
    val provider: String = "password",
    val isAdmin: Boolean = false
) {
    val effectivePhotoUrl: String
        get() = photoUrl.ifBlank { avatar.orEmpty() }

    val effectiveId: String
        get() = originalId?.takeIf { it.isNotBlank() } ?: id
}

data class AuthResponse(
    val token: String = "",
    val user: AuthUser = AuthUser(),
    val success: Boolean = true,
    val message: String? = null
)

internal data class StoredAccount(
    val id: String,
    val email: String,
    val name: String,
    val passwordHash: String,
    val role: String,
    val photoUrl: String
)
