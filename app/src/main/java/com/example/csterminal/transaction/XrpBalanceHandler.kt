package com.example.csterminal.transaction

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.csterminal.AppDestinations
import com.example.csterminal.MainActivity
import com.example.csterminal.Values
import com.example.csterminal.seed.CheckXrpSeed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.io.entity.StringEntity
import org.json.JSONObject
import org.json.JSONArray
import org.xrpl.xrpl4j.client.XrplClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams
import org.xrpl.xrpl4j.model.transactions.Payment
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount
import org.xrpl.xrpl4j.model.transactions.Address

class XrpBalanceHandler(private val context: Context) {

    suspend fun checkBalance() {
        withContext(Dispatchers.IO) {
            val address = Values.XRPAddress
            if (address.isEmpty()) return@withContext

            HttpClients.createDefault().use { client ->
                while (isActive) {
                    try {
                        val request = HttpPost("https://xrplcluster.com")
                        val body = JSONObject()
                        body.put("method", "account_info")
                        val params = JSONArray()
                        val pObj = JSONObject()
                        pObj.put("account", address)
                        pObj.put("ledger_index", "validated")
                        params.put(pObj)
                        body.put("params", params)
                        
                        request.entity = StringEntity(body.toString(), ContentType.APPLICATION_JSON)
                        
                        val responseString = client.execute(request).use { response ->
                            EntityUtils.toString(response.entity)
                        }

                        val json = JSONObject(responseString)
                        val result = json.optJSONObject("result")
                        
                        if (result != null && result.has("account_data")) {
                            val accountData = result.getJSONObject("account_data")
                            val balanceDrops = accountData.getString("Balance").toLong()
                            val balanceXRP = balanceDrops / 1_000_000.0

                            println("XRP Balance: $balanceXRP XRP")
                            println("Expected: ${MainActivity.ppc} XRP")

                            
                            val txRequest = HttpPost("https://xrplcluster.com")
                            val txBody = JSONObject()
                            txBody.put("method", "account_tx")
                            val txParams = JSONArray()
                            val txP = JSONObject()
                            txP.put("account", address)
                            txP.put("limit", 1)
                            txParams.put(txP)
                            txBody.put("params", txParams)
                            txRequest.entity = StringEntity(txBody.toString(), ContentType.APPLICATION_JSON)
                            
                            val txResponse = client.execute(txRequest).use { EntityUtils.toString(it.entity) }
                            val txJson = JSONObject(txResponse)
                            val txResult = txJson.optJSONObject("result")
                            val transactions = txResult?.optJSONArray("transactions")
                            if (transactions != null && transactions.length() > 0) {
                                val tx = transactions.getJSONObject(0).optJSONObject("tx")
                                if (tx != null) {
                                    MainActivity.transactionId = tx.optString("hash", "")
                                }
                            }

                            val epsilon = 0.000001

                            if (balanceXRP > 0) {
                                if (balanceXRP >= MainActivity.ppc - epsilon) {
                                    
                                    
                                    forwardXrp(balanceDrops)

                                    if (balanceXRP <= MainActivity.ppc + 0.1) {
                                        println("Payment Received!")
                                        Handler(Looper.getMainLooper()).post {
                                            MainActivity.navigateTo?.invoke(AppDestinations.XRPPAID)
                                        }
                                    } else {
                                        println("Overpaid!")
                                        Handler(Looper.getMainLooper()).post {
                                            MainActivity.navigateTo?.invoke(AppDestinations.XRPOVERPAID)
                                        }
                                    }
                                    break 
                                } else {
                                    println("Underpaid!")
                                    Handler(Looper.getMainLooper()).post {
                                        MainActivity.navigateTo?.invoke(AppDestinations.XRPUNDERPAID)
                                    }
                                    break 
                                }
                            }
                        } else {
                            println("Waiting for XRP payment (Account not found or not active yet)...")
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        println("Error checking XRP balance: ${e.message}")
                    }
                    delay(10000L)
                }
            }
        }
    }

    private suspend fun forwardXrp(balanceDrops: Long) {
        withContext(Dispatchers.IO) {
            try {
                val xrplClient = XrplClient("https://xrplcluster.com".toHttpUrl())
                val sourceWallet = CheckXrpSeed.xrpWallet ?: return@withContext
                val destinationAddress = Address.of(Values.CentralXRPAddress)
                
                
                val reserveDrops = 1_000_000L
                val feeDrops = XrpCurrencyAmount.ofDrops(15L) 
                
                val amountToForwardDrops = balanceDrops - reserveDrops - feeDrops.value().toLong()
                
                if (amountToForwardDrops > 0) {
                    val accountInfo = xrplClient.accountInfo(
                        AccountInfoRequestParams.of(sourceWallet.classicAddress())
                    )
                    
                    val payment = Payment.builder()
                        .account(sourceWallet.classicAddress())
                        .fee(feeDrops)
                        .sequence(accountInfo.accountData().sequence())
                        .destination(destinationAddress)
                        .amount(XrpCurrencyAmount.ofDrops(amountToForwardDrops))
                        .signingPublicKey(sourceWallet.publicKey())
                        .build()
                    
                    
                    
                    val submitResult = xrplClient.submit(sourceWallet, payment)
                    
                    if (submitResult.result().startsWith("tes")) {
                        println("XRP funds forwarded successfully.")
                    } else {
                        println("XRP forward failed: ${submitResult.result()}")
                    }
                } else {
                    println("Insufficient XRP balance to forward (less than 1 XRP reserve).")
                }
            } catch (e: Exception) {
                println("Error forwarding XRP: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
