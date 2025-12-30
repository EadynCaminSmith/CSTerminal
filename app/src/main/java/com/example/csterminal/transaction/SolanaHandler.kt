package com.example.csterminal.transaction

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.csterminal.AppDestinations
import com.example.csterminal.MainActivity
import com.example.csterminal.Values
import com.example.csterminal.seed.CheckSolanaSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters

class SolanaHandler(private val context: Context) {
    private val client = OkHttpClient()
    private val rpcUrl = "https://api.mainnet-beta.solana.com"

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    suspend fun checkBalance(currency: String, passFees: Boolean, rateHandler: RateHandler) {
        MainActivity.transactionId = ""
        MainActivity.confirmations = 0
        withContext(Dispatchers.IO) {
            val address = CheckSolanaSeed.receiveAddress
            if (address.isEmpty()) return@withContext

            while (isActive) {
                try {
                    val balanceLamports = getBalance(address)
                    val balanceSOL = balanceLamports / 1_000_000_000.0
                    val expectedSOL = MainActivity.ppc
                    val tolerance = expectedSOL * 0.0001

                    if (balanceSOL > 0) {
                        if (balanceSOL >= expectedSOL - tolerance) {
                            
                            val txId = forwardFunds(balanceLamports)
                            if (txId != null) {
                                MainActivity.transactionId = txId
                                
                                
                                val feeLamports = 5000 
                                val feeSOL = feeLamports / 1_000_000_000.0
                                val rate = rateHandler.fetchMarketRate("SOL", currency) ?: 0.0
                                val feeFiat = feeSOL * rate
                                
                                MainActivity.lastFeePaidFiat = String.format("%.2f", feeFiat)
                                MainActivity.lastFeePassedToCustomer = passFees
                                
                                val dest = if (balanceSOL > expectedSOL + tolerance)
                                    AppDestinations.SOLOVERPAID else AppDestinations.SOLPAID
                                notifyUI(dest)
                            } else {
                                Log.e("SolanaHandler", "Failed to forward funds.")
                            }
                            break
                        } else {
                            notifyUI(AppDestinations.SOLUNDERPAID)
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SolanaHandler", "Error checking balance: ${e.message}")
                }
                delay(5000L)
            }
        }
    }

    private fun getBalance(address: String): Long {
        try {
            val json = """{"jsonrpc":"2.0","id":1,"method":"getBalance","params":["$address"]}"""
            val request = Request.Builder().url(rpcUrl)
                .post(json.toRequestBody("application/json".toMediaType())).build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string() ?: return 0L
                return JSONObject(body).optJSONObject("result")?.optLong("value") ?: 0L
            }
        } catch (e: Exception) {
            return 0L
        }
    }

    private fun getLatestBlockhash(): String? {
        try {
            val json = """{"jsonrpc":"2.0","id":1,"method":"getLatestBlockhash"}"""
            val request = Request.Builder().url(rpcUrl)
                .post(json.toRequestBody("application/json".toMediaType())).build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string() ?: return null
                return JSONObject(body).optJSONObject("result")?.optJSONObject("value")?.optString("blockhash")
            }
        } catch (e: Exception) {
            Log.e("SolanaHandler", "Error getting blockhash: ${e.message}")
            return null
        }
    }

    private fun sendTransaction(signedTx: String): String? {
        try {
            val json = """{"jsonrpc":"2.0","id":1,"method":"sendTransaction","params":["$signedTx", {"encoding": "base64"}]}"""
            val request = Request.Builder().url(rpcUrl)
                .post(json.toRequestBody("application/json".toMediaType())).build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string() ?: return null
                val result = JSONObject(body).optString("result")
                if (result.isNotEmpty()) {
                    return result
                } else {
                    Log.e("SolanaHandler", "Send transaction error: $body")
                    return null
                }
            }
        } catch (e: Exception) {
            Log.e("SolanaHandler", "Error sending transaction: ${e.message}")
            return null
        }
    }


    private fun forwardFunds(lamports: Long): String? {
        Log.d("SolanaHandler", "Attempting to forward $lamports lamports.")
        val fromAddress = CheckSolanaSeed.receiveAddress
        val privateKeyBytes = CheckSolanaSeed.privateKey ?: return null
        val toAddress = Values.CentralSOLAddress

        val blockhash = getLatestBlockhash() ?: return null

        val fee = 5000 
        val amountToSend = lamports - fee
        if (amountToSend <= 0) {
            Log.e("SolanaHandler", "Balance not high enough to cover fee.")
            return null
        }

        val message = createTransactionMessage(fromAddress, toAddress, amountToSend, blockhash)

        try {
            val privateKey = Ed25519PrivateKeyParameters(privateKeyBytes, 0)
            val signer = Ed25519Signer()
            signer.init(true, privateKey)
            signer.update(message, 0, message.size)
            val signedBytes = signer.generateSignature()

            val transaction = buildTransaction(message, signedBytes)

            val encodedTx = Base64.getEncoder().encodeToString(transaction)

            return sendTransaction(encodedTx)
        } catch (e: Exception) {
            Log.e("SolanaHandler", "Error signing or sending transaction", e)
            return null
        }
    }

    private fun encodeLength(length: Int): ByteArray {
        val out = mutableListOf<Byte>()
        var remLen = length
        while (true) {
            var elem = remLen and 0x7F
            remLen = remLen ushr 7
            if (remLen == 0) {
                out.add(elem.toByte())
                break
            } else {
                elem = elem or 0x80
                out.add(elem.toByte())
            }
        }
        return out.toByteArray()
    }

    private fun createTransactionMessage(fromAddress: String, toAddress: String, lamports: Long, recentBlockhash: String): ByteArray {
        val fromBytes = Base58.decode(fromAddress)
        val toBytes = Base58.decode(toAddress)
        val systemProgramBytes = Base58.decode("11111111111111111111111111111111")
        val blockhashBytes = Base58.decode(recentBlockhash)

        val accountKeys = listOf(fromBytes, toBytes, systemProgramBytes)
        
        
        val instructionData = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(2) 
            .putLong(lamports)
            .array()
        
        
        val header = byteArrayOf(
            1, 
            0, 
            1  
        )

        val messageBuffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN)

        messageBuffer.put(header)
        
        
        messageBuffer.put(encodeLength(accountKeys.size))
        accountKeys.forEach { messageBuffer.put(it) }

        
        messageBuffer.put(blockhashBytes)
        
        
        messageBuffer.put(encodeLength(1)) 

        
        messageBuffer.put(2.toByte()) 
        messageBuffer.put(encodeLength(2)) 
        messageBuffer.put(0.toByte()) 
        messageBuffer.put(1.toByte()) 
        messageBuffer.put(encodeLength(instructionData.size))
        messageBuffer.put(instructionData)
        
        val finalMessage = ByteArray(messageBuffer.position())
        messageBuffer.rewind()
        messageBuffer.get(finalMessage)
        
        return finalMessage
    }

    private fun buildTransaction(message: ByteArray, signature: ByteArray): ByteArray {
        val signatureCount = encodeLength(1)
        val transaction = ByteArray(signatureCount.size + signature.size + message.size)
        
        var offset = 0
        System.arraycopy(signatureCount, 0, transaction, offset, signatureCount.size)
        offset += signatureCount.size
        System.arraycopy(signature, 0, transaction, offset, signature.size)
        offset += signature.size
        System.arraycopy(message, 0, transaction, offset, message.size)
        
        return transaction
    }


    private fun notifyUI(destination: AppDestinations) {
        Handler(Looper.getMainLooper()).post {
            MainActivity.navigateTo?.invoke(destination)
        }
    }
}

object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val ALPHABET_MAP = IntArray(128)

    init {
        for (i in ALPHABET_MAP.indices) {
            ALPHABET_MAP[i] = -1
        }
        for (i in ALPHABET.indices) {
            ALPHABET_MAP[ALPHABET[i].code] = i
        }
    }

    fun decode(input: String): ByteArray {
        val input58 = ByteArray(input.length)
        for (i in input.indices) {
            val c = input[i]
            val digit = if (c.code < 128) ALPHABET_MAP[c.code] else -1
            if (digit < 0) {
                throw IllegalArgumentException("Invalid character in Base58 input")
            }
            input58[i] = digit.toByte()
        }

        var zeros = 0
        while (zeros < input58.size && input58[zeros].toInt() == 0) {
            zeros++
        }

        val decoded = ByteArray(input.length)
        var outputStart = decoded.size
        var inputStart = zeros
        while (inputStart < input58.size) {
            decoded[--outputStart] = divmod(input58, inputStart, 58, 256).toByte()
            if (input58[inputStart].toInt() == 0) {
                inputStart++
            }
        }

        while (outputStart < decoded.size && decoded[outputStart].toInt() == 0) {
            outputStart++
        }

        val result = ByteArray(zeros + (decoded.size - outputStart))
        System.arraycopy(decoded, outputStart, result, zeros, decoded.size - outputStart)
        return result
    }

    private fun divmod(number: ByteArray, firstDigit: Int, base: Int, divisor: Int): Int {
        var remainder = 0
        for (i in firstDigit until number.size) {
            val digit = number[i].toInt() and 0xFF
            val temp = remainder * base + digit
            number[i] = (temp / divisor).toByte()
            remainder = temp % divisor
        }
        return remainder
    }
}
