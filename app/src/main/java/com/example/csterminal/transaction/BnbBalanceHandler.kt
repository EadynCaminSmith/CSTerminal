package com.example.csterminal.transaction

import android.content.Context
import android.util.Log
import com.example.csterminal.AppDestinations
import com.example.csterminal.MainActivity
import com.example.csterminal.R
import com.example.csterminal.Values
import com.example.csterminal.seed.CheckBnbSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigInteger

class BnbBalanceHandler(private val context: Context) {
    private val web3j: Web3j = Web3j.build(HttpService("https://bsc-dataseed.binance.org/"))
    private val credentials by lazy { Credentials.create(CheckBnbSeed.privateKey) }
    private val fromAddress = Values.BNBAddress
    private val toAddress = Values.CentralBNBAddress

    suspend fun checkBalance() {
        if (fromAddress.isEmpty()) {
            Log.e("BnbBalanceHandler", "Address is empty, cannot check balance.")
            return
        }

        var balance = BigInteger.ZERO
        val startTime = System.currentTimeMillis()
        val timeout = 300000

        while (balance <= BigInteger.ZERO && System.currentTimeMillis() - startTime < timeout) {
            try {
                val ethGetBalance = web3j.ethGetBalance(fromAddress, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send()
                if(ethGetBalance.hasError()) {
                    Log.e("BnbBalanceHandler", "Error checking balance: ${ethGetBalance.error.message}")
                } else {
                    balance = ethGetBalance.balance
                    Log.d("BnbBalanceHandler", "Current Balance: $balance")
                    if (balance > BigInteger.ZERO) {
                        val expectedAmount = MainActivity.ppc
                        val actualAmount = Convert.fromWei(balance.toString(), Convert.Unit.ETHER).toDouble()
                        val difference = kotlin.math.abs(expectedAmount - actualAmount)
                        val tolerance = 0.0001

                        when {
                            difference <= tolerance -> handlePayment(AppDestinations.BNBPAID)
                            actualAmount > expectedAmount -> handlePayment(AppDestinations.BNBOVERPAID)
                            else -> handlePayment(AppDestinations.BNBUNDERPAID)
                        }
                        return
                    }
                }
            } catch (e: Exception) {
                Log.e("BnbBalanceHandler", "Error checking balance", e)
            }
            delay(5000)
        }
        if (balance == BigInteger.ZERO) {
            Log.d("BnbBalanceHandler", "Timeout reached, no payment received.")
            withContext(Dispatchers.Main) {
                MainActivity.navigateTo?.invoke(AppDestinations.KEYPAD)
            }
        }
    }

    private suspend fun handlePayment(destination: AppDestinations) {
        sendTransaction()
        withContext(Dispatchers.Main) {
            MainActivity.navigateTo?.invoke(destination)
        }
    }

    private suspend fun sendTransaction() {
        try {
            val nonce = web3j.ethGetTransactionCount(fromAddress, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send().transactionCount
            val gasPrice = web3j.ethGasPrice().send().gasPrice
            val gasLimit = BigInteger.valueOf(21000)
            val balance = web3j.ethGetBalance(fromAddress, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send().balance
            val valueToSend = balance.subtract(gasLimit.multiply(gasPrice))

            if (valueToSend > BigInteger.ZERO) {
                val rawTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, valueToSend)
                val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
                val hexValue = Numeric.toHexString(signedMessage)
                val transactionResponse = web3j.ethSendRawTransaction(hexValue).send()
                if (transactionResponse.hasError()) {
                    Log.e("BnbBalanceHandler", "Error sending transaction: ${transactionResponse.error.message}")
                } else {
                    val txHash = transactionResponse.transactionHash
                    Log.d("BnbBalanceHandler", "Transaction successful: $txHash")
                    MainActivity.transactionId = txHash
                }
            } else {
                Log.d("BnbBalanceHandler", "Not enough funds to cover gas fees.")
            }
        } catch (e: Exception) {
            Log.e("BnbBalanceHandler", "Error sending transaction", e)
        }
    }
}
