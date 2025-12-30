package com.example.csterminal.seed

import android.content.Context
import org.bitcoindevkit.Mnemonic
import org.bitcoindevkit.WordCount
import java.io.File

class generateSeedPhrase(private val context: Context) {
    var phrase = ""
    companion object {
        var phrase = ""
    }

    fun generateBIP39Phrase(): String {
        try {
            val mnemonic = Mnemonic(WordCount.WORDS12)
            phrase = mnemonic.asString()
            generateSeedPhrase.phrase = phrase
            
            val file = File(context.filesDir, "seed.txt")
            file.writeText(phrase)
            println(phrase)
            return phrase
        } catch (e: Exception) {
            println("Error generating seed: $e")
            return "Error"
        }
    }
}
