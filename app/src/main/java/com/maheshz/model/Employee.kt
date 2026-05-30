package com.maheshz.model

import kotlinx.serialization.Serializable

@Serializable
data class Employee(
    val employeeId: String,
    val orgId: String,
    val employeeCode: String,
    val fullName: String,
    val serverBaseUrl: String,
    val deviceFingerprint: String
)
