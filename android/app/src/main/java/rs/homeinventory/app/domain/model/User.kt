package rs.homeinventory.app.domain.model

import rs.homeinventory.app.data.local.UserRole

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val isActive: Boolean,
    val currency: String
)
