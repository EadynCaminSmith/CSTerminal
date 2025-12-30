package com.example.csterminal.transaction

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.csterminal.AppDestinations
import com.example.csterminal.MainActivity
import com.example.csterminal.Values
import com.example.csterminal.seed.generateSeedPhrase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.io.entity.StringEntity
import org.json.JSONArray
import org.json.JSONObject
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import org.web3j.crypto.MnemonicUtils
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric

class TrxBalanceHandler(private val context: Context) {

    suspend fun checkBalance(currency: String, passFees: Boolean, rateHandler: RateHandler) {
        MainActivity.transactionId = ""
        MainActivity.confirmations = 0
        withContext(Dispatchers.IO) {
            val address = Values.TRXAdress
            if (address.isEmpty()) return@withContext

            try {
                HttpClients.createDefault().use { client ->
                    while (isActive) {
                        try {
                            val balanceSUN = getTrxAddressBalance(client, address)
                            val balanceTRX = balanceSUN / 1_000_000.0

                            Log.i("CSTerminal", "TRX Balance: $balanceTRX TRX for address: $address")
                            println("TRX Balance: $balanceTRX TRX")

                            val epsilon = 0.000001
                            val diff = balanceTRX - MainActivity.ppc

                            if (balanceTRX > 0) {
                                val txData = getLatestTransactionData(client, address)
                                val txHash = txData.optString("hash", "")
                                val confirmations = txData.optInt("confirmations", 0)
                                
                                if (txHash.isNotEmpty()) {
                                    MainActivity.transactionId = txHash
                                    Log.i("CSTerminal", "TRX Transaction: $txHash, Confirmations: $confirmations")
                                }

                                if (balanceTRX >= MainActivity.ppc || Math.abs(diff) < epsilon) {
                                    
                                    
                                    
                                    if (confirmations >= 19) {
                                        Log.i("CSTerminal", "19 Confirmations reached. Forwarding funds to central wallet...")
                                        
                                        var feePaid = 0.0
                                        if (Values.CentralTrxAddress.isNotEmpty()) {
                                            val forwardedAmount = forwardTrxFunds(address, Values.CentralTrxAddress)
                                            if (forwardedAmount > 0) {
                                                 feePaid = balanceTRX - forwardedAmount
                                            }
                                        }
                                        
                                        val rate = rateHandler.fetchMarketRate("TRX", currency) ?: 0.0
                                        val feeFiat = feePaid * rate
                                        MainActivity.lastFeePaidFiat = String.format("%.2f", feeFiat)
                                        MainActivity.lastFeePassedToCustomer = passFees

                                        Handler(Looper.getMainLooper()).post {
                                            if (Math.abs(diff) < epsilon) {
                                                MainActivity.navigateTo?.invoke(AppDestinations.TRXPAID)
                                            } else {
                                                MainActivity.navigateTo?.invoke(AppDestinations.TRXOVERPAID)
                                            }
                                        }
                                        break
                                    } else {
                                        Log.i("CSTerminal", "Waiting for 19 confirmations... Current: $confirmations")
                                        MainActivity.confirmations = confirmations
                                    }
                                } else if (balanceTRX > 0 && balanceTRX < MainActivity.ppc) {
                                    Log.i("CSTerminal", "TRX Underpaid! Balance: $balanceTRX, Required: ${MainActivity.ppc}")
                                    Handler(Looper.getMainLooper()).post {
                                        MainActivity.navigateTo?.invoke(AppDestinations.TRXUNDERPAID)
                                    }
                                    break
                                }
                            } else {
                                Log.i("CSTerminal", "Waiting for TRX payment on address: $address")
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            Log.e("CSTerminal", "Error checking TRX balance: ${e.message}")
                            e.printStackTrace()
                        }
                        delay(10000L)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("CSTerminal", "Error in TRX balance handler: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun forwardTrxFunds(fromAddress: String, toAddress: String): Double {
        return withContext(Dispatchers.IO) {
            try {
                Log.i("CSTerminal", "Initiating real TRX forward from $fromAddress to $toAddress")
                
                
                val seedPhrase = generateSeedPhrase.phrase
                if (seedPhrase.isEmpty()) {
                    Log.e("CSTerminal", "Forward failed: Seed phrase missing")
                    return@withContext 0.0
                }
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
                val ecKeyPair = credentials.ecKeyPair

                
                HttpClients.createDefault().use { client ->
                    val createRequest = HttpPost("https://api.trongrid.io/wallet/createtransaction")
                    val jsonBody = JSONObject()
                    jsonBody.put("owner_address", fromAddress)
                    jsonBody.put("to_address", toAddress)
                    
                    val balanceSUN = getTrxAddressBalance(client, fromAddress)
                    
                    val amountToSendSUN = balanceSUN - 1_100_000 
                    
                    if (amountToSendSUN <= 0) {
                        Log.e("CSTerminal", "Forward failed: Insufficient balance for fee ($balanceSUN SUN)")
                        return@withContext 0.0
                    }
                    
                    jsonBody.put("amount", amountToSendSUN)
                    jsonBody.put("visible", true) 
                    
                    createRequest.entity = StringEntity(jsonBody.toString())
                    val txResponse = client.execute(createRequest).use { response ->
                        JSONObject(EntityUtils.toString(response.entity))
                    }

                    if (!txResponse.has("txID")) {
                        Log.e("CSTerminal", "Failed to create TRX transaction: $txResponse")
                        return@withContext 0.0
                    }

                    val txID = txResponse.getString("txID")
                    
                    
                    val sigData = Sign.signMessage(Numeric.hexStringToByteArray(txID), ecKeyPair, false)
                    val r = sigData.r
                    val s = sigData.s
                    val v = sigData.v
                    
                    val signature = ByteArray(65)
                    System.arraycopy(r, 0, signature, 0, 32)
                    System.arraycopy(s, 0, signature, 32, 32)
                    signature[64] = (v[0].toInt()).toByte() 
                    
                    val sigHex = Numeric.toHexStringNoPrefix(signature)
                    val signatures = JSONArray()
                    signatures.put(sigHex)
                    txResponse.put("signature", signatures)

                    
                    val broadcastRequest = HttpPost("https://api.trongrid.io/wallet/broadcasttransaction")
                    broadcastRequest.entity = StringEntity(txResponse.toString())
                    val broadcastResponse = client.execute(broadcastRequest).use { response ->
                        JSONObject(EntityUtils.toString(response.entity))
                    }

                    if (broadcastResponse.optBoolean("result", false)) {
                        Log.i("CSTerminal", "TRX Forwarded successfully! TxID: $txID")
                        return@withContext amountToSendSUN / 1_000_000.0
                    } else {
                        Log.e("CSTerminal", "TRX Broadcast failed: $broadcastResponse")
                        return@withContext 0.0
                    }
                }
            } catch (e: Exception) {
                Log.e("CSTerminal", "Exception during TRX forwarding: ${e.message}")
                e.printStackTrace()
                return@withContext 0.0
            }
        }
    }

    private suspend fun getTrxAddressBalance(client: org.apache.hc.client5.http.impl.classic.CloseableHttpClient, address: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                val request = HttpGet("https://apilist.tronscan.org/api/account?address=$address")
                client.execute(request).use { response ->
                    val entity = response.entity
                    val responseString = EntityUtils.toString(entity)
                    EntityUtils.consume(entity)
                    val jsonObject = JSONObject(responseString)
                    jsonObject.optLong("balance", 0L)
                }
            } catch (e: Exception) {
                Log.e("CSTerminal", "TRX API Balance Error: $e")
                0L
            }
        }
    }

    private suspend fun getLatestTransactionData(client: org.apache.hc.client5.http.impl.classic.CloseableHttpClient, address: String): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                val request = HttpGet("https://apilist.tronscan.org/api/transaction?sort=-timestamp&count=true&limit=1&start=0&address=$address")
                client.execute(request).use { response ->
                    val entity = response.entity
                    val responseString = EntityUtils.toString(entity)
                    EntityUtils.consume(entity)
                    val jsonObject = JSONObject(responseString)
                    val data = jsonObject.optJSONArray("data")
                    if (data != null && data.length() > 0) {
                        val tx = data.getJSONObject(0)
                        val txBlock = tx.optLong("block", 0L)
                        if (txBlock > 0) {
                            val currentBlock = getCurrentBlockHeight(client)
                            if (currentBlock > 0) {
                                val confirmations = (currentBlock - txBlock + 1).toInt()
                                tx.put("confirmations", confirmations)
                            }
                        }
                        return@withContext tx
                    }
                }
            } catch (e: Exception) {
                Log.e("CSTerminal", "TRX Transaction API Error: $e")
            }
            JSONObject()
        }
    }

    private suspend fun getCurrentBlockHeight(client: org.apache.hc.client5.http.impl.classic.CloseableHttpClient): Long {
        return withContext(Dispatchers.IO) {
            try {
                val request = HttpGet("https://apilist.tronscan.org/api/block?limit=1&start=0&sort=-number")
                client.execute(request).use { response ->
                    val entity = response.entity
                    val responseString = EntityUtils.toString(entity)
                    EntityUtils.consume(entity)
                    val jsonObject = JSONObject(responseString)
                    val data = jsonObject.optJSONArray("data")
                    if (data != null && data.length() > 0) {
                        return@withContext data.getJSONObject(0).getLong("number")
                    }
                }
            } catch (e: Exception) {
                Log.e("CSTerminal", "TRX Block API Error: $e")
            }
            0L
        }
    }
}
