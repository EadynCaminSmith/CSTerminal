package com.example.csterminal.seed

import android.content.Context
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.DeterministicSeed
import java.lang.Exception

class CheckLtcSeed(private val context: Context) {
    var receiveAddress = ""; companion object {
        var receiveAddress = ""
        var privateKey: ECKey? = null
    }

    fun checkLitecoinWallet() {
        try {
            val seedPhrase = generateSeedPhrase.phrase
            val seed = DeterministicSeed(seedPhrase, null, "", System.currentTimeMillis() / 1000)
            val masterPrivateKey: DeterministicKey = HDKeyDerivation.createMasterPrivateKey(seed.seedBytes)

            
            val purposeKey = HDKeyDerivation.deriveChildKey(masterPrivateKey, 49 or ChildNumber.HARDENED_BIT)
            val coinTypeKey = HDKeyDerivation.deriveChildKey(purposeKey, 2 or ChildNumber.HARDENED_BIT)
            val accountKey = HDKeyDerivation.deriveChildKey(coinTypeKey, 0 or ChildNumber.HARDENED_BIT)
            val changeKey = HDKeyDerivation.deriveChildKey(accountKey, 0)
            val addressKey = HDKeyDerivation.deriveChildKey(changeKey, 0)

            val ecKey = ECKey.fromPrivate(addressKey.privKey)
            privateKey = ecKey 

            val params = object : MainNetParams() {
                override fun getAddressHeader(): Int {
                    return 48 
                }
                override fun getP2SHHeader(): Int {
                    return 50 
                }
            }

            
            val redeemScript = ScriptBuilder.createP2WPKHOutputScript(ecKey)
            val p2shScript = ScriptBuilder.createP2SHOutputScript(redeemScript)
            val address = LegacyAddress.fromScriptHash(params, p2shScript.pubKeyHash)

            receiveAddress = address.toString()
            CheckLtcSeed.receiveAddress = receiveAddress
        } catch (e: Exception) {
            println("Error creating Litecoin wallet: ${e.message}")
            e.printStackTrace()
        }
    }
}
