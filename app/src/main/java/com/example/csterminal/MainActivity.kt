package com.example.csterminal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.csterminal.seed.CheckAtomSeed
import com.example.csterminal.seed.CheckBnbSeed
import com.example.csterminal.seed.CheckEthSeed
import com.example.csterminal.seed.CheckLtcSeed
import com.example.csterminal.seed.CheckSeed
import com.example.csterminal.seed.CheckSolanaSeed
import com.example.csterminal.seed.CheckTrxSeed
import com.example.csterminal.seed.CheckXrpSeed
import com.example.csterminal.seed.generateSeedPhrase
import com.example.csterminal.transaction.AtomBalanceHandler
import com.example.csterminal.transaction.BalanceHandler
import com.example.csterminal.transaction.BnbBalanceHandler
import com.example.csterminal.transaction.EthBalanceHandler
import com.example.csterminal.transaction.LtcBalanceHandler
import com.example.csterminal.transaction.RateHandler
import com.example.csterminal.transaction.SolanaHandler
import com.example.csterminal.transaction.Transaction
import com.example.csterminal.transaction.TransactionViewModel
import com.example.csterminal.transaction.TrxBalanceHandler
import com.example.csterminal.transaction.XrpBalanceHandler
import com.example.csterminal.ui.theme.CSTerminalTheme
import com.example.csterminal.ui.theme.ThemeViewModel
import com.google.zxing.BarcodeFormat
import com.iposprinter.iposprinterservice.IPosPrinterCallback
import com.iposprinter.iposprinterservice.IPosPrinterService
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

fun vectorToBitmap(context: Context, drawableId: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth.coerceAtLeast(1),
        drawable.intrinsicHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun generateQR(content: String): Bitmap? {
    if(content.isEmpty()) return null
    try {
        val writer = BarcodeEncoder()
        val bitMatrix = writer.encode(
            content,
            BarcodeFormat.QR_CODE,
            400,
            400
        )
        return writer.createBitmap(bitMatrix)
    } catch (e: Exception){
        e.printStackTrace()
        return null
    }
}

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        var ppc: Double = 0.0
        var navigateTo: ((AppDestinations) -> Unit)? = null
        var transactionId by mutableStateOf("")
        var confirmations: Int by mutableStateOf(0)
        var printerService: IPosPrinterService? = null
        var isPrinterServiceConnected by mutableStateOf(false)
        var isPrinterBindFailed by mutableStateOf(false)
        var connectionStatusMessage by mutableStateOf("Connecting to Printer...")
        var lastFeePaidFiat: String = "0.00"
        var lastFeePassedToCustomer: Boolean = false

        private fun processBitmapForPrinting(bitmap: Bitmap): Bitmap {
            val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
            val canvas = Canvas(newBitmap)
            canvas.drawColor(AndroidColor.WHITE)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            return newBitmap
        }

        fun printReceipt(context: Context, txHash: String, txUrl: String, fiatAmount: String, cryptoAmount: Double, cryptoSymbol: String, currency: String) {
            val service = printerService
            if (service == null) {
                Log.e(TAG, "Printer service is not connected.")
                return
            }
            try {
                val callback = object : IPosPrinterCallback.Stub() {
                    override fun onRunResult(isSuccess: Boolean) {
                        Log.d(TAG, "Receipt Print onRunResult: $isSuccess")
                    }
                    override fun onReturnString(result: String?) {
                        Log.d(TAG, "Receipt Print onReturnString: $result")
                    }
                }
                service.printerInit(callback)
                val originalLogo = vectorToBitmap(context, R.drawable.logos)
                if (originalLogo != null) {
                    val processedLogo = processBitmapForPrinting(originalLogo)
                    service.printBitmap(1, 8, processedLogo, callback)
                    service.printBlankLines(1, 5, callback)
                }
                service.setPrinterPrintAlignment(1, callback)
                service.printSpecifiedTypeText("CS Consulting\n", "ST", 32, callback)
                service.printBlankLines(1, 10, callback)

                service.setPrinterPrintAlignment(1, callback)
                service.printSpecifiedTypeText("TRANSACTION RECEIPT\n", "ST", 32, callback)
                service.printBlankLines(1, 10, callback)
                service.setPrinterPrintAlignment(0, callback)

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val currentDate = sdf.format(Date())
                service.printText("Date: $currentDate\n", callback)

                service.printText("Amount: $fiatAmount $currency\n", callback)
                val formattedCryptoAmount = when(cryptoSymbol) {
                    "TRX", "ATOM", "SOL", "LTC", "XRP" -> String.format("%.6f", cryptoAmount)
                    else -> String.format("%.8f", cryptoAmount)
                }
                service.printText("Paid: $formattedCryptoAmount $cryptoSymbol\n", callback)
                service.printBlankLines(1, 10, callback)
                service.setPrinterPrintAlignment(1, callback)
                service.printText("Hash:\n$txHash\n", callback)
                service.printBlankLines(1, 20, callback)
                service.printQRCode(txUrl, 8, 1, callback)
                service.printBlankLines(1, 20, callback)
                Handler(Looper.getMainLooper()).post {
                    try {
                        service.printerPerformPrint(160, callback)
                    } catch (e: Exception) {
                        Log.e(TAG, "Perform print failed for receipt", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error printing receipt", e)
                e.printStackTrace()
            }
        }
        fun testPrint(context: Context, rates: Map<String, Double>, randomAddress: String, currency: String) {
            val service = printerService
            if (service == null) {
                Log.e(TAG, "Printer service is not connected.")
                return
            }

            try {
                val printerStatus = service.getPrinterStatus()
                Log.d(TAG, "Printer status: $printerStatus")
                if (printerStatus != 0) {
                    Log.e(TAG, "Printer is not ready. Status: $printerStatus")
                    return
                }

                val callback = object : IPosPrinterCallback.Stub() {
                    override fun onRunResult(isSuccess: Boolean) {
                        Log.d(TAG, "Test Print onRunResult: $isSuccess")
                    }
                    override fun onReturnString(result: String?) {
                        Log.d(TAG, "Test Print onReturnString: $result")
                    }
                }

                service.printerInit(callback)
                val originalLogo = vectorToBitmap(context, R.drawable.logos)
                if (originalLogo != null) {
                    val processedLogo = processBitmapForPrinting(originalLogo)
                    service.printBitmap(1, 8, processedLogo, callback)
                    service.printBlankLines(1, 5, callback)
                }
                service.setPrinterPrintAlignment(1, callback)
                service.printSpecifiedTypeText("CS Consulting\n", "ST", 32, callback)
                service.printBlankLines(1, 10, callback)

                service.setPrinterPrintAlignment(1, callback)
                service.printSpecifiedTypeText("TEST PRINT REPORT\n", "ST", 32, callback)
                service.printBlankLines(1, 10, callback)

                service.setPrinterPrintAlignment(0, callback)
                rates.forEach { (symbol, price) ->
                    service.printText("$symbol Rate: $price $currency\n", callback)
                }

                service.printBlankLines(1, 10, callback)
                service.setPrinterPrintAlignment(1, callback)
                service.printText("Random Central Address:\n", callback)
                service.printText("$randomAddress\n", callback)
                service.printBlankLines(1, 10, callback)
                service.printQRCode(randomAddress, 8, 1, callback)

                service.printBlankLines(1, 20, callback)
                Handler(Looper.getMainLooper()).post {
                    try {
                        service.printerPerformPrint(160, callback)
                        Log.d(TAG, "Test print commands sent successfully.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Perform print failed for test print", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during test print", e)
                e.printStackTrace()
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "onServiceConnected: $name")
            printerService = IPosPrinterService.Stub.asInterface(service)
            isPrinterServiceConnected = true
            connectionStatusMessage = "Connected"
            Log.d(TAG, "Printer Service Connected")
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected: $name")
            printerService = null
            isPrinterServiceConnected = false
            connectionStatusMessage = "Disconnected"
            Log.d(TAG, "Printer Service Disconnected")
        }

        override fun onBindingDied(name: ComponentName?) {
            super.onBindingDied(name)
            Log.e(TAG, "onBindingDied: $name")
            isPrinterServiceConnected = false
            printerService = null
            connectionStatusMessage = "Binding Died"
        }

        override fun onNullBinding(name: ComponentName?) {
            super.onNullBinding(name)
            Log.e(TAG, "onNullBinding: $name")
            isPrinterServiceConnected = false
            printerService = null
            connectionStatusMessage = "Null Binding (Service Rejected)"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val intent = Intent().apply {
            setPackage("com.iposprinter.iposprinterservice")
            action = "com.iposprinter.iposprinterservice.IPosPrintService"
        }

        val bound = bindService(intent, connection, Context.BIND_AUTO_CREATE)
        Log.d(TAG, "bindService result = $bound")

        if (!bound) {
            isPrinterBindFailed = true
            connectionStatusMessage = "Bind Failed"
        }

        try {
            val pi = packageManager.getPackageInfo("com.iposprinter.iposprinterservice", 0)
            Log.d(TAG, "Package found: ${pi.packageName} version: ${pi.versionName}")
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package com.iposprinter.iposprinterservice NOT FOUND")
            connectionStatusMessage = "Service App Not Installed"
            isPrinterBindFailed = true
        }

        if (!isPrinterBindFailed) {
            val intent = Intent()
            intent.component = ComponentName("com.iposprinter.iposprinterservice", "com.iposprinter.iposprinterservice.IPosPrinterService")

            try {
                val bound = bindService(intent, connection, Context.BIND_AUTO_CREATE)
                Log.d(TAG, "bindService result: $bound")
                if (!bound) {
                    isPrinterBindFailed = true
                    connectionStatusMessage = "Bind Failed (False)"
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException binding service", e)
                isPrinterBindFailed = true
                connectionStatusMessage = "Permission Denied"
            } catch (e: Exception) {
                Log.e(TAG, "Error binding service", e)
                isPrinterBindFailed = true
                connectionStatusMessage = "Error: ${e.message}"
            }
        }
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val transactionViewModel: TransactionViewModel = viewModel()
            CSTerminalTheme(darkTheme = themeViewModel.isDarkTheme.value) {
                CSTerminalApp(themeViewModel, transactionViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unbindService(connection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

object Values {
    var BTCAddress: String = ""
    var EthAddress: String = ""
    var LTCAddress: String = ""
    var TRXAdress: String = ""
    var ATOMAddress: String = ""
    var XRPAddress: String = ""
    var SOLAddress: String = ""
    var BNBAddress: String = ""
    var USDTAddress: String = ""
    var USDCAddress: String = ""

    var CentralBTCAddress: String = ""
    var CentralEthAddress: String = ""
    var CentralLTCAddress: String = ""
    var CentralTrxAddress: String = ""
    var CentralAtomAddress: String = ""
    var CentralSOLAddress: String = ""
    var CentralBNBAddress: String = ""
    var CentralXRPAddress: String = ""
    var BlockchairAPIKey: String = ""

    var currency: String = "AUD"
}

@Composable
fun CSTerminalApp(themeViewModel: ThemeViewModel, transactionViewModel: TransactionViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.DASHBOARD) }
    MainActivity.navigateTo = { destination ->
        currentDestination = destination
    }

    val currentCurrency = themeViewModel.currency.value

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.filter {
                it != AppDestinations.PAYMENT &&
                        it != AppDestinations.CWALLET &&
                        it != AppDestinations.SURCHARGES &&
                        it != AppDestinations.ENABLED_COINS &&
                        it != AppDestinations.CONFIRMING &&
                        !it.name.endsWith("PAYMENT") &&
                        !it.name.endsWith("PAID") &&
                        !it.name.endsWith("OVERPAID") &&
                        !it.name.endsWith("UNDERPAID")
            }.forEach { destination ->
                item(
                    icon = {
                        if (destination.icon is ImageVector) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        } else if (destination.icon is Int) {
                            Icon(
                                painter = painterResource(id = destination.icon),
                                contentDescription = destination.label,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val contentModifier = Modifier.padding(innerPadding)
            when (currentDestination) {
                AppDestinations.DASHBOARD -> Dashboard(contentModifier, currentCurrency, themeViewModel, transactionViewModel)
                AppDestinations.KEYPAD -> KeyPad(contentModifier, currentCurrency, onPayClick = {
                    currentDestination = AppDestinations.PAYMENT
                })
                AppDestinations.FAVORITES -> FavoritesScreen(contentModifier, transactionViewModel, themeViewModel)
                AppDestinations.PROFILE -> ProfileScreen(
                    modifier = contentModifier,
                    isDarkTheme = themeViewModel.isDarkTheme.value,
                    onThemeToggle = { themeViewModel.toggleTheme() },
                    currentCurrency = currentCurrency,
                    onCurrencyChange = { themeViewModel.setCurrency(it) },
                    onSurchargesClick = { currentDestination = AppDestinations.SURCHARGES },
                    onEnabledCoinsClick = { currentDestination = AppDestinations.ENABLED_COINS }
                )
                AppDestinations.SURCHARGES -> SurchargesScreen(
                    modifier = contentModifier,
                    themeViewModel = themeViewModel
                )
                AppDestinations.ENABLED_COINS -> SelectCoinsScreen(
                    modifier = contentModifier,
                    themeViewModel = themeViewModel
                )
                AppDestinations.PAYMENT -> PaymentScreen(
                    modifier = contentModifier,
                    isDarkTheme = themeViewModel.isDarkTheme.value,
                    currentCurrency = currentCurrency,
                    onCWalletClick = {
                        currentDestination = AppDestinations.CWALLET
                    }
                )
                AppDestinations.CWALLET -> CWallet(contentModifier,
                    themeViewModel = themeViewModel,
                    currentCurrency = currentCurrency,
                    onBTCClick = { currentDestination = AppDestinations.BTCPAYMENT },
                    onETHClick = { currentDestination = AppDestinations.ETHPAYMENT },
                    onLTCClick = { currentDestination = AppDestinations.LTCPAYMENT },
                    onTRXClick = { currentDestination = AppDestinations.TRXPAYMENT },
                    onATOMClick = { currentDestination = AppDestinations.ATOMPAYMENT },
                    onSOLClick = { currentDestination = AppDestinations.SOLPAYMENT },
                    onBNBClick = { currentDestination = AppDestinations.BNBPAYMENT },
                    onXRPClick = { currentDestination = AppDestinations.XRPPAYMENT }
                )
                AppDestinations.CONFIRMING -> ConfirmingScreen(contentModifier, MainActivity.confirmations, themeViewModel.isDarkTheme.value)
                AppDestinations.BTCPAYMENT -> BTCPayment(contentModifier, currentCurrency, themeViewModel)
                AppDestinations.BTCPAID -> BTCApproved(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.BTCUNDERPAID -> BTCUnderpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.BTCOVERPAID -> BTCOverpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.ETHPAYMENT -> ETHPayment(contentModifier, currentCurrency, themeViewModel)
                AppDestinations.ETHPAID -> ETHApproved(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.ETHUNDERPAID -> ETHUnderpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.ETHOVERPAID -> ETHOverpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.LTCPAYMENT -> LTCPayment(contentModifier, currentCurrency, themeViewModel)
                AppDestinations.LTCPAID -> LTCApproved(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.LTCUNDERPAID -> LTCUnderpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.LTCOVERPAID -> LTCOverpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.TRXPAYMENT -> TRXPayment(contentModifier, currentCurrency, themeViewModel)
                AppDestinations.TRXPAID -> TRXApproved(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.TRXUNDERPAID -> TRXUnderpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.TRXOVERPAID -> TRXOverpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.ATOMPAYMENT -> ATOMPayment(contentModifier, currentCurrency, themeViewModel)
                AppDestinations.ATOMPAID -> ATOMApproved(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.ATOMUNDERPAID -> ATOMUnderpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.ATOMOVERPAID -> ATOMOverpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.SOLPAYMENT -> SOLPayment(contentModifier, currentCurrency, themeViewModel)
                AppDestinations.SOLPAID -> SOLApproved(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.SOLUNDERPAID -> SOLUnderpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.SOLOVERPAID -> SOLOverpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.BNBPAYMENT -> BNBPayment(contentModifier, currentCurrency, themeViewModel)
                AppDestinations.BNBPAID -> BNBApproved(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.BNBUNDERPAID -> BNBUnderpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.BNBOVERPAID -> BNBOverpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.XRPPAYMENT -> XRPPayment(contentModifier, currentCurrency, themeViewModel)
                AppDestinations.XRPPAID -> XRPApproved(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.XRPUNDERPAID -> XRPUnderpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
                AppDestinations.XRPOVERPAID -> XRPOverpaid(contentModifier, currentCurrency, transactionViewModel, themeViewModel)
            }
        }
    }
}

public object priceHandler {
    var displayPrice by mutableStateOf("0.00")
    var realPrice = ""
    var priceChange = ""
    fun priceUpdate() {
        if (priceChange != "d") {
            if (realPrice.length < 10) {
                realPrice += priceChange
            }
        } else {
            if (realPrice.isNotEmpty()) {
                realPrice = realPrice.dropLast(1)
            }
        }

        if (realPrice.isEmpty()) {
            displayPrice = "0.00"
        } else {
            val priceValue = realPrice.toDoubleOrNull() ?: 0.0
            displayPrice = String.format("%.2f", priceValue / 100.0)
        }
    }
}

@Composable
fun WipeAnimatedLogo(modifier: Modifier = Modifier, colorFilter: ColorFilter? = null) {
    val infiniteTransition = rememberInfiniteTransition(label = "wipe_transition")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wipe_progress"
    )

    Image(
        painter = painterResource(id = R.drawable.logos),
        contentDescription = "Logo",
        colorFilter = colorFilter,
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val w = size.width

                val isWipeOut = progress < 1f
                val localP = if (isWipeOut) progress else progress - 1f

                val colors = if (isWipeOut) {
                    listOf(Color.Transparent, Color.Black)
                } else {
                    listOf(Color.Black, Color.Transparent)
                }

                val startX = (w * 2 * localP) - w
                val endX = (w * 2 * localP)

                val brush = Brush.horizontalGradient(
                    colors = colors,
                    startX = startX,
                    endX = endX
                )
                drawRect(brush = brush, blendMode = BlendMode.DstIn)
            }
    )
}

enum class Timeframe {
    Daily, Weekly, Monthly
}

fun parseFiatAmount(amount: String): Double {
    return amount.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
}

@Composable
fun SimpleLineChart(data: List<Double>, modifier: Modifier = Modifier, color: Color) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val maxVal = data.maxOrNull() ?: 1.0
        val range = if (maxVal == 0.0) 1.0 else maxVal

        val width = size.width
        val height = size.height
        val xStep = if (data.size > 1) width / (data.size - 1) else 0f

        val path = androidx.compose.ui.graphics.Path()

        data.forEachIndexed { index, value ->
            val x = index * xStep
            val y = height - ((value / range) * height).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
fun SimplePieChart(data: Map<String, Double>, modifier: Modifier = Modifier, isDarkTheme: Boolean) {
    val total = data.values.sum()
    val colors = listOf(
        Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6),
        Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFF4DB6AC),
        Color(0xFFFF8A65), Color(0xFFA1887F), Color(0xFF90A4AE), Color(0xFF7986CB)
    )

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                data.entries.forEachIndexed { index, entry ->
                    val sweepAngle = ((entry.value / total) * 360).toFloat()
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
                drawCircle(
                    color = if (isDarkTheme) Color(0xFF121212) else Color.White,
                    radius = size.minDimension / 4
                )
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            data.entries.forEachIndexed { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(colors[index % colors.size], RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${entry.key} (${String.format("%.1f", (entry.value / total) * 100)}%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun Dashboard(
    modifier: Modifier = Modifier,
    currency: String,
    themeViewModel: ThemeViewModel,
    transactionViewModel: TransactionViewModel
) {
    var selectedTimeframe by remember { mutableStateOf(Timeframe.Daily) }
    val isDarkTheme = themeViewModel.isDarkTheme.value
    val transactions = transactionViewModel.transactions
    val currencySymbol = getCurrencySymbol(currency)

    var exchangeRate by remember { mutableDoubleStateOf(1.0) }
    val rateHandler = remember { RateHandler(themeViewModel) }

    LaunchedEffect(currency) {
        if (currency != "AUD") {
            val rate = rateHandler.fetchFiatRate("AUD", currency)
            if (rate != null) exchangeRate = rate
        } else {
            exchangeRate = 1.0
        }
    }
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()

    val filteredTransactions = transactions.filter { tx ->
        val txTime = tx.timestamp
        calendar.timeInMillis = now
        when(selectedTimeframe) {
            Timeframe.Daily -> {
                
                val currentDay = calendar.get(Calendar.DAY_OF_YEAR)
                val currentYear = calendar.get(Calendar.YEAR)
                calendar.timeInMillis = txTime
                calendar.get(Calendar.DAY_OF_YEAR) == currentDay && calendar.get(Calendar.YEAR) == currentYear
            }
            Timeframe.Weekly -> {
                
                txTime >= now - (7L * 24 * 60 * 60 * 1000)
            }
            Timeframe.Monthly -> {
                
                txTime >= now - (30L * 24 * 60 * 60 * 1000)
            }
        }
    }

    val totalSalesCount = filteredTransactions.size
    val grossRevenue = filteredTransactions.sumOf { parseFiatAmount(it.fiatAmount) } * exchangeRate
    val averageSales = if (totalSalesCount > 0) grossRevenue / totalSalesCount else 0.0

    val coinBreakdown = filteredTransactions.groupingBy { it.coinSymbol }
        .eachCount().mapValues { it.value.toDouble() }

    
    val feesPaidByMerchant = filteredTransactions.filter { !it.feePassedToCustomer }.sumOf { parseFiatAmount(it.feeFiat) } * exchangeRate
    val feesPassedToCustomer = filteredTransactions.filter { it.feePassedToCustomer }.sumOf { parseFiatAmount(it.feeFiat) } * exchangeRate


    
    val graphData = remember(filteredTransactions, selectedTimeframe, exchangeRate) {
        val buckets = when (selectedTimeframe) {
            Timeframe.Daily -> MutableList(24) { 0.0 }
            Timeframe.Weekly -> MutableList(7) { 0.0 }
            Timeframe.Monthly -> MutableList(5) { 0.0 }
        }

        val calculationCalendar = Calendar.getInstance()

        filteredTransactions.forEach { tx ->
            val amount = parseFiatAmount(tx.fiatAmount) * exchangeRate

            when (selectedTimeframe) {
                Timeframe.Daily -> {
                    calculationCalendar.timeInMillis = tx.timestamp
                    val hour = calculationCalendar.get(Calendar.HOUR_OF_DAY)
                    if (hour in 0..23) buckets[hour] += amount
                }
                Timeframe.Weekly -> {
                    val diff = now - tx.timestamp
                    val daysAgo = (diff / (24 * 60 * 60 * 1000)).toInt()
                    val index = 6 - daysAgo
                    if (index in 0..6) buckets[index] += amount
                }
                Timeframe.Monthly -> {
                    val diff = now - tx.timestamp
                    val daysAgo = (diff / (24 * 60 * 60 * 1000)).toInt()
                    val weeksAgo = daysAgo / 7
                    val index = 4 - weeksAgo
                    if (index in 0..4) buckets[index] += amount
                }
            }
        }
        buckets
    }

    val chartColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Top Logo",
            modifier = Modifier.height(60.dp),
            colorFilter = if (isDarkTheme) ColorFilter.tint(Color.White) else null
        )

        Text("Payment Summary", style = MaterialTheme.typography.headlineMedium)

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Timeframe.entries.forEachIndexed { index, timeframe ->
                SegmentedButton(
                    selected = selectedTimeframe == timeframe,
                    onClick = { selectedTimeframe = timeframe },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = Timeframe.entries.size)
                ) {
                    Text(timeframe.name)
                }
            }
        }

        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sales", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Gross Revenue", style = MaterialTheme.typography.labelMedium)
                        Text("$currencySymbol${String.format("%.2f", grossRevenue)}", style = MaterialTheme.typography.titleMedium)
                    }
                    Column {
                        Text("Average Sales", style = MaterialTheme.typography.labelMedium)
                        Text("$currencySymbol${String.format("%.2f", averageSales)}", style = MaterialTheme.typography.titleMedium)
                    }
                    Column {
                        Text("Total Sales", style = MaterialTheme.typography.labelMedium)
                        Text("$totalSalesCount", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                if (graphData.isNotEmpty()) {
                    SimpleLineChart(
                        data = graphData,
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        color = chartColor
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text("No data for this period")
                    }
                }
            }
        }

        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Coin Breakdown", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                if (coinBreakdown.isNotEmpty()) {
                    SimplePieChart(
                        data = coinBreakdown,
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        isDarkTheme = isDarkTheme
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No data available")
                    }
                }
            }
        }

        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Fees", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fees paid by merchant")
                    Text("$currencySymbol${String.format("%.2f", feesPaidByMerchant)}")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fees passed to customer")
                    Text("$currencySymbol${String.format("%.2f", feesPassedToCustomer)}")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        
        Image(
            painter = painterResource(id = R.drawable.biglogo),
            contentDescription = "Bottom Logo",
            modifier = Modifier.fillMaxWidth(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
            colorFilter = if (!isDarkTheme) ColorFilter.tint(Color.Black) else null
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun KeyPad(modifier: Modifier = Modifier, currency: String, onPayClick: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Home Screen", modifier = Modifier.padding(vertical = 16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ){
            val currencySymbol = getCurrencySymbol(currency)
            Text(
                text = currencySymbol + priceHandler.displayPrice,
                modifier = Modifier.padding(20.dp),
                fontSize = 50.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("00", "0", "back")
            )
            keys.forEach { rowKeys ->
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowKeys.forEach { key ->
                        if (key == "back") {
                            KeypadButton(
                                icon = R.drawable.back,
                                onClick = { priceHandler.priceChange = "d"; priceHandler.priceUpdate() }
                            )
                        } else {
                            KeypadButton(
                                text = key,
                                onClick = { priceHandler.priceChange = key; priceHandler.priceUpdate() }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onPayClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "PAY",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun androidx.compose.foundation.layout.RowScope.KeypadButton(
    text: String? = null,
    icon: Int? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = "Backspace",
                modifier = Modifier.fillMaxHeight(0.5f).aspectRatio(1f)
            )
        } else {
            Text(
                text = text ?: "",
                fontSize = 30.sp
            )
        }
    }
}

@Composable
fun FavoritesScreen(modifier: Modifier = Modifier, transactionViewModel: TransactionViewModel, themeViewModel: ThemeViewModel) {
    val transactions = transactionViewModel.transactions
    val currentCurrency = themeViewModel.currency.value
    val rateHandler = remember { RateHandler(themeViewModel) }
    val conversionRates = remember(currentCurrency) { mutableStateMapOf<String, Double>() }

    LaunchedEffect(transactions, currentCurrency) {
        withContext(Dispatchers.IO) {
            val uniqueCurrencies = transactions.map { it.currency }.distinct()
            uniqueCurrencies.forEach { sourceCurrency ->
                val rate = rateHandler.fetchFiatRate(sourceCurrency, currentCurrency)
                conversionRates[sourceCurrency] = rate
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Transactions", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No transactions yet.")
            }
        } else {
            val groupedTransactions = transactions.groupBy { 
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.timestamp))
            }
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                groupedTransactions.forEach { (date, txs) ->
                    item {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(txs) { transaction ->
                        val rate = conversionRates[transaction.currency]
                        TransactionItem(transaction, rate ?: 1.0, currentCurrency)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, rate: Double, displayCurrency: String) {
    var expanded by remember { mutableStateOf(false) }
    val formattedDate = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(transaction.timestamp))
    }

    
    val amount = remember(transaction.fiatAmount) {
        parseFiatAmount(transaction.fiatAmount)
    }
    
    val convertedAmount = amount * rate

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${getCurrencySymbol(displayCurrency)}${String.format("%.2f", convertedAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand",
                    modifier = Modifier.rotate(if (expanded) 180f else 0f)
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Crypto Amount: ${transaction.cryptoAmount} ${transaction.coinSymbol}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Hash:", fontWeight = FontWeight.Bold)
                Text(transaction.txHash, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                
                val qrBitmap = generateQR(transaction.txUrl)
                if (qrBitmap != null) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Transaction Link QR",
                            modifier = Modifier.size(150.dp)
                        )
                    }
                }

                
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                var isPrinting by remember { mutableStateOf(false) }
                val isPrinterConnected = MainActivity.isPrinterServiceConnected

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (!isPrinting && isPrinterConnected) {
                            isPrinting = true
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val cryptoAmt = transaction.cryptoAmount.toDoubleOrNull() ?: 0.0
                                    val printableFiatAmount = String.format("%.2f", convertedAmount)
                                    MainActivity.printReceipt(
                                        context,
                                        transaction.txHash,
                                        transaction.txUrl,
                                        printableFiatAmount,
                                        cryptoAmt,
                                        transaction.coinSymbol,
                                        displayCurrency
                                    )
                                }
                                isPrinting = false
                            }
                        }
                    },
                    enabled = isPrinterConnected && !isPrinting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                     if (isPrinting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Printing...")
                    } else {
                        Text("Print Receipt")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    currentCurrency: String,
    onCurrencyChange: (String) -> Unit,
    onSurchargesClick: () -> Unit,
    onEnabledCoinsClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currencies = listOf("AUD", "USD", "GBP", "EUR")

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings Screen", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dark Mode")
            Switch(
                checked = isDarkTheme,
                onCheckedChange = { onThemeToggle() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Local Currency")
            Box {
                Button(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(currentCurrency)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    currencies.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text(currency) },
                            onClick = {
                                onCurrencyChange(currency)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onSurchargesClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Manage Surcharges")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onEnabledCoinsClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Select Cryptocurrencies")
        }
    }
}

@Composable
fun SurchargesScreen(
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel
) {
    val cryptoSymbols = listOf("BTC", "ETH", "LTC", "TRX", "ATOM", "XRP", "SOL", "BNB", "USDT", "USDC")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Surcharges / Discounts", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(cryptoSymbols, key = { it }) { symbol ->
                SurchargeItem(symbol = symbol, themeViewModel = themeViewModel)
            }
        }
    }
}

@Composable
fun SelectCoinsScreen(
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel
) {
    val cryptoSymbols = listOf("BTC", "ETH", "LTC", "TRX", "ATOM", "XRP", "SOL", "BNB", "USDT", "USDC")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Enabled Cryptocurrencies", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(cryptoSymbols, key = { it }) { symbol ->
                var isEnabled by remember { mutableStateOf(themeViewModel.enabledCoins[symbol] ?: true) }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { 
                        isEnabled = !isEnabled
                        themeViewModel.setCoinEnabled(symbol, isEnabled)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = symbol, style = MaterialTheme.typography.titleLarge)
                        Checkbox(
                            checked = isEnabled,
                            onCheckedChange = { 
                                isEnabled = it
                                themeViewModel.setCoinEnabled(symbol, it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SurchargeItem(symbol: String, themeViewModel: ThemeViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val currentVal = themeViewModel.cryptoSurcharges[symbol] ?: 0.0
    
    var surchargeText by remember(symbol) { 
        mutableStateOf(if (currentVal > 0) currentVal.toString().removeSuffix(".0") else "") 
    }
    var discountText by remember(symbol) { 
        mutableStateOf(if (currentVal < 0) kotlin.math.abs(currentVal).toString().removeSuffix(".0") else "") 
    }
    
    var isFeeEnabled by remember(symbol) { mutableStateOf(themeViewModel.passFeesOn[symbol] ?: false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = symbol, 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand",
                    modifier = Modifier.rotate(if (expanded) 180f else 0f)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            isFeeEnabled = !isFeeEnabled 
                            themeViewModel.setPassFee(symbol, isFeeEnabled)
                        }
                ) {
                    Checkbox(
                        checked = isFeeEnabled,
                        onCheckedChange = { 
                            isFeeEnabled = it 
                            themeViewModel.setPassFee(symbol, it)
                        }
                    )
                    Text(text = "Pass Network Fees to Customer")
                }

                Spacer(modifier = Modifier.height(8.dp))

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    
                    TextField(
                        value = surchargeText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { char -> char.isDigit() || char == '.' }) {
                                surchargeText = newValue
                                if (newValue.isNotEmpty()) {
                                    discountText = ""
                                    val valDouble = newValue.toDoubleOrNull() ?: 0.0
                                    themeViewModel.setSurcharge(symbol, valDouble)
                                } else {
                                    if (discountText.isEmpty()) {
                                        themeViewModel.setSurcharge(symbol, 0.0)
                                    }
                                }
                            }
                        },
                        label = { Text("Surcharge %") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    
                    TextField(
                        value = discountText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { char -> char.isDigit() || char == '.' }) {
                                discountText = newValue
                                if (newValue.isNotEmpty()) {
                                    surchargeText = ""
                                    val valDouble = newValue.toDoubleOrNull() ?: 0.0
                                    themeViewModel.setSurcharge(symbol, -valDouble)
                                } else {
                                    if (surchargeText.isEmpty()) {
                                        themeViewModel.setSurcharge(symbol, 0.0)
                                    }
                                }
                            }
                        },
                        label = { Text("Discount %") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmingScreen(modifier: Modifier = Modifier, confirmations: Int, isDarkTheme: Boolean) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(150.dp), strokeWidth = 4.dp, color = if (isDarkTheme) Color.White else Color.Black)
            WipeAnimatedLogo(Modifier.size(80.dp), colorFilter = if (isDarkTheme) ColorFilter.tint(Color.White) else null)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Transaction Confirming",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Confirmations: $confirmations",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun PaymentScreen(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    currentCurrency: String,
    onCWalletClick: () -> Unit
) {
    val currencySymbol = getCurrencySymbol(currentCurrency)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Image(
            painter = painterResource(id = R.drawable.pay2),
            contentDescription = "Pay",
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = Modifier.size(150.dp),
            colorFilter = if (isDarkTheme) ColorFilter.tint(Color.White) else null
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Total",
            style = androidx.compose.ui.text.TextStyle(fontSize = 25.sp)
        )
        Text(
            text = currencySymbol + priceHandler.displayPrice,
            style = androidx.compose.ui.text.TextStyle(fontSize = 40.sp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.cards),
                contentDescription = "cards",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.size(240.dp),
            )
        }

        Button(
            onClick = onCWalletClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Pay With Crypto",
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

fun getCurrencySymbol(currency: String): String {
    return when(currency) {
        "EUR" -> "€"
        "GBP" -> "£"
        "USD" -> "$"
        "AUD" -> "A$"
        else -> "$"
    }
}

@Composable
fun CWallet(modifier: Modifier = Modifier, themeViewModel: ThemeViewModel, currentCurrency: String, onBTCClick: () -> Unit, onETHClick: () -> Unit, onLTCClick: () -> Unit, onTRXClick: () -> Unit, onATOMClick: () -> Unit, onSOLClick: () -> Unit, onBNBClick: () -> Unit, onXRPClick: () -> Unit) {
    val context = LocalContext.current
    val feeMap = remember { mutableStateMapOf<String, Double>() }
    val rateHandler = remember { RateHandler(themeViewModel) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            listOf("BTC", "ETH", "LTC", "TRX", "ATOM", "XRP", "SOL", "BNB", "USDT", "USDC").forEach { symbol ->
                if (themeViewModel.passFeesOn[symbol] == true) {
                    val fee = rateHandler.getFeeInFiat(symbol, currentCurrency)
                    if (fee != null) {
                        feeMap[symbol] = fee
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Currency",
            style = androidx.compose.ui.text.TextStyle(fontSize = 25.sp),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val currencies = listOf(
                "BTC" to R.drawable.btc, "ETH" to R.drawable.eth,
                "LTC" to R.drawable.ltc, "TRX" to R.drawable.trx,
                "ATOM" to R.drawable.atom, "XRP" to R.drawable.xrp,
                "SOL" to R.drawable.sol, "BNB" to R.drawable.bnb,
                "USDT" to R.drawable.usdt, "USDC" to R.drawable.usdc
            ).filter { (name, _) -> 
                themeViewModel.enabledCoins[name] ?: true
            }

            currencies.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { (name, icon) ->
                        val surcharge = themeViewModel.cryptoSurcharges[name] ?: 0.0
                        var summaryText: String? = null
                        
                        val feeVal = feeMap[name]
                        val feeText = if (feeVal != null) {
                            val currencySymbol = getCurrencySymbol(currentCurrency)
                            "${currencySymbol}${String.format("%.2f", feeVal)} Fee"
                        } else null

                        if (surcharge > 0) {
                            summaryText = "${surcharge.toString().removeSuffix(".0")}% Surcharge"
                        } else if (surcharge < 0) {
                            summaryText = "${kotlin.math.abs(surcharge).toString().removeSuffix(".0")}% Discount"
                        }

                        if (feeText != null) {
                            if (summaryText != null) {
                                summaryText += " & $feeText"
                            } else {
                                summaryText = feeText
                            }
                        }

                        CWalletButton(
                            text = name,
                            icon = icon,
                            summaryText = summaryText,
                            onClick = {
                                val seedPhrase = generateSeedPhrase(context).generateBIP39Phrase()
                                when (name) {
                                    "BTC" -> {
                                        CheckSeed(context).checkBitcoinWallet()
                                        Values.BTCAddress = CheckSeed.receiveAddress
                                        onBTCClick()
                                    }
                                    "ETH" -> {
                                        CheckEthSeed(context).checkEthereumWallet()
                                        Values.EthAddress = CheckEthSeed.receiveAddress
                                        onETHClick()
                                    }
                                    "LTC" -> {
                                        CheckLtcSeed(context).checkLitecoinWallet()
                                        Values.LTCAddress = CheckLtcSeed.receiveAddress
                                        onLTCClick()
                                    }
                                    "TRX" -> {
                                        CheckTrxSeed(context).checkTronWallet()
                                        Values.TRXAdress = CheckTrxSeed.receiveAddress
                                        onTRXClick()
                                    }
                                    "ATOM" -> {
                                        CheckAtomSeed(context).checkCosmosWallet()
                                        Values.ATOMAddress = CheckAtomSeed.receiveAddress
                                        onATOMClick()
                                    }
                                    "SOL" -> {
                                        CheckSolanaSeed(context).checkSolanaWallet()
                                        Values.SOLAddress = CheckSolanaSeed.receiveAddress
                                        onSOLClick()
                                    }
                                    "BNB" -> {
                                        CheckBnbSeed(context).checkBNBWallet(seedPhrase)
                                        Values.BNBAddress = CheckBnbSeed.receiveAddress
                                        onBNBClick()
                                    }
                                    "XRP" -> {
                                        CheckXrpSeed(context).checkXrpWallet(seedPhrase)
                                        Values.XRPAddress = CheckXrpSeed.receiveAddress
                                        onXRPClick()
                                    }
                                }
                            }
                        )
                    }
                    
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun androidx.compose.foundation.layout.RowScope.CWalletButton(
    text: String,
    icon: Int,
    summaryText: String? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(4.dp)
    ) {
        @Suppress("DEPRECATION")
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = text,
                    modifier = Modifier.fillMaxHeight(0.8f).aspectRatio(1f)
                )
            }
            Text(
                text = text,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (summaryText != null) {
                Text(
                    text = summaryText,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Any,
) {
    DASHBOARD("Dashboard", Icons.Default.Home),
    KEYPAD("Keypad", R.drawable.bill),
    FAVORITES("Transactions", Icons.AutoMirrored.Filled.List),
    PROFILE("Settings", Icons.Default.Settings),
    PAYMENT(label = "Payment", Icons.Default.Check),
    CWALLET(label = "CWALLET", Icons.Default.Face),
    SURCHARGES(label = "Surcharges", Icons.Default.Settings),
    ENABLED_COINS(label = "Enabled Coins", Icons.Default.Settings),
    BTCPAYMENT(label = "BTCPAYMEMT", Icons.Default.Face),
    BTCPAID(label = "BTCPAID", Icons.Default.Face),
    BTCOVERPAID(label = "BTCOVERPAID", Icons.Default.Face),
    BTCUNDERPAID(label = "BTCUNDERPAID", Icons.Default.Face),
    ETHPAYMENT(label = "ETHPAYMENT", Icons.Default.Face),
    ETHPAID(label = "ETHPAID", Icons.Default.Face),
    ETHOVERPAID(label = "ETHOVERPAID", Icons.Default.Face),
    ETHUNDERPAID(label = "ETHUNDERPAID", Icons.Default.Face),
    LTCPAYMENT(label = "LTCPAYMENT", Icons.Default.Face),
    LTCPAID(label = "LTCPAID", Icons.Default.Face),
    LTCOVERPAID(label = "LTCOVERPAID", Icons.Default.Face),
    LTCUNDERPAID(label = "LTCUNDERPAID", Icons.Default.Face),
    TRXPAYMENT(label = "TRXPAYMENT", Icons.Default.Face),
    TRXPAID(label = "TRXPAID", Icons.Default.Face),
    TRXOVERPAID(label = "TRXOVERPAID", Icons.Default.Face),
    TRXUNDERPAID(label = "TRXUNDERPAID", Icons.Default.Face),
    ATOMPAYMENT(label = "ATOMPAYMENT", Icons.Default.Face),
    ATOMPAID(label = "ATOMPAID", Icons.Default.Face),
    ATOMOVERPAID(label = "ATOMOVERPAID", Icons.Default.Face),
    ATOMUNDERPAID(label = "ATOMUNDERPAID", Icons.Default.Face),
    SOLPAYMENT(label = "SOLPAYMENT", Icons.Default.Face),
    SOLPAID(label = "SOLPAID", Icons.Default.Face),
    SOLOVERPAID(label = "SOLOVERPAID", Icons.Default.Face),
    SOLUNDERPAID(label = "SOLUNDERPAID", Icons.Default.Face),
    BNBPAYMENT(label = "BNBPAYMENT", Icons.Default.Face),
    BNBPAID(label = "BNBPAID", Icons.Default.Face),
    BNBOVERPAID(label = "BNBOVERPAID", Icons.Default.Face),
    BNBUNDERPAID(label = "BNBUNDERPAID", Icons.Default.Face),
    XRPPAYMENT(label = "XRPPAYMENT", Icons.Default.Face),
    XRPPAID(label = "XRPPAID", Icons.Default.Face),
    XRPOVERPAID(label = "XRPOVERPAID", Icons.Default.Face),
    XRPUNDERPAID(label = "XRPUNDERPAID", Icons.Default.Face),
    CONFIRMING(label = "CONFIRMING", Icons.Default.Face)
}

@Composable
fun BTCPayment(modifier: Modifier = Modifier, currency: String, vm: ThemeViewModel){
    var price by remember { mutableDoubleStateOf(0.0) }; val context = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { CheckSeed(context).checkBitcoinWallet(); Values.BTCAddress = CheckSeed.receiveAddress; RateHandler(vm).calculateCryptoAmount("BTC", currency, priceHandler.displayPrice.toDouble())?.let { price = it; MainActivity.ppc = it; BalanceHandler(context).checkBalance() } } }
    if (MainActivity.transactionId.isNotEmpty()) {
        ConfirmingScreen(modifier, MainActivity.confirmations, vm.isDarkTheme.value)
    } else {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) { generateQR(Values.BTCAddress)?.let { Image(it.asImageBitmap(), null) }; Spacer(Modifier.size(14.dp)); Text(Values.BTCAddress); Text("Price: ${String.format("%.8f", price)}") }
    }
}

@Composable
fun ETHPayment(modifier: Modifier = Modifier, currency: String, vm: ThemeViewModel) {
    var price by remember { mutableDoubleStateOf(0.0) }; val context = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { CheckEthSeed(context).checkEthereumWallet(); Values.EthAddress = CheckEthSeed.receiveAddress; RateHandler(vm).calculateCryptoAmount("ETH", currency, priceHandler.displayPrice.toDouble())?.let { price = it; MainActivity.ppc = it; EthBalanceHandler(context).checkBalance() } } }
    if (MainActivity.transactionId.isNotEmpty()) {
        ConfirmingScreen(modifier, MainActivity.confirmations, vm.isDarkTheme.value)
    } else {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) { generateQR(Values.EthAddress)?.let { Image(it.asImageBitmap(), null) }; Spacer(Modifier.size(14.dp)); Text(Values.EthAddress); Text("Price: ${String.format("%.8f", price)}") }
    }
}

@Composable
fun LTCPayment(modifier: Modifier = Modifier, currency: String, vm: ThemeViewModel) {
    var price by remember { mutableDoubleStateOf(0.0) }; val context = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { CheckLtcSeed(context).checkLitecoinWallet(); Values.LTCAddress = CheckLtcSeed.receiveAddress; RateHandler(vm).calculateCryptoAmount("LTC", currency, priceHandler.displayPrice.toDouble())?.let { price = it; MainActivity.ppc = it; LtcBalanceHandler(context).checkBalance() } } }
    if (MainActivity.transactionId.isNotEmpty()) {
        ConfirmingScreen(modifier, MainActivity.confirmations, vm.isDarkTheme.value)
    } else {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) { generateQR(Values.LTCAddress)?.let { Image(it.asImageBitmap(), null) }; Spacer(Modifier.size(14.dp)); Text(Values.LTCAddress); Text("Price: ${String.format("%.6f", price)}") }
    }
}

@Composable
fun TRXPayment(modifier: Modifier = Modifier, currency: String, vm: ThemeViewModel) {
    var price by remember { mutableDoubleStateOf(0.0) }; val context = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { CheckTrxSeed(context).checkTronWallet(); Values.TRXAdress = CheckTrxSeed.receiveAddress; RateHandler(vm).calculateCryptoAmount("TRX", currency, priceHandler.displayPrice.toDouble())?.let { price = it; MainActivity.ppc = it; TrxBalanceHandler(context).checkBalance(currency, vm.passFeesOn["TRX"] ?: false, RateHandler(vm)) } } }
    if (MainActivity.transactionId.isNotEmpty()) {
        ConfirmingScreen(modifier, MainActivity.confirmations, vm.isDarkTheme.value)
    } else {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) { generateQR(Values.TRXAdress)?.let { Image(it.asImageBitmap(), null) }; Spacer(Modifier.size(14.dp)); Text(Values.TRXAdress); Text("Price: ${String.format("%.6f", price)}") }
    }
}

@Composable
fun ATOMPayment(modifier: Modifier = Modifier, currency: String, vm: ThemeViewModel) {
    var price by remember { mutableDoubleStateOf(0.0) }; val context = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { CheckAtomSeed(context).checkCosmosWallet(); Values.ATOMAddress = CheckAtomSeed.receiveAddress; RateHandler(vm).calculateCryptoAmount("ATOM", currency, priceHandler.displayPrice.toDouble())?.let { price = it; MainActivity.ppc = it; AtomBalanceHandler(context).checkBalance() } } }
    if (MainActivity.transactionId.isNotEmpty()) {
        ConfirmingScreen(modifier, MainActivity.confirmations, vm.isDarkTheme.value)
    } else {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) { generateQR(Values.ATOMAddress)?.let { Image(it.asImageBitmap(), null) }; Spacer(Modifier.size(14.dp)); Text(Values.ATOMAddress); Text("Price: ${String.format("%.6f", price)}") }
    }
}

@Composable
fun SOLPayment(modifier: Modifier = Modifier, currency: String, vm: ThemeViewModel) {
    var price by remember { mutableDoubleStateOf(0.0) }; val context = LocalContext.current
    LaunchedEffect(Unit) { RateHandler(vm).calculateCryptoAmount("SOL", currency, priceHandler.displayPrice.toDouble())?.let { price = it; MainActivity.ppc = it; withContext(Dispatchers.IO) { SolanaHandler(context).checkBalance(currency, vm.passFeesOn["SOL"] ?: false, RateHandler(vm)) } } }
    if (MainActivity.transactionId.isNotEmpty()) {
        ConfirmingScreen(modifier, MainActivity.confirmations, vm.isDarkTheme.value)
    } else {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) { generateQR(Values.SOLAddress)?.let { Image(it.asImageBitmap(), null) }; Spacer(Modifier.size(14.dp)); Text(Values.SOLAddress); Text("Price: ${String.format("%.6f", price)}") }
    }
}

@Composable
fun BNBPayment(modifier: Modifier = Modifier, currency: String, vm: ThemeViewModel) {
    var price by remember { mutableDoubleStateOf(0.0) }; val context = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { val s = generateSeedPhrase(context).generateBIP39Phrase(); CheckBnbSeed(context).checkBNBWallet(s); Values.BNBAddress = CheckBnbSeed.receiveAddress; RateHandler(vm).calculateCryptoAmount("BNB", currency, priceHandler.displayPrice.toDouble())?.let { price = it; MainActivity.ppc = it; BnbBalanceHandler(context).checkBalance() } } }
    if (MainActivity.transactionId.isNotEmpty()) {
        ConfirmingScreen(modifier, MainActivity.confirmations, vm.isDarkTheme.value)
    } else {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) { generateQR(Values.BNBAddress)?.let { Image(it.asImageBitmap(), null) }; Spacer(Modifier.size(14.dp)); Text(Values.BNBAddress); Text("Price: ${String.format("%.8f", price)}") }
    }
}

@Composable
fun XRPPayment(modifier: Modifier = Modifier, currency: String, vm: ThemeViewModel) {
    var price by remember { mutableDoubleStateOf(0.0) }; val context = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { val s = generateSeedPhrase(context).generateBIP39Phrase(); CheckXrpSeed(context).checkXrpWallet(s); Values.XRPAddress = CheckXrpSeed.receiveAddress; RateHandler(vm).calculateCryptoAmount("XRP", currency, priceHandler.displayPrice.toDouble())?.let { price = it; MainActivity.ppc = it; XrpBalanceHandler(context).checkBalance() } } }
    if (MainActivity.transactionId.isNotEmpty()) {
        ConfirmingScreen(modifier, MainActivity.confirmations, vm.isDarkTheme.value)
    } else {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) { generateQR(Values.XRPAddress)?.let { Image(it.asImageBitmap(), null) }; Spacer(Modifier.size(14.dp)); Text(Values.XRPAddress); Text("Price: ${String.format("%.6f", price)}") }
    }
}

@Composable
fun PrintReceiptButton(txUrl: String, cryptoSymbol: String, currency: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }
    val isPrinterConnected = MainActivity.isPrinterServiceConnected
    
    Button(
        onClick = {
            if (!isPrinting && isPrinterConnected) {
                isPrinting = true
                scope.launch {
                    withContext(Dispatchers.IO) {
                        MainActivity.printReceipt(
                            context,
                            MainActivity.transactionId,
                            txUrl,
                            priceHandler.displayPrice,
                            MainActivity.ppc,
                            cryptoSymbol,
                            currency
                        )
                    }
                    isPrinting = false
                }
            }
        },
        enabled = isPrinterConnected && !isPrinting,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isPrinting) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Printing...")
        } else {
            Text("Print Receipt")
        }
    }
}

@Composable
fun PaymentStatusScreen(symbol: String, baseUrl: String, cur: String, vm: TransactionViewModel, status: String = "Approved") {
    val txId = MainActivity.transactionId; val url = baseUrl + txId
    LaunchedEffect(txId) { if (txId.isNotEmpty()) vm.addTransaction(Transaction(priceHandler.displayPrice, cur, String.format("%.8f", MainActivity.ppc), symbol, txId, url, MainActivity.lastFeePaidFiat, MainActivity.lastFeePassedToCustomer)) }
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        if (txId.isNotEmpty()) { generateQR(url)?.let { Image(it.asImageBitmap(), null, Modifier.size(200.dp)) }; PrintReceiptButton(url, symbol, cur) }
        else WipeAnimatedLogo(Modifier.size(100.dp))
        Spacer(Modifier.size(14.dp)); Text("Payment $status")
    }
}


@Composable fun BTCApproved(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("BTC", "https://mempool.space/tx/", cur, vm) }
@Composable fun BTCOverpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("BTC", "https://mempool.space/tx/", cur, vm, "Overpaid") }
@Composable fun BTCUnderpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("BTC", "https://mempool.space/tx/", cur, vm, "Underpaid") }
@Composable fun ETHApproved(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("ETH", "https://etherscan.io/tx/", cur, vm) }
@Composable fun ETHOverpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("ETH", "https://etherscan.io/tx/", cur, vm, "Overpaid") }
@Composable fun ETHUnderpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("ETH", "https://etherscan.io/tx/", cur, vm, "Underpaid") }
@Composable fun LTCApproved(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("LTC", "https://live.blockcypher.com/ltc/tx/", cur, vm) }
@Composable fun LTCOverpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("LTC", "https://live.blockcypher.com/ltc/tx/", cur, vm, "Overpaid") }
@Composable fun LTCUnderpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("LTC", "https://live.blockcypher.com/ltc/tx/", cur, vm, "Underpaid") }
@Composable fun TRXApproved(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("TRX", "https://tronscan.org/#/transaction/", cur, vm) }
@Composable fun TRXOverpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("TRX", "https://tronscan.org/#/transaction/", cur, vm, "Overpaid") }
@Composable fun TRXUnderpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("TRX", "https://tronscan.org/#/transaction/", cur, vm, "Underpaid") }
@Composable fun ATOMApproved(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("ATOM", "https://www.mintscan.io/cosmos/tx/", cur, vm) }
@Composable fun ATOMOverpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("ATOM", "https://www.mintscan.io/cosmos/tx/", cur, vm, "Overpaid") }
@Composable fun ATOMUnderpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("ATOM", "https://www.mintscan.io/cosmos/tx/", cur, vm, "Underpaid") }
@Composable fun SOLApproved(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("SOL", "https://solscan.io/tx/", cur, vm) }
@Composable fun SOLOverpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("SOL", "https://solscan.io/tx/", cur, vm, "Overpaid") }
@Composable fun SOLUnderpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("SOL", "https://solscan.io/tx/", cur, vm, "Underpaid") }
@Composable fun BNBApproved(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("BNB", "https://bscscan.com/tx/", cur, vm) }
@Composable fun BNBOverpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("BNB", "https://bscscan.com/tx/", cur, vm, "Overpaid") }
@Composable fun BNBUnderpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("BNB", "https://bscscan.com/tx/", cur, vm, "Underpaid") }
@Composable fun XRPApproved(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("XRP", "https://xrpscan.com/tx/", cur, vm) }
@Composable fun XRPOverpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("XRP", "https://xrpscan.com/tx/", cur, vm, "Overpaid") }
@Composable fun XRPUnderpaid(modifier: Modifier, cur: String, vm: TransactionViewModel, themeViewModel: ThemeViewModel) { PaymentStatusScreen("XRP", "https://xrpscan.com/tx/", cur, vm, "Underpaid") }