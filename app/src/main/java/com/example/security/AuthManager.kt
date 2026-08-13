package com.example.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class AuthManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isPinSetup: Boolean
        get() = prefs.contains(KEY_PIN_HASH)

    val isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    val autoLockTimeoutMinutes: Int
        get() = prefs.getInt(KEY_AUTO_LOCK_TIMEOUT, 1)

    fun setMasterPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    fun verifyMasterPin(pin: String): Boolean {
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return savedHash == hashPin(pin)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun setAutoLockTimeout(minutes: Int) {
        prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT, minutes).apply()
    }

    fun clearSecurityData() {
        prefs.edit().clear().apply()
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "vault_security_prefs"
        private const val KEY_PIN_HASH = "key_master_pin_hash"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "key_auto_lock_timeout"
    }
}
