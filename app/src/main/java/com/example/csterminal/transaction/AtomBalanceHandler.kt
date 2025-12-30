package com.example.csterminal.transaction

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
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
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.io.entity.StringEntity
import org.json.JSONObject
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.MnemonicUtils
import org.web3j.crypto.Sign
import java.math.BigDecimal
import java.util.Arrays

class AtomBalanceHandler(private val context: Context) {

    suspend fun checkBalance() {
        MainActivity.transactionId = ""
        withContext(Dispatchers.IO) {
            val address = Values.ATOMAddress
            if (address.isEmpty()) return@withContext

            try {
                HttpClients.createDefault().use { client ->
                    while (isActive) {
                        try {
                            val balanceUatom = getAtomAddressBalance(client, address)
                            val balanceATOM = balanceUatom.divide(BigDecimal("1000000"))

                            Log.i("CSTerminal", "ATOM Balance: $balanceATOM ATOM for address: $address")
                            
                            val epsilon = BigDecimal("0.000001")
                            val required = BigDecimal(MainActivity.ppc.toString())
                            val diff = balanceATOM.subtract(required).abs()

                            if (balanceATOM > BigDecimal.ZERO) {
                                
                                val txHash = getLatestIncomingTransactionHash(client, address)
                                if (txHash.isNotEmpty()) {
                                    MainActivity.transactionId = txHash
                                }

                                if (balanceATOM >= required || diff < epsilon) {
                                    Log.i("CSTerminal", "ATOM Payment Received and Confirmed. Forwarding...")
                                    
                                    if (Values.CentralAtomAddress.isNotEmpty()) {
                                        forwardAtomFunds(address, Values.CentralAtomAddress, balanceUatom)
                                    }
                                    
                                    Handler(Looper.getMainLooper()).post {
                                        if (diff < epsilon) {
                                            MainActivity.navigateTo?.invoke(AppDestinations.ATOMPAID)
                                        } else {
                                            MainActivity.navigateTo?.invoke(AppDestinations.ATOMOVERPAID)
                                        }
                                    }
                                    break
                                } else if (balanceATOM > BigDecimal.ZERO && balanceATOM < required) {
                                    Log.i("CSTerminal", "ATOM Underpaid! Balance: $balanceATOM, Required: $required")
                                    Handler(Looper.getMainLooper()).post {
                                        MainActivity.navigateTo?.invoke(AppDestinations.ATOMUNDERPAID)
                                    }
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            Log.e("CSTerminal", "Error checking ATOM balance: ${e.message}")
                        }
                        delay(10000L)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("CSTerminal", "Error in ATOM balance handler: ${e.message}")
            }
        }
    }

    private suspend fun forwardAtomFunds(fromAddress: String, toAddress: String, amountUatom: BigDecimal) {
        withContext(Dispatchers.IO) {
            try {
                Log.i("CSTerminal", "Initiating real ATOM forward from $fromAddress to $toAddress")

                
                val seedPhrase = generateSeedPhrase.phrase
                val seed = MnemonicUtils.generateSeed(seedPhrase, null)
                val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
                val path = intArrayOf(44 or Bip32ECKeyPair.HARDENED_BIT, 118 or Bip32ECKeyPair.HARDENED_BIT, 0 or Bip32ECKeyPair.HARDENED_BIT, 0, 0)
                val atomKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)

                
                val pubKeyBI = atomKeyPair.publicKey
                val xBytes = pubKeyBI.toByteArray()
                val xClean = if (xBytes.size > 32) Arrays.copyOfRange(xBytes, xBytes.size - 32, xBytes.size) else xBytes
                val compressedPubKey = ByteArray(33)
                compressedPubKey[0] = if (pubKeyBI.testBit(0)) 0x03.toByte() else 0x02.toByte()
                System.arraycopy(xClean, 0, compressedPubKey, 1, 32)

                HttpClients.createDefault().use { client ->
                    
                    val authRequest = HttpGet("https://rest.cosmos.directory/cosmoshub/cosmos/auth/v1beta1/accounts/$fromAddress")
                    val authResponseStr = client.execute(authRequest).use { response -> EntityUtils.toString(response.entity) }
                    val authJson = JSONObject(authResponseStr)
                    val accountObj = authJson.optJSONObject("account") ?: throw Exception("Account info not found.")
                    val realAccount = if (accountObj.has("base_account")) accountObj.getJSONObject("base_account") else accountObj

                    val accountNumber = realAccount.optString("account_number", "0").toLong()
                    val sequence = realAccount.optString("sequence", "0").toLong()

                    val nodeInfoRequest = HttpGet("https://rest.cosmos.directory/cosmoshub/cosmos/base/tendermint/v1beta1/node_info")
                    val nodeInfoStr = client.execute(nodeInfoRequest).use { response -> EntityUtils.toString(response.entity) }
                    val chainId = JSONObject(nodeInfoStr).getJSONObject("default_node_info").getString("network")

                    
                    val feeAmount = 5000L
                    val gasLimit = 200000L
                    val sendAmount = amountUatom.toLong() - feeAmount
                    if (sendAmount <= 0) return@use

                    
                    val msgSend = encodeMsgSend(fromAddress, toAddress, sendAmount, "uatom")
                    val txBody = encodeTxBody(msgSend)
                    val authInfo = encodeAuthInfo(compressedPubKey, sequence, feeAmount, "uatom", gasLimit)

                    
                    val signDoc = encodeSignDoc(txBody, authInfo, chainId, accountNumber)
                    val hash = java.security.MessageDigest.getInstance("SHA-256").digest(signDoc)

                    
                    
                    
                    
                    val sigData = Sign.signMessage(hash, atomKeyPair, false) 
                    
                    val signature = ByteArray(64)
                    System.arraycopy(sigData.r, 0, signature, 0, 32)
                    System.arraycopy(sigData.s, 0, signature, 32, 32)

                    
                    val txRaw = encodeTx(txBody, authInfo, signature)
                    val broadcastBody = JSONObject()
                    broadcastBody.put("tx_bytes", Base64.encodeToString(txRaw, Base64.NO_WRAP))
                    broadcastBody.put("mode", "BROADCAST_MODE_SYNC")

                    val post = HttpPost("https://rest.cosmos.directory/cosmoshub/cosmos/tx/v1beta1/txs")
                    post.entity = StringEntity(broadcastBody.toString())
                    client.execute(post).use { response ->
                        Log.i("CSTerminal", "ATOM Broadcast Result: ${EntityUtils.toString(response.entity)}")
                    }
                }
            } catch (e: Exception) {
                Log.e("CSTerminal", "Forwarding error: ${e.message}")
            }
        }
    }


    
    
    private fun encodeVarint(value: Long): ByteArray {
        var v = value
        val out = mutableListOf<Byte>()
        while (v >= 0x80) {
            out.add(((v and 0x7f) or 0x80).toByte())
            v = v ushr 7
        }
        out.add(v.toByte())
        return out.toByteArray()
    }

    private fun encodeTag(fieldNumber: Int, wireType: Int) = encodeVarint(((fieldNumber shl 3) or wireType).toLong())
    private fun encodeBytes(fieldNumber: Int, value: ByteArray) = encodeTag(fieldNumber, 2) + encodeVarint(value.size.toLong()) + value
    private fun encodeString(fieldNumber: Int, value: String) = encodeBytes(fieldNumber, value.toByteArray())
    private fun encodeUint64(fieldNumber: Int, value: Long) = encodeTag(fieldNumber, 0) + encodeVarint(value)

    private fun encodeMsgSend(from: String, to: String, amount: Long, denom: String): ByteArray {
        val coin = encodeString(1, denom) + encodeString(2, amount.toString())
        val amountBytes = encodeBytes(3, coin)
        return encodeString(1, from) + encodeString(2, to) + amountBytes
    }

    private fun encodeTxBody(msgSend: ByteArray): ByteArray {
        val any = encodeString(1, "/cosmos.bank.v1beta1.MsgSend") + encodeBytes(2, msgSend)
        return encodeBytes(1, any)
    }

    private fun encodeAuthInfo(pubKey: ByteArray, seq: Long, fee: Long, denom: String, gas: Long): ByteArray {
        val pkAny = encodeString(1, "/cosmos.crypto.secp256k1.PubKey") + encodeBytes(2, encodeBytes(1, pubKey))
        
        
        val single = encodeUint64(1, 1)
        val modeInfo = encodeBytes(1, single)
        
        val signerInfo = encodeBytes(1, pkAny) + encodeBytes(2, modeInfo) + encodeUint64(3, seq)
        
        val coin = encodeString(1, denom) + encodeString(2, fee.toString())
        val feeMsg = encodeBytes(1, coin) + encodeUint64(2, gas)
        
        return encodeBytes(1, signerInfo) + encodeBytes(2, feeMsg)
    }

    private fun encodeSignDoc(body: ByteArray, auth: ByteArray, chainId: String, accNum: Long): ByteArray {
        return encodeBytes(1, body) + encodeBytes(2, auth) + encodeString(3, chainId) + encodeUint64(4, accNum)
    }

    private fun encodeTx(body: ByteArray, auth: ByteArray, sig: ByteArray): ByteArray {
        return encodeBytes(1, body) + encodeBytes(2, auth) + encodeBytes(3, sig)
    }

    private suspend fun getAtomAddressBalance(client: CloseableHttpClient, address: String): BigDecimal {
        return withContext(Dispatchers.IO) {
            try {
                val request = HttpGet("https://rest.cosmos.directory/cosmoshub/cosmos/bank/v1beta1/balances/$address")
                val responseString = client.execute(request).use { response -> EntityUtils.toString(response.entity) }
                val json = JSONObject(responseString)
                val balances = json.optJSONArray("balances")
                if (balances != null) {
                    for (i in 0 until balances.length()) {
                        val bal = balances.getJSONObject(i)
                        if (bal.getString("denom") == "uatom") {
                            return@withContext BigDecimal(bal.getString("amount"))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CSTerminal", "ATOM API Balance Error: $e")
            }
            BigDecimal.ZERO
        }
    }

    private suspend fun getLatestIncomingTransactionHash(client: CloseableHttpClient, address: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://rest.cosmos.directory/cosmoshub/cosmos/tx/v1beta1/txs?events=transfer.recipient='$address'&pagination.limit=1&order_by=ORDER_BY_DESC"
                val request = HttpGet(url)
                val responseString = client.execute(request).use { response -> EntityUtils.toString(response.entity) }
                val json = JSONObject(responseString)
                val txResponses = json.optJSONArray("tx_responses")
                if (txResponses != null && txResponses.length() > 0) {
                    return@withContext txResponses.getJSONObject(0).getString("txhash")
                }
            } catch (e: Exception) {
                Log.e("CSTerminal", "ATOM Transaction Hash API Error: $e")
            }
            ""
        }
    }
}
