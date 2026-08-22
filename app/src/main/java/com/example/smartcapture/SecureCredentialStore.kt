package com.example.smartcapture

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureCredentialStore(context: Context) {

    data class Credentials(val staffId: String, val password: String)

    private val preferences = context.getSharedPreferences("secure_credentials", Context.MODE_PRIVATE)
    private val keyAlias = "smart_capture_credentials_key"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun hasSavedCredentials(): Boolean = preferences.contains("ciphertext")

    fun save(pin: String, credentials: Credentials) {
        require(pin.length >= 6)
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val payload = "${credentials.staffId}\u0000${credentials.password}"
        val encrypted = encrypt(payload.toByteArray(StandardCharsets.UTF_8))
        preferences.edit()
            .putString("pin_hash", hashPin(pin, salt))
            .putString("pin_salt", encode(salt))
            .putString("ciphertext", encode(encrypted.second))
            .putString("iv", encode(encrypted.first))
            .apply()
    }

    fun load(pin: String): Credentials? {
        return try {
            val salt = preferences.getString("pin_salt", null)?.let(::decode) ?: return null
            val expected = preferences.getString("pin_hash", null) ?: return null
            if (!MessageDigest.isEqual(hashPin(pin, salt).toByteArray(), expected.toByteArray())) return null

            val iv = preferences.getString("iv", null)?.let(::decode) ?: return null
            val ciphertext = preferences.getString("ciphertext", null)?.let(::decode) ?: return null
            val payload = decrypt(iv, ciphertext).toString(StandardCharsets.UTF_8)
            val separator = payload.indexOf('\u0000')
            if (separator <= 0) return null
            Credentials(payload.substring(0, separator), payload.substring(separator + 1))
        } catch (_: Exception) {
            null
        }
    }

    private fun getKey(): SecretKey {
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            keyAlias,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build())
        return generator.generateKey()
    }

    private fun encrypt(value: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        return cipher.iv to cipher.doFinal(value)
    }

    private fun decrypt(iv: ByteArray, value: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(value)
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        var result = pin.toByteArray(StandardCharsets.UTF_8) + salt
        repeat(100_000) {
            result = MessageDigest.getInstance("SHA-256").digest(result)
        }
        return encode(result)
    }

    private fun encode(value: ByteArray): String = Base64.encodeToString(value, Base64.NO_WRAP)
    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)
}
