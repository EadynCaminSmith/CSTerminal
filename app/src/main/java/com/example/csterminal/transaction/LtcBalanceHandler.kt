package com.example.csterminal.transaction

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.csterminal.AppDestinations
import com.example.csterminal.MainActivity
import com.example.csterminal.Values
import com.example.csterminal.seed.CheckLtcSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.bitcoinj.core.*
import org.bitcoinj.params.MainNetParams
import org.json.JSONObject
import org.bitcoindevkit.*
import org.bitcoinj.script.ScriptBuilder

class LtcBalanceHandler(private val context: Context) {

    suspend fun checkBalance() {
        withContext(Dispatchers.IO) {
            val ltcReceiveAddress = CheckLtcSeed.receiveAddress
            println("Address: $ltcReceiveAddress")

            HttpClients.createDefault().use { client ->
                while (isActive) {
                    try {
                        val (balanceLitoshis, isConfirmed) = getAddressBalance(client, ltcReceiveAddress)
                        val balanceLTC = balanceLitoshis / 100_000_000.0

                        println("Balance: $balanceLTC LTC")
                        println("Confirmed: $isConfirmed")
                        println("Expected: ${MainActivity.ppc} LTC")

                        val paymentTolerance = MainActivity.ppc * 0.0001

                        if (balanceLTC > 0) {
                            if (balanceLTC >= MainActivity.ppc - paymentTolerance) {
                                if (isConfirmed) {
                                    if (Values.CentralLTCAddress.isNotEmpty() && CheckLtcSeed.privateKey != null) {
                                        
                                        forwardFundsBDK(CheckLtcSeed.privateKey!!, Values.CentralLTCAddress)
                                    }

                                    if (balanceLTC > MainActivity.ppc + paymentTolerance) {
                                        println("Overpaid!")
                                        Handler(Looper.getMainLooper()).post {
                                            MainActivity.navigateTo?.invoke(AppDestinations.LTCOVERPAID)
                                        }
                                    } else {
                                        println("Payment Received and Confirmed!")
                                        Handler(Looper.getMainLooper()).post {
                                            MainActivity.navigateTo?.invoke(AppDestinations.LTCPAID)
                                        }
                                    }
                                    break
                                } else {
                                    println("Payment detected but unconfirmed. Waiting for confirmation...")
                                }
                            } else {
                                println("Underpaid!")
                                Handler(Looper.getMainLooper()).post {
                                    MainActivity.navigateTo?.invoke(AppDestinations.LTCUNDERPAID)
                                }
                                break
                            }
                        } else {
                            println("Waiting for payment...")
                        }
                    } catch (e: Exception) {
                        println("Error checking balance: ${e.message}")
                        e.printStackTrace()
                    }
                    delay(5000L)
                }
            }
        }
    }

    private suspend fun forwardFundsBDK(privateKey: ECKey, toAddress: String) {
        println("Attempting to forward funds to $toAddress using BDK")
        try {
            val ltcParams = object : MainNetParams() {
                override fun getAddressHeader() = 48
                override fun getP2SHHeader() = 50
            }
            
            
            val wif = privateKey.getPrivateKeyEncoded(ltcParams).toString()
            val descriptorString = "sh(wpkh($wif))"
            
            val descriptor = Descriptor(descriptorString, Network.BITCOIN)
            
            
            val wallet = Wallet(
                descriptor,
                null,
                Network.BITCOIN,
                DatabaseConfig.Memory
            )
            
            
            val blockchain = Blockchain(
                BlockchainConfig.Esplora(
                    EsploraConfig("https://litecoinspace.org/api", null, 5u, 5u, null)
                )
            )
            
            
            println("BDK: Syncing with blockchain...")
            wallet.sync(blockchain, null)
            
            val balance = wallet.getBalance()
            println("BDK: Total balance = ${balance.total} satoshis")
            
            if (balance.total == 0uL) {
                println("BDK: No confirmed balance found to forward.")
                return
            }
            
            
            val targetAddress = LegacyAddress.fromString(ltcParams, toAddress)
            
            val scriptPubKey = ScriptBuilder.createOutputScript(targetAddress)
            
            val bdkScript = org.bitcoindevkit.Script(scriptPubKey.program.map { b: Byte -> b.toUByte() })

            
            val txBuilder = TxBuilder()
                .drainWallet()
                .drainTo(bdkScript)
                .feeRate(1.0f) 
                
            val result = txBuilder.finish(wallet)
            val psbt = result.psbt
            
            
            val signed = wallet.sign(psbt, null)
            if (signed) {
                val tx = psbt.extractTx()
                blockchain.broadcast(tx)
                println("BDK: Forwarding successful! TxID: ${tx.txid()}")
            } else {
                println("BDK: Transaction signing failed.")
            }

        } catch (e: Exception) {
            println("BDK Error during forwarding: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun getAddressBalance(client: CloseableHttpClient, address: String): Pair<Long, Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = Values.BlockchairAPIKey
                val url = "https://api.blockchair.com/litecoin/dashboards/address/$address?key=$apiKey"
                val request = HttpGet(url)
                client.execute(request).use { response ->
                    val entity = response.entity
                    val responseString = EntityUtils.toString(entity)
                    EntityUtils.consume(entity)

                    val jsonObject = JSONObject(responseString)
                    val data = jsonObject.optJSONObject("data")

                    if (data == null) {
                        return@withContext Pair(0L, false)
                    }

                    val addressData = data.getJSONObject(address)
                    val addressInfo = addressData.getJSONObject("address")
                    val finalBalance = addressInfo.getLong("balance")

                    val isConfirmed = finalBalance > 0

                    if (isConfirmed) {
                        val transactions = addressData.getJSONArray("transactions")
                        if (transactions.length() > 0) {
                            MainActivity.transactionId = transactions.getString(0)
                        }
                    }

                    Pair(finalBalance, isConfirmed)
                }
            } catch (e: Exception) {
                println("API Request Error: $e")
                Pair(0L, false)
            }
        }
    }
}
