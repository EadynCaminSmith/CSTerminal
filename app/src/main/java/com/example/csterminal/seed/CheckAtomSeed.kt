package com.example.csterminal.seed

import android.content.Context
import android.util.Log
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.MnemonicUtils
import org.web3j.crypto.Hash
import java.util.Arrays

class CheckAtomSeed(private val context: Context) {
    var receiveAddress = ""; companion object {var receiveAddress = ""}

    fun checkCosmosWallet() {
        try {
            val seedPhrase = generateSeedPhrase.phrase
            if (seedPhrase.isEmpty()) {
                Log.e("CSTerminal", "Seed phrase is empty!")
                return
            }

            val seed = MnemonicUtils.generateSeed(seedPhrase, null)
            val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)

            
            val path = intArrayOf(
                44 or Bip32ECKeyPair.HARDENED_BIT,
                118 or Bip32ECKeyPair.HARDENED_BIT,
                0 or Bip32ECKeyPair.HARDENED_BIT,
                0,
                0
            )
            val atomKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
            
            
            val pubKeyBI = atomKeyPair.publicKey
            val xBytes = pubKeyBI.toByteArray()
            val xClean = if (xBytes.size > 32) {
                Arrays.copyOfRange(xBytes, xBytes.size - 32, xBytes.size)
            } else if (xBytes.size < 32) {
                val padded = ByteArray(32)
                System.arraycopy(xBytes, 0, padded, 32 - xBytes.size, xBytes.size)
                padded
            } else {
                xBytes
            }
            
            val compressedPubKey = ByteArray(33)
            compressedPubKey[0] = if (pubKeyBI.testBit(0)) 0x03.toByte() else 0x02.toByte()
            System.arraycopy(xClean, 0, compressedPubKey, 1, 32)

            
            
            val hash160 = Hash.sha256hash160(compressedPubKey)
            
            val cosmosAddress = Bech32.encode("cosmos", convertBits(hash160, 8, 5, true))
            receiveAddress = cosmosAddress
            CheckAtomSeed.receiveAddress = cosmosAddress

            Log.i("CSTerminal", "Generated ATOM Address: $cosmosAddress")
        } catch (e: Exception) {
            Log.e("CSTerminal", "Error creating ATOM wallet: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): ByteArray {
        var acc = 0
        var bits = 0
        val result = mutableListOf<Byte>()
        val maxv = (1 shl toBits) - 1
        for (value in data) {
            val b = value.toInt() and 0xff
            acc = (acc shl fromBits) or b
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                result.add(((acc shr bits) and maxv).toByte())
            }
        }
        if (pad) {
            if (bits > 0) {
                result.add(((acc shl (toBits - bits)) and maxv).toByte())
            }
        } else if (bits >= fromBits || ((acc shl (toBits - bits)) and maxv) != 0) {
            throw Exception("Could not convert bits")
        }
        return result.toByteArray()
    }
}

object Bech32 {
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    fun encode(hrp: String, values: ByteArray): String {
        val checksum = createChecksum(hrp, values)
        val combined = ByteArray(values.size + checksum.size)
        System.arraycopy(values, 0, combined, 0, values.size)
        System.arraycopy(checksum, 0, combined, values.size, checksum.size)
        val sb = StringBuilder(hrp.length + 1 + combined.size)
        sb.append(hrp)
        sb.append('1')
        for (b in combined) {
            sb.append(CHARSET[b.toInt() and 0xff])
        }
        return sb.toString()
    }

    private fun createChecksum(hrp: String, values: ByteArray): ByteArray {
        val hrpExpanded = expandHrp(hrp)
        val enc = ByteArray(hrpExpanded.size + values.size + 6)
        System.arraycopy(hrpExpanded, 0, enc, 0, hrpExpanded.size)
        System.arraycopy(values, 0, enc, hrpExpanded.size, values.size)
        val mod = polymod(enc) xor 1
        val result = ByteArray(6)
        for (i in 0 until 6) {
            result[i] = ((mod shr (5 * (5 - i))) and 31).toByte()
        }
        return result
    }

    private fun expandHrp(hrp: String): ByteArray {
        val res = ByteArray(hrp.length * 2 + 1)
        for (i in hrp.indices) {
            val c = hrp[i].toInt()
            res[i] = (c shr 5).toByte()
            res[i + hrp.length + 1] = (c and 31).toByte()
        }
        res[hrp.length] = 0
        return res
    }

    private fun polymod(values: ByteArray): Int {
        var chk = 1
        val generators = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
        for (v in values) {
            val top = chk shr 25
            chk = (chk and 0x1ffffff shl 5) xor (v.toInt() and 0xff)
            for (i in 0 until 5) {
                if ((top shr i and 1) == 1) {
                    chk = chk xor generators[i]
                }
            }
        }
        return chk
    }
}
