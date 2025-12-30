package com.example.csterminal.transaction

import android.content.Context
import android.os.Handler
import android.os.Looper
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
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.bitcoindevkit.Address
import org.bitcoindevkit.AddressIndex
import org.bitcoindevkit.Descriptor
import org.bitcoindevkit.DescriptorSecretKey
import org.bitcoindevkit.KeychainKind
import org.bitcoindevkit.Mnemonic
import org.bitcoindevkit.Network
import org.bitcoindevkit.Wallet
import org.bitcoindevkit.DatabaseConfig
import org.bitcoindevkit.TxBuilder
import org.json.JSONObject

class BalanceHandler (private val context: Context){

    suspend fun checkBalance() {
        withContext(Dispatchers.IO) {
            val seedPhrase = generateSeedPhrase.phrase
            if (seedPhrase.isEmpty()) return@withContext

            try {
                val mnemonic = Mnemonic.fromString(seedPhrase)
                val descriptorSecretKey = DescriptorSecretKey(Network.BITCOIN, mnemonic, null)

                
                val externalDescriptor = Descriptor.newBip84(descriptorSecretKey, KeychainKind.EXTERNAL, Network.BITCOIN)
                val internalDescriptor = Descriptor.newBip84(descriptorSecretKey, KeychainKind.INTERNAL, Network.BITCOIN)

                val wallet = Wallet(
                    externalDescriptor,
                    internalDescriptor,
                    Network.BITCOIN,
                    DatabaseConfig.Memory
                )

                
                val btcReceiveAddress = wallet.getAddress(AddressIndex.New).address.asString()
                println("Address: $btcReceiveAddress")

                HttpClients.createDefault().use { client ->
                    while (isActive) {
                        try {
                            
                            val (balanceSatoshis, isConfirmed) = getAddressBalance(client, btcReceiveAddress)
                            val balanceBTC = balanceSatoshis / 100_000_000.0

                            println("Balance: $balanceBTC BTC")
                            println("Confirmed: $isConfirmed")
                            println("Expected: ${MainActivity.ppc} BTC")

                            val epsilon = 0.00000001

                            if (balanceBTC > 0 && (balanceBTC >= MainActivity.ppc || Math.abs(balanceBTC - MainActivity.ppc) < epsilon)) {
                                

                                if (isConfirmed) {
                                    
                                    if (Values.CentralBTCAddress.isNotEmpty()) {
                                        forwardFunds(wallet, Values.CentralBTCAddress)
                                    }

                                    if (Math.abs(balanceBTC - MainActivity.ppc) < epsilon){
                                        println("Payment Received and Confirmed!")
                                        Handler(Looper.getMainLooper()).post {
                                            MainActivity.navigateTo?.invoke(AppDestinations.BTCPAID)
                                        }
                                    } else {
                                        println("Overpaid!")
                                        Handler(Looper.getMainLooper()).post {
                                            MainActivity.navigateTo?.invoke(AppDestinations.BTCOVERPAID)
                                        }
                                    }
                                    break
                                } else {
                                    println("Payment detected but unconfirmed. Waiting for confirmation...")
                                }

                            } else if (balanceBTC > 0 && balanceBTC < MainActivity.ppc) {
                                println("Underpaid!")
                                
                                Handler(Looper.getMainLooper()).post {
                                    MainActivity.navigateTo?.invoke(AppDestinations.BTCUNDERPAID)
                                }
                                break
                            } else {
                                println("Waiting for payment...")
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            println("Error checking balance: ${e.message}")
                            e.printStackTrace()
                        }
                        delay(5000L)
                    }
                }
            } catch (e: Exception) {
                 if (e is CancellationException) throw e
                 println("Error initializing wallet or checking balance: ${e.message}")
                 e.printStackTrace()
            }
        }
    }

    private suspend fun forwardFunds(wallet: Wallet, destinationAddress: String) {
        withContext(Dispatchers.IO) {
            var attempts = 0
            val maxAttempts = 3
            while (attempts < maxAttempts) {
                try {
                    
                    
                    val blockchainConfig = org.bitcoindevkit.BlockchainConfig.Electrum(
                        org.bitcoindevkit.ElectrumConfig("ssl://electrum.blockstream.info:50002", null, 5u, null, 20uL, true)
                    )
                    val blockchain = org.bitcoindevkit.Blockchain(blockchainConfig)

                    
                    wallet.sync(blockchain, null)

                    val balance = wallet.getBalance()
                    println("Wallet Confirmed Balance: ${balance.confirmed}")

                    if (balance.confirmed > 0u) {
                         val address = Address(destinationAddress)
                         val script = address.scriptPubkey()

                         
                         val result = TxBuilder()
                            .drainWallet()
                            .drainTo(script)
                            .feeRate(5.0f)
                            .finish(wallet)

                         val psbt = result.psbt

                         val finalized = wallet.sign(psbt, null)
                         if (finalized) {
                             val tx = psbt.extractTx()
                             blockchain.broadcast(tx)
                             println("Funds forwarded to $destinationAddress. TxId: ${tx.txid()}")
                         } else {
                             println("Transaction not finalized. Cannot broadcast.")
                         }
                    } else {
                        println("No confirmed funds to forward yet.")
                    }
                    
                    break
                } catch (e: Exception) {
                    attempts++
                    println("Error forwarding funds (Attempt $attempts/$maxAttempts): ${e.message}")
                    e.printStackTrace()
                    if (attempts < maxAttempts) {
                         delay(3000L)
                    }
                }
            }
        }
    }

    private suspend fun getAddressBalance(client: CloseableHttpClient, address: String): Pair<Long, Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val request = HttpGet("https://blockchain.info/rawaddr/$address")
                client.execute(request).use { response ->
                    val entity = response.entity
                    val responseString = EntityUtils.toString(entity)
                    EntityUtils.consume(entity)
                    val jsonObject = JSONObject(responseString)
                    val finalBalance = jsonObject.getLong("final_balance")
                    val txs = jsonObject.optJSONArray("txs")
                    var confirmed = false

                    if (txs != null && txs.length() > 0) {
                        val latestTx = txs.getJSONObject(0)
                        val txHash = latestTx.getString("hash")
                        MainActivity.transactionId = txHash

                        
                        if (latestTx.has("block_height") && !latestTx.isNull("block_height")) {
                            confirmed = true
                        }
                    }

                    Pair(finalBalance, confirmed)
                }
            } catch (e: Exception) {
                println("API Request Error: $e")
                Pair(0L, false)
            }
        }
    }
}
