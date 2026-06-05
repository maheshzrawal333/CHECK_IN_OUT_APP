package com.maheshz.checkinout.util

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val EMPLOYEE_ID = stringPreferencesKey("employee_id")
        val ORG_ID = stringPreferencesKey("org_id")
        val EMPLOYEE_CODE = stringPreferencesKey("employee_code")
        val FULL_NAME = stringPreferencesKey("full_name")
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        val JWT_ACCESS_TOKEN = stringPreferencesKey("jwt_access_token")
        val JWT_REFRESH_TOKEN = stringPreferencesKey("jwt_refresh_token")
        val ORG_CODE = stringPreferencesKey("org_code")
        val SESSION_ID = stringPreferencesKey("session_id")
    }

    val isPendingFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        // If we have an employee ID cached but NO access token, they are waiting for approval
        prefs[EMPLOYEE_ID] != null && prefs[JWT_ACCESS_TOKEN] == null
    }

    suspend fun saveAuthData(empId: String, orgId: String, empCode: String, orgCode: String, name: String, access: String, refresh: String) {
        context.dataStore.edit { prefs ->
            prefs[EMPLOYEE_ID] = empId
            prefs[ORG_ID] = orgId
            prefs[EMPLOYEE_CODE] = empCode
            prefs[ORG_CODE] = orgCode
            prefs[FULL_NAME] = name
            prefs[JWT_ACCESS_TOKEN] = access
            prefs[JWT_REFRESH_TOKEN] = refresh
        }
    }

    suspend fun saveTokens(access: String, refresh: String) {
        context.dataStore.edit { prefs ->
            prefs[JWT_ACCESS_TOKEN] = access
            prefs[JWT_REFRESH_TOKEN] = refresh
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    val employeeCodeFlow: Flow<String?> = context.dataStore.data.map { it[EMPLOYEE_CODE] }
    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[JWT_ACCESS_TOKEN] }
    val refreshTokenFlow: Flow<String?> = context.dataStore.data.map { it[JWT_REFRESH_TOKEN] }
    val orgCodeFlow: Flow<String?> = context.dataStore.data.map { it[ORG_CODE] }
    val fullNameFlow: Flow<String?> = context.dataStore.data.map { it[FULL_NAME] }
    val sessionIdFlow: Flow<String?> = context.dataStore.data.map { it[SESSION_ID] }

    fun getDeviceFingerprint(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    suspend fun savePendingData(empId: String, empCode: String, orgCode: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[EMPLOYEE_ID] = empId
            prefs[EMPLOYEE_CODE] = empCode
            prefs[ORG_CODE] = orgCode
            prefs[FULL_NAME] = name

            // Explicitly strip away any old tokens if they exist
            // to force the app to stay on the waiting screen layout
            prefs.remove(JWT_ACCESS_TOKEN)
            prefs.remove(JWT_REFRESH_TOKEN)
        }
    }

    suspend fun saveSessionId(id: String) {
        context.dataStore.edit { it[SESSION_ID] = id }
    }

    suspend fun saveOrgCode(orgCode: String) {
        context.dataStore.edit { preferences ->
            preferences[ORG_CODE] = orgCode
        }
    }

    suspend fun saveEmployeeCode(employeeCode: String) {
        context.dataStore.edit { preferences ->
            preferences[EMPLOYEE_CODE] = employeeCode
        }
    }

    suspend fun saveFullName(fullName: String) {
        context.dataStore.edit { preferences ->
            preferences[FULL_NAME] = fullName
        }
    }
}