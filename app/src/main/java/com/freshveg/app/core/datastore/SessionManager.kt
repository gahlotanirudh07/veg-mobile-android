package com.freshveg.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auth_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val MOBILE = stringPreferencesKey("mobile")
        val ROLE = stringPreferencesKey("role")
        val SELLER_CODE = stringPreferencesKey("seller_code")
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val IS_SELLER = booleanPreferencesKey("is_seller")
        val IS_BUYER = booleanPreferencesKey("is_buyer")
        val IS_STAFF = booleanPreferencesKey("is_staff")
        val CAN_MANAGE_RATES = booleanPreferencesKey("can_manage_rates")
        val CAN_MANAGE_ORDERS = booleanPreferencesKey("can_manage_orders")
        val CAN_MANAGE_BILLING = booleanPreferencesKey("can_manage_billing")
        val CAN_MANAGE_STAFF = booleanPreferencesKey("can_manage_staff")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val SAVED_MOBILE = stringPreferencesKey("saved_mobile")
        val SAVED_PASSWORD = stringPreferencesKey("saved_password")
        val HIDE_MASTER_CATALOGUE = booleanPreferencesKey("hide_master_catalogue")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val businessName: Flow<String?> = context.dataStore.data.map { it[BUSINESS_NAME] }
    val role: Flow<String?> = context.dataStore.data.map { it[ROLE] }
    val sellerCode: Flow<String?> = context.dataStore.data.map { it[SELLER_CODE] }
    val isSeller: Flow<Boolean> = context.dataStore.data.map { it[IS_SELLER] ?: false }
    val isBuyer: Flow<Boolean> = context.dataStore.data.map { it[IS_BUYER] ?: false }
    val isStaff: Flow<Boolean> = context.dataStore.data.map { it[IS_STAFF] ?: false }
    val canManageRates: Flow<Boolean> = context.dataStore.data.map { it[CAN_MANAGE_RATES] ?: true }
    val canManageOrders: Flow<Boolean> = context.dataStore.data.map { it[CAN_MANAGE_ORDERS] ?: true }
    val canManageBilling: Flow<Boolean> = context.dataStore.data.map { it[CAN_MANAGE_BILLING] ?: true }
    val canManageStaff: Flow<Boolean> = context.dataStore.data.map { it[CAN_MANAGE_STAFF] ?: true }
    val rememberMe: Flow<Boolean> = context.dataStore.data.map { it[REMEMBER_ME] ?: false }
    val savedMobile: Flow<String?> = context.dataStore.data.map { it[SAVED_MOBILE] }
    val savedPassword: Flow<String?> = context.dataStore.data.map { it[SAVED_PASSWORD] }
    val hideMasterCatalogue: Flow<Boolean> = context.dataStore.data.map { it[HIDE_MASTER_CATALOGUE] ?: false }

    suspend fun setHideMasterCatalogue(hide: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HIDE_MASTER_CATALOGUE] = hide
        }
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        name: String,
        mobile: String,
        role: String = "SELLER",
        sellerCode: String?,
        businessName: String,
        isSeller: Boolean = false,
        isBuyer: Boolean = true,
        isStaff: Boolean = false,
        canManageRates: Boolean = true,
        canManageOrders: Boolean = true,
        canManageBilling: Boolean = true,
        canManageStaff: Boolean = true
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
            prefs[USER_ID] = userId
            prefs[USER_NAME] = name
            prefs[MOBILE] = mobile
            prefs[ROLE] = role
            sellerCode?.let { prefs[SELLER_CODE] = it }
            prefs[BUSINESS_NAME] = businessName
            prefs[IS_SELLER] = isSeller
            prefs[IS_BUYER] = isBuyer
            prefs[IS_STAFF] = isStaff
            prefs[CAN_MANAGE_RATES] = canManageRates
            prefs[CAN_MANAGE_ORDERS] = canManageOrders
            prefs[CAN_MANAGE_BILLING] = canManageBilling
            prefs[CAN_MANAGE_STAFF] = canManageStaff
        }
    }

    suspend fun saveRememberMe(mobile: String, password: String?, remember: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[REMEMBER_ME] = remember
            if (remember) {
                prefs[SAVED_MOBILE] = mobile
                if (password != null) prefs[SAVED_PASSWORD] = password
            } else {
                prefs.remove(SAVED_MOBILE)
                prefs.remove(SAVED_PASSWORD)
            }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            // Clear session tokens but preserve remember_me preferences if enabled
            val remember = prefs[REMEMBER_ME] ?: false
            val savedMobile = prefs[SAVED_MOBILE]
            val savedPassword = prefs[SAVED_PASSWORD]
            prefs.clear()
            if (remember && savedMobile != null) {
                prefs[REMEMBER_ME] = true
                prefs[SAVED_MOBILE] = savedMobile
                if (savedPassword != null) prefs[SAVED_PASSWORD] = savedPassword
            }
        }
    }
}
