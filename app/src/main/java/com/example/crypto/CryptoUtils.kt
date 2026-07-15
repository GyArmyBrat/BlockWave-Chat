package com.example.crypto

import android.util.Base64
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    // --- RSA (Asymmetric) for Key Exchange & Digital Signatures ---

    fun generateRsaKeyPair(): Pair<String, String> {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair = keyGen.generateKeyPair()
        
        val privateKeyBase64 = Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP)
        val publicKeyBase64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        
        return Pair(privateKeyBase64, publicKeyBase64)
    }

    private fun getPublicKey(base64PublicKey: String): PublicKey {
        val keyBytes = Base64.decode(base64PublicKey, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(spec)
    }

    private fun getPrivateKey(base64PrivateKey: String): PrivateKey {
        val keyBytes = Base64.decode(base64PrivateKey, Base64.NO_WRAP)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(spec)
    }

    // Encrypt AES key using recipient's RSA Public Key
    fun encryptRsa(plainBytes: ByteArray, recipientPublicKeyBase64: String): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(recipientPublicKeyBase64))
        val encryptedBytes = cipher.doFinal(plainBytes)
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    // Decrypt AES key using our own RSA Private Key
    fun decryptRsa(encryptedKeyBase64: String, myPrivateKeyBase64: String): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, getPrivateKey(myPrivateKeyBase64))
        val encryptedBytes = Base64.decode(encryptedKeyBase64, Base64.NO_WRAP)
        return cipher.doFinal(encryptedBytes)
    }

    // Sign a message hash using our private key
    fun signData(dataHex: String, privateKeyBase64: String): String {
        val privateKey = getPrivateKey(privateKeyBase64)
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(dataHex.toByteArray())
        val signatureBytes = signature.sign()
        return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
    }

    // Verify a message hash signature using sender's public key
    fun verifySignature(dataHex: String, signatureBase64: String, publicKeyBase64: String): Boolean {
        return try {
            val publicKey = getPublicKey(publicKeyBase64)
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            signature.update(dataHex.toByteArray())
            val signatureBytes = Base64.decode(signatureBase64, Base64.NO_WRAP)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }


    // --- AES (Symmetric) for Message Encryption ---

    fun generateAesKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    fun encryptAes(plainText: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding") // ECB is straightforward for demonstration, CBC or GCM with IV is better, but ECB ensures a robust demo with simple byte layout
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    fun decryptAes(encryptedBase64: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun recreateAesKey(keyBytes: ByteArray): SecretKey {
        return SecretKeySpec(keyBytes, "AES")
    }


    // --- SHA-256 for Hashing and Integrity ---

    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(text.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { String.format("%02x", it) }
    }


    // --- Ethereum / Web3 Wallet Simulation ---

    fun generateSimulatedWallet(): WalletCredentials {
        // Generate a random private key (64 hex characters)
        val secureRandom = SecureRandom()
        val privateKeyBytes = ByteArray(32)
        secureRandom.nextBytes(privateKeyBytes)
        val privateKeyHex = privateKeyBytes.joinToString("") { String.format("%02x", it) }

        // Generate a random 40-char hex address
        val addressBytes = ByteArray(20)
        secureRandom.nextBytes(addressBytes)
        val addressHex = "0x" + addressBytes.joinToString("") { String.format("%02x", it) }

        // Initial balance
        val balance = 100.0 + secureRandom.nextDouble() * 50.0

        // Create RSA key pairs for this wallet
        val (rsaPrivate, rsaPublic) = generateRsaKeyPair()

        // Generate seed phrase (12 words)
        val words = listOf(
            "orbit", "velvet", "scout", "matrix", "hybrid", "glance", 
            "canyon", "echo", "pulsar", "quantum", "gravity", "shiver",
            "cosmic", "slate", "ripple", "breeze", "anchor", "shadow"
        )
        val shuffled = words.shuffled()
        val seedPhrase = shuffled.subList(0, 12).joinToString(" ")

        return WalletCredentials(
            walletAddress = addressHex,
            privateKey = privateKeyHex,
            publicKey = rsaPublic,
            privateRsaKey = rsaPrivate,
            balance = balance,
            seedPhrase = seedPhrase
        )
    }
}

data class WalletCredentials(
    val walletAddress: String,
    val privateKey: String,
    val publicKey: String, // RSA Public Key
    val privateRsaKey: String, // RSA Private Key
    val balance: Double, // in POL
    val seedPhrase: String
)
