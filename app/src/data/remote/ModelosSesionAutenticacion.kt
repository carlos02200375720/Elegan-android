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

    val canUploadMedia: Boolean
        get() = isAdmin ||
            role.equals("admin", ignoreCase = true) ||
            role.equals("superadmin", ignoreCase = true)
}

data class AuthResponse(
    val token: String = "",
    val user: AuthUser = AuthUser(),
    val success: Boolean = true,
    val message: String? = null
)
