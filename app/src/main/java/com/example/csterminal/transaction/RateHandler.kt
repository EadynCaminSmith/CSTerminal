package com.example.csterminal.transaction

import com.example.csterminal.ui.theme.ThemeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.HttpHeaders
import org.apache.hc.core5.http.NameValuePair
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.net.URIBuilder
import org.json.JSONObject
import java.io.IOException
import java.net.URISyntaxException


class RateHandler(private val themeViewModel: ThemeViewModel) {
    val CMCAPIKEY = ""
    val BLOCKCHAIR_API_KEY = ""
    val EXCHANGERATE_API_KEY = ""

    
    suspend fun fetchMarketRate(symbol: String = "BTC", currency: String = "AUD"): Double? {
        val uri = "https://pro-api.coinmarketcap.com/v2/cryptocurrency/quotes/latest"
        val parameters: MutableList<NameValuePair> = ArrayList<NameValuePair>()
        parameters.add(BasicNameValuePair("symbol", symbol))
        parameters.add(BasicNameValuePair("convert", currency))

        return withContext(Dispatchers.IO) {
            try {
                val result = makeAPICall(uri, parameters)
                if (result != null) {
                    val jsonObject = JSONObject(result)
                    val data = jsonObject.getJSONObject("data")
                    val currencyList = data.getJSONArray(symbol)
                    if (currencyList.length() > 0) {
                        val currencyData = currencyList.getJSONObject(0)
                        val quote = currencyData.getJSONObject("quote")
                        val quoteData = quote.getJSONObject(currency)
                        return@withContext quoteData.getDouble("price")
                    }
                }
            } catch (e: Exception) {
                println("Error parsing JSON or network error: $e")
            }
            return@withContext null
        }
    }

    suspend fun fetchFiatRate(from: String = "AUD", to: String): Double {
        if (from == to) return 1.0
        val uri = "https://v6.exchangerate-api.com/v6/$EXCHANGERATE_API_KEY/latest/$from"
        
        return withContext(Dispatchers.IO) {
            try {
                val result = makeAPICall(uri, null)
                if (result != null) {
                    val jsonObject = JSONObject(result)
                    val rates = jsonObject.getJSONObject("conversion_rates")
                    if (rates.has(to)) {
                        return@withContext rates.getDouble(to)
                    }
                }
            } catch (e: Exception) {
                println("Error fetching fiat rate: $e")
            }
            return@withContext 1.0 
        }
    }

    
    suspend fun calculateCryptoAmount(symbol: String, currency: String, fiatAmount: Double): Double? {
        
        val marketRate = fetchMarketRate(symbol, currency) ?: return null
        
        return withContext(Dispatchers.IO) {
            
            val baseCrypto = fiatAmount / marketRate
            
            var cryptoWithFee = baseCrypto

            
            if (themeViewModel.passFeesOn[symbol] == true) {
                val feeInCrypto = fetchNetworkFee(symbol)
                if (feeInCrypto != null) {
                    cryptoWithFee += feeInCrypto
                }
            }

            
            
            
            val surchargePercent = themeViewModel.cryptoSurcharges[symbol] ?: 0.0
            val multiplier = 1.0 + (surchargePercent / 100.0)
            
            val finalCrypto = cryptoWithFee * multiplier
            
            return@withContext finalCrypto
        }
    }

    suspend fun getFeeInFiat(symbol: String, currency: String): Double? {
        return withContext(Dispatchers.IO) {
            val cryptoFee = fetchNetworkFee(symbol) ?: return@withContext null
            val rate = fetchMarketRate(symbol, currency) ?: return@withContext null
            return@withContext cryptoFee * rate
        }
    }

    suspend fun fetchNetworkFee(symbol: String): Double? {
        val chain = when (symbol) {
            "BTC" -> "bitcoin"
            "ETH" -> "ethereum"
            "LTC" -> "litecoin"
            "TRX" -> "tron"
            "ATOM" -> "cosmos"
            "SOL" -> "solana"
            "BNB" -> "binance-coin"
            "XRP" -> "ripple"
            else -> return null
        }

        val uri = "https://api.blockchair.com/$chain/stats"
        val parameters: MutableList<NameValuePair> = ArrayList<NameValuePair>()
        parameters.add(BasicNameValuePair("key", BLOCKCHAIR_API_KEY))

        return withContext(Dispatchers.IO) {
            try {
                val result = makeAPICall(uri, parameters)
                if (result != null) {
                    val jsonObject = JSONObject(result)
                    
                    if (jsonObject.has("data")) {
                        val data = jsonObject.getJSONObject("data")
                        
                        return@withContext when (symbol) {
                            "BTC", "LTC" -> {
                                
                                if (data.has("median_transaction_fee_24h")) {
                                    val medianFee = data.getDouble("median_transaction_fee_24h")
                                    medianFee / 100_000_000.0
                                } else if (data.has("suggested_transaction_fee_per_byte_sat")) {
                                    val feePerByte = data.getDouble("suggested_transaction_fee_per_byte_sat")
                                    
                                    (feePerByte * 140) / 100_000_000.0
                                } else null
                            }
                            "ETH" -> {
                                if (data.has("median_gas_price")) {
                                    val medianGasPriceWei = data.getDouble("median_gas_price")
                                    
                                    (medianGasPriceWei * 21_000) / 1_000_000_000_000_000_000.0
                                } else null
                            }
                            "XRP" -> {
                                if (data.has("median_transaction_fee_24h")) {
                                    val medianFeeDrops = data.getDouble("median_transaction_fee_24h")
                                    
                                    medianFeeDrops / 1_000_000.0
                                } else null
                            }
                            "SOL" -> {
                                if (data.has("median_transaction_fee_24h")) {
                                    val medianFeeLamports = data.getDouble("median_transaction_fee_24h")
                                    
                                    medianFeeLamports / 1_000_000_000.0
                                } else null
                            }
                            "TRX" -> {
                                
                                
                                0.1 
                            }
                            else -> null
                        }
                    }
                }
            } catch (e: Exception) {
                
                if (symbol != "ATOM" && symbol != "BNB") {
                    println("Error fetching fees for $symbol: ${e.message}")
                }
            }
            
            return@withContext null
        }
    }

    @Throws(URISyntaxException::class, IOException::class)
    fun makeAPICall(uri: String?, parameters: MutableList<NameValuePair>?): String? {
        var response_content: String? = ""

        val query: URIBuilder = URIBuilder(uri)
        if (parameters != null) {
            query.addParameters(parameters)
        }

        val client = HttpClients.createDefault()
        try {
            val request: HttpGet = HttpGet(query.build())

            request.setHeader(HttpHeaders.ACCEPT, "application/json")
            if (uri?.contains("coinmarketcap") == true) {
                request.addHeader("X-CMC_PRO_API_KEY", CMCAPIKEY)
            }

            val response = client.execute(request)

            try {
                val entity: HttpEntity? = response.entity
                response_content = EntityUtils.toString(entity)
                EntityUtils.consume(entity)
            } finally {
                response.close()
            }
        } finally {
            client.close()
        }

        return response_content
    }
}
