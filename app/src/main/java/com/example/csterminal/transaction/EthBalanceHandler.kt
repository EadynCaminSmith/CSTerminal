package com.example.csterminal.transaction

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.csterminal.AppDestinations
import com.example.csterminal.MainActivity
import com.example.csterminal.Values
import com.example.csterminal.seed.CheckEthSeed
import com.example.csterminal.seed.generateSeedPhrase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.io.entity.StringEntity
import org.json.JSONObject
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.coroutines.coroutineContext
import org.web3j.crypto.MnemonicUtils
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.tx.ChainId

class EthBalanceHandler(private val context: Context) {

    suspend fun checkBalance() {
        MainActivity.transactionId = ""
        CheckEthSeed(context).checkEthereumWallet()
        val address = CheckEthSeed.receiveAddress
        println("Monitoring ETH Address: $address for amount: ${MainActivity.ppc}")
        withContext(Dispatchers.IO) {
            HttpClients.createDefault().use { client ->
                while (coroutineContext.isActive) {
                    try {
                        val balanceEth = getAddressBalance(client, address)
                        println("Current ETH Balance: $balanceEth")
                        val epsilon = 0.000001
                        val diff = balanceEth.toDouble() - MainActivity.ppc
                        if (balanceEth.toDouble() > 0) {
                            val txHash = getLatestTransactionHash(client, address)
                            if (txHash.isNotEmpty()) {
                                MainActivity.transactionId = txHash
                                println("Transaction Found: $txHash")
                            }
                        }
                        if (balanceEth.toDouble() > 0 && (balanceEth.toDouble() >= MainActivity.ppc || Math.abs(diff) < epsilon)) {
                             if (Values.CentralEthAddress.isNotEmpty()) {
                                 forwardFunds(address, Values.CentralEthAddress)
                             }
                             if (Math.abs(diff) < epsilon) {
                                 println("Payment Received!")
                                 Handler(Looper.getMainLooper()).post {
                                    MainActivity.navigateTo?.invoke(AppDestinations.ETHPAID)
                                 }
                                 break
                             } else if (balanceEth.toDouble() > MainActivity.ppc) {
                                 println("Overpaid!")
                                 Handler(Looper.getMainLooper()).post {
                                    MainActivity.navigateTo?.invoke(AppDestinations.ETHOVERPAID)
                                 }
                                 break
                             }
                        } else if (balanceEth.toDouble() > 0 && balanceEth.toDouble() < MainActivity.ppc) {
                             println("Underpaid! Balance: $balanceEth, Required: ${MainActivity.ppc}")
                             Handler(Looper.getMainLooper()).post {
                                MainActivity.navigateTo?.invoke(AppDestinations.ETHUNDERPAID)
                             }
                             break
                        } else {
                            println("Waiting for payment...")
                        }
                    } catch (e: Exception) {
                        println("Error checking ETH balance: ${e.message}")
                        e.printStackTrace()
                    }
                    delay(5000L)
                }
            }
        }
    }

    private suspend fun forwardFunds(fromAddress: String, toAddress: String) {
        withContext(Dispatchers.IO) {
            try {
                println("Attempting to forward ETH from $fromAddress to $toAddress")
                val web3j = Web3j.build(HttpService("https://ethereum-rpc.publicnode.com"))
                val seedPhrase = generateSeedPhrase.phrase
                val seed = MnemonicUtils.generateSeed(seedPhrase, null)
                val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
                val path = intArrayOf(44 or Bip32ECKeyPair.HARDENED_BIT, 60 or Bip32ECKeyPair.HARDENED_BIT, 0 or Bip32ECKeyPair.HARDENED_BIT, 0, 0)
                val ethereumKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
                val credentials = Credentials.create(ethereumKeyPair)
                val ethGetTransactionCount = web3j.ethGetTransactionCount(
                    fromAddress, DefaultBlockParameterName.LATEST
                ).send()
                val nonce = ethGetTransactionCount.transactionCount
                val ethGasPrice = web3j.ethGasPrice().send()
                val gasPrice = ethGasPrice.gasPrice
                val gasLimit = BigInteger.valueOf(21000)
                val ethGetBalance = web3j.ethGetBalance(fromAddress, DefaultBlockParameterName.LATEST).send()
                val balance = ethGetBalance.balance
                val cost = gasPrice.multiply(gasLimit)
                if (balance > cost) {
                    val value = balance.subtract(cost)
                    val chainId = ChainId.MAINNET
                    val rawTransaction = RawTransaction.createEtherTransaction(
                        nonce, gasPrice, gasLimit, toAddress, value
                    )
                    val signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
                    val hexValue = Numeric.toHexString(signedMessage)
                    val ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send()
                    if (ethSendTransaction.hasError()) {
                        println("Error sending ETH: ${ethSendTransaction.error.message}")
                    } else {
                        println("ETH Forwarded! Tx Hash: ${ethSendTransaction.transactionHash}")
                    }
                } else {
                    println("Insufficient funds to cover gas for forwarding.")
                }
            } catch (e: Exception) {
                println("Exception forwarding ETH: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun getAddressBalance(client: CloseableHttpClient, address: String): BigDecimal {
        val formattedAddress = if (!address.startsWith("0x")) "0x$address" else address
        try {
            val post = HttpPost("https://ethereum-rpc.publicnode.com")
            post.setHeader("Content-Type", "application/json")
            val jsonBody = JSONObject()
            jsonBody.put("jsonrpc", "2.0")
            jsonBody.put("method", "eth_getBalance")
            val params = org.json.JSONArray()
            params.put(formattedAddress)
            params.put("latest")
            jsonBody.put("params", params)
            jsonBody.put("id", 1)
            post.entity = StringEntity(jsonBody.toString())
            client.execute(post).use { response ->
                val entity = response.entity
                val responseString = EntityUtils.toString(entity)
                val responseJson = JSONObject(responseString)
                if (responseJson.has("result")) {
                    val resultHex = responseJson.getString("result")
                    val hex = resultHex.removePrefix("0x")
                    if (hex.isNotEmpty()) {
                        val balanceWei = BigInteger(hex, 16)
                        return BigDecimal(balanceWei).movePointLeft(18)
                    }
                } else {
                    println("RPC Error Response: $responseString")
                }
            }
        } catch (e: Exception) {
            println("RPC Exception: $e")
        }
        return BigDecimal.ZERO
    }

    private fun getLatestTransactionHash(client: CloseableHttpClient, address: String): String {
        try {
            val postBlockNum = HttpPost("https://ethereum-rpc.publicnode.com")
            postBlockNum.setHeader("Content-Type", "application/json")
            val jsonBlockNum = JSONObject()
            jsonBlockNum.put("jsonrpc", "2.0")
            jsonBlockNum.put("method", "eth_blockNumber")
            val paramsNum = org.json.JSONArray()
            jsonBlockNum.put("params", paramsNum)
            jsonBlockNum.put("id", 2)
            postBlockNum.entity = StringEntity(jsonBlockNum.toString())
            var latestBlockHex = ""
            client.execute(postBlockNum).use { response ->
                val responseString = EntityUtils.toString(response.entity)
                val responseJson = JSONObject(responseString)
                if (responseJson.has("result")) {
                    latestBlockHex = responseJson.getString("result")
                }
            }
            if (latestBlockHex.isEmpty()) return ""
            val latestBlockNum = BigInteger(latestBlockHex.removePrefix("0x"), 16)
            val targetAddress = address.lowercase().removePrefix("0x")
            for (i in 0 until 5) {
                val blockNumToCheck = latestBlockNum.subtract(BigInteger.valueOf(i.toLong()))
                val blockHex = "0x" + blockNumToCheck.toString(16)
                val postBlock = HttpPost("https://ethereum-rpc.publicnode.com")
                postBlock.setHeader("Content-Type", "application/json")
                val jsonBlock = JSONObject()
                jsonBlock.put("jsonrpc", "2.0")
                jsonBlock.put("method", "eth_getBlockByNumber")
                val paramsBlock = org.json.JSONArray()
                paramsBlock.put(blockHex)
                paramsBlock.put(true) 
                jsonBlock.put("params", paramsBlock)
                jsonBlock.put("id", 3 + i)
                postBlock.entity = StringEntity(jsonBlock.toString())
                client.execute(postBlock).use { response ->
                    val responseString = EntityUtils.toString(response.entity)
                    val responseJson = JSONObject(responseString)
                    if (responseJson.has("result")) {
                        val result = responseJson.optJSONObject("result")
                        if (result != null) {
                            val transactions = result.optJSONArray("transactions")
                            if (transactions != null) {
                                for (j in 0 until transactions.length()) {
                                    val tx = transactions.getJSONObject(j)
                                    if (!tx.isNull("to")) {
                                        val to = tx.getString("to")
                                        if (to.lowercase().removePrefix("0x") == targetAddress) {
                                            return tx.getString("hash")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error fetching transaction hash: $e")
            e.printStackTrace()
        }
        return ""
    }
}
