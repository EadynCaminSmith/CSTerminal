package com.example.csterminal.seed

import android.content.Context
import android.util.Log
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import org.web3j.crypto.MnemonicUtils
import org.web3j.utils.Numeric
import java.security.MessageDigest

class CheckTrxSeed(private val context: Context) {
    var receiveAddress = ""; companion object {var receiveAddress = ""}

    fun checkTronWallet() {
        try {
            val seedPhrase = generateSeedPhrase.phrase
            if (seedPhrase.isEmpty()) {
                println("SEED PHRASER IS EMPTY")
                Log.e("CSTerminal", "Seed phrase is empty!")
                return
            }
            
            
            println("TRON Seed Phrase: $seedPhrase")
            Log.i("CSTerminal", "TRON Seed Phrase: $seedPhrase")
            
            val seed = MnemonicUtils.generateSeed(seedPhrase, null)
            val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
            
            
            val path = intArrayOf(
                44 or Bip32ECKeyPair.HARDENED_BIT,
                195 or Bip32ECKeyPair.HARDENED_BIT,
                0 or Bip32ECKeyPair.HARDENED_BIT,
                0,
                0
            )
            val tronKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
            val credentials = Credentials.create(tronKeyPair)
            
            val addressBytes = Numeric.hexStringToByteArray(credentials.address)
            val tronAddress = toTronAddress(addressBytes)
            receiveAddress = tronAddress
            CheckTrxSeed.receiveAddress = receiveAddress
            
            println("Generated TRON Address: $tronAddress")
            Log.i("CSTerminal", "Generated TRON Address: $tronAddress")
        } catch (e: Exception) {
            println("Error creating TRON wallet: ${e.message}")
            Log.e("CSTerminal", "Error creating TRON wallet: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun toTronAddress(addressBytes: ByteArray): String {
        val add0x41 = ByteArray(addressBytes.size + 1)
        add0x41[0] = 0x41
        System.arraycopy(addressBytes, 0, add0x41, 1, addressBytes.size)
        val hash0 = sha256(add0x41)
        val hash1 = sha256(hash0)
        val checkSum = ByteArray(4)
        System.arraycopy(hash1, 0, checkSum, 0, 4)
        val addChecksum = ByteArray(add0x41.size + 4)
        System.arraycopy(add0x41, 0, addChecksum, 0, add0x41.size)
        System.arraycopy(checkSum, 0, addChecksum, add0x41.size, 4)
        return Base58.encode(addChecksum)
    }

    private fun sha256(input: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input)
    }
}

object Base58 {
    private val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray()
    private val ENCODED_ZERO = ALPHABET[0]

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        var zeros = 0
        while (zeros < input.size && input[zeros].toInt() == 0) {
            zeros++
        }
        val encoded = CharArray(input.size * 2)
        var outputStart = encoded.size
        val inputCopy = input.copyOf()
        var inputStart = zeros
        while (inputStart < inputCopy.size) {
            encoded[--outputStart] = ALPHABET[divmod(inputCopy, inputStart, 256, 58).toInt()]
            if (inputCopy[inputStart].toInt() == 0) {
                inputStart++
            }
        }
        while (outputStart < encoded.size && encoded[outputStart] == ENCODED_ZERO) {
            outputStart++
        }
        while (--zeros >= 0) {
            encoded[--outputStart] = ENCODED_ZERO
        }
        return String(encoded, outputStart, encoded.size - outputStart)
    }

    private fun divmod(number: ByteArray, firstDigit: Int, base: Int, divisor: Int): Byte {
        var remainder = 0
        for (i in firstDigit until number.size) {
            val digit = number[i].toInt() and 0xFF
            val temp = remainder * base + digit
            number[i] = (temp / divisor).toByte()
            remainder = temp % divisor
        }
        return remainder.toByte()
    }
}
