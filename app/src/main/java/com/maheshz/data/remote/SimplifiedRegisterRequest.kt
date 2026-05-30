package com.maheshz.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SimplifiedRegisterRequest(
    val orgCode: String,
    val fullName: String,
    val email: String,
    val fpPublicKey: String,
    val deviceFingerprint: String
)