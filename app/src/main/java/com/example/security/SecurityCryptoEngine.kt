package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedEnvelope(
    val cipherText: String,
    val iv: String,
    val algorithm: String = "AES-256-GCM",
    val integrityHash: String
)

object SecurityCryptoEngine {
    private const val TAG = "RedGameCrypto"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val VAULT_KEY_PREFIX = "redgame_vault_aes256_"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    private fun getOrCreateKeystoreKey(uid: String): SecretKey {
        val alias = VAULT_KEY_PREFIX + sha256Hex(uid).take(16)
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encryptUserVault(uid: String, plaintext: String): EncryptedEnvelope {
        return try {
            val key = getOrCreateKeystoreKey(uid)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val ivBytes = cipher.iv
            val cipherBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            val cipherB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
            val ivB64 = Base64.encodeToString(ivBytes, Base64.NO_WRAP)
            EncryptedEnvelope(
                cipherText = cipherB64,
                iv = ivB64,
                algorithm = "AES-256-GCM",
                integrityHash = sha256Hex("$ivB64:$cipherB64:$uid")
            )
        } catch (e: Exception) {
            Log.w(TAG, "Keystore unavailable, using derived AES-256-GCM key fallback", e)
            encryptWithDerivedKey(uid, "vault_$uid", plaintext)
        }
    }

    fun decryptUserVault(uid: String, cipherTextB64: String, ivB64: String): String {
        return try {
            val key = getOrCreateKeystoreKey(uid)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val ivBytes = Base64.decode(ivB64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(cipherTextB64, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, ivBytes))
            String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            decryptWithDerivedKey(uid, "vault_$uid", cipherTextB64, ivB64)
        }
    }

    fun encryptRoomChatMessage(roomId: String, roomSalt: String, plaintext: String): EncryptedEnvelope {
        return encryptWithDerivedKey(roomId, roomSalt, plaintext)
    }

    fun decryptRoomChatMessage(roomId: String, roomSalt: String, cipherTextB64: String, ivB64: String): String {
        return decryptWithDerivedKey(roomId, roomSalt, cipherTextB64, ivB64)
    }

    private fun deriveAes256Key(contextId: String, salt: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest("RedGame::E2E::$contextId::$salt".toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun encryptWithDerivedKey(contextId: String, salt: String, plaintext: String): EncryptedEnvelope {
        val key = deriveAes256Key(contextId, salt)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ivBytes = cipher.iv
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        val cipherB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        val ivB64 = Base64.encodeToString(ivBytes, Base64.NO_WRAP)
        return EncryptedEnvelope(
            cipherText = cipherB64,
            iv = ivB64,
            algorithm = "AES-256-GCM",
            integrityHash = sha256Hex("$ivB64:$cipherB64:$contextId")
        )
    }

    private fun decryptWithDerivedKey(
        contextId: String,
        salt: String,
        cipherTextB64: String,
        ivB64: String
    ): String {
        return try {
            val key = deriveAes256Key(contextId, salt)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val ivBytes = Base64.decode(ivB64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(cipherTextB64, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, ivBytes))
            String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            "[Pesan terenkripsi tidak dapat didekripsi]"
        }
    }

    fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isHardwareKeystoreActive(uid: String): Boolean {
        return try {
            getOrCreateKeystoreKey(uid)
            true
        } catch (_: Exception) {
            false
        }
    }
}
