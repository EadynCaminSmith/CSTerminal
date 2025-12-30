package com.example.csterminal.seed

import android.content.Context
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bitcoinj.wallet.DeterministicSeed
import java.lang.Exception
import java.math.BigInteger

class CheckSolanaSeed(private val context: Context) {
    var receiveAddress = ""; companion object {
        var receiveAddress = ""
        var privateKey: ByteArray? = null
        
        private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

        fun encodeBase58(input: ByteArray): String {
            var value = BigInteger(1, input)
            val sb = StringBuilder()
            while (value > BigInteger.ZERO) {
                val mod = value.remainder(BigInteger.valueOf(58L))
                sb.append(ALPHABET[mod.toInt()])
                value = value.divide(BigInteger.valueOf(58L))
            }
            for (b in input) {
                if (b.toInt() == 0) sb.append(ALPHABET[0]) else break
            }
            return sb.reverse().toString()
        }
    }

    fun checkSolanaWallet() {
        try {
            val seedPhrase = generateSeedPhrase.phrase
            val seed = DeterministicSeed(seedPhrase, null, "", System.currentTimeMillis() / 1000).seedBytes
            
            
            val privKeyParams = Ed25519PrivateKeyParameters(seed, 0)
            val pubKeyParams = privKeyParams.generatePublicKey()
            
            val publicKeyBytes = pubKeyParams.encoded
            privateKey = privKeyParams.encoded + publicKeyBytes
            
            receiveAddress = encodeBase58(publicKeyBytes)
            
            CheckSolanaSeed.receiveAddress = receiveAddress
            CheckSolanaSeed.privateKey = privateKey
            
            println("Solana Address: $receiveAddress")

        } catch (e: Exception) {
            println("Error creating Solana wallet: ${e.message}")
            e.printStackTrace()
        }
    }
}
