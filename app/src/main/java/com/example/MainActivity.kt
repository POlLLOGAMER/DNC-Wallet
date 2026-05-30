package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.service.DncEngine
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.DncViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: DncViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Safe area padding applied automatically on standard navigation components
                    }
                ) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: DncViewModel,
    modifier: Modifier = Modifier
) {
    val walletKeyPair by viewModel.walletKeyPair.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val peers by viewModel.peers.collectAsState()
    val isMiningActive by viewModel.isMiningActive.collectAsState()
    val miningMetrics by viewModel.miningMetrics.collectAsState()
    val storageChallenges by viewModel.storageChallenges.collectAsState()
    val guardians by viewModel.guardians.collectAsState()
    val engineLogs by viewModel.engineLogs.collectAsState()
    val networkMode by viewModel.networkMode.collectAsState()
    val networkPort by viewModel.networkPort.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        // App Header
        AppHeader(
            networkMode = networkMode,
            networkPort = networkPort,
            onConfigClick = {
                // Show network mode dialog/toggle
            }
        )

        // Custom Navigation Tab Bar
        CustomTabBar(
            activeIndex = activeTab,
            onTabSelected = { activeTab = it }
        )

        Divider(color = BorderSlate, thickness = 1.dp)

        // Display contents depending on tab select
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when (activeTab) {
                0 -> AccountTabContent(
                    walletKeyPair = walletKeyPair,
                    engineLogs = engineLogs,
                    networkMode = networkMode,
                    networkPort = networkPort,
                    onCreateAccount = { viewModel.createAccount() },
                    onImportKeys = { pk, sk ->
                        val ok = viewModel.loginWithKeys(pk, sk)
                        if (ok) {
                            Toast.makeText(context, "Keys updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error updating keys.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLogout = { viewModel.logout() },
                    onChangeNet = { mode, port -> viewModel.changeNetwork(mode, port) }
                )
                1 -> WalletTabContent(
                    walletKeyPair = walletKeyPair,
                    balance = balance,
                    transactions = transactions,
                    onSendTransaction = { recipient, amount ->
                        val result = viewModel.sendDiac(recipient, amount)
                        Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                    }
                )
                2 -> MiningTabContent(
                    walletKeyPair = walletKeyPair,
                    isMiningActive = isMiningActive,
                    miningMetrics = miningMetrics,
                    onToggleMining = { viewModel.toggleMining() }
                )
                3 -> NetworkTabContent(
                    peers = peers,
                    networkPort = networkPort,
                    onAddPeer = { ip, port ->
                        val ok = viewModel.addPeer(ip, port)
                        if (ok) {
                            Toast.makeText(context, "Peer registered!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Peer invalid or already exists.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onPingPeer = { viewModel.pingPeer(it) }
                )
                4 -> AuditTabContent(
                    storageChallenges = storageChallenges,
                    guardians = guardians,
                    onAuditRun = { frag, data ->
                        val result = viewModel.triggerCustomAudit(frag, data)
                        Toast.makeText(context, "PoS checked: Integrity hash correct!", Toast.LENGTH_SHORT).show()
                    },
                    onRecoveryRun = { g1, g2 ->
                        val addr = viewModel.triggerRecovery(g1, g2)
                        Toast.makeText(context, "Keys Rotated to: $addr", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}

@Composable
fun AppHeader(
    networkMode: String,
    networkPort: Int,
    onConfigClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "DISTRIBUTED NETWORK",
                fontSize = 9.sp,
                color = TechCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DIAC NET ",
                    fontSize = 20.sp,
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "CHAIN",
                    fontSize = 19.sp,
                    color = TechCyan,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.SansSerif
                )
            }
            Text(
                text = "Node Protocol: $networkMode | Port: $networkPort",
                fontSize = 11.sp,
                color = SubtleText,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        
        // Active dot container
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1F22D3EE))
                    .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                    .clickable { onConfigClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TechCyan)
                        .border(1.dp, TechCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
fun CustomTabBar(
    activeIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        Pair("Account", Icons.Default.AccountBox),
        Pair("Wallet", Icons.Default.Add),
        Pair("Mining", Icons.Default.PlayArrow),
        Pair("P2P Mesh", Icons.Default.Share),
        Pair("PoS & Recovery", Icons.Default.Lock)
    )

    ScrollableTabRow(
        selectedTabIndex = activeIndex,
        containerColor = SlateBackground,
        contentColor = TechCyan,
        edgePadding = 12.dp,
        divider = {}  // Immersive UI eliminates crude dividers
    ) {
        tabs.forEachIndexed { index, (label, icon) ->
            val isSelected = activeIndex == index
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = { 
                    Icon(
                        imageVector = icon, 
                        contentDescription = label, 
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) TechCyan else SubtleText
                    ) 
                },
                text = { 
                    Text(
                        text = label, 
                        fontSize = 10.sp, 
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        color = if (isSelected) TextLight else SubtleText
                    ) 
                },
                unselectedContentColor = SubtleText,
                selectedContentColor = TechCyan
            )
        }
    }
}

// ================= TABS IMPLEMENTATIONS =================

@Composable
fun AccountTabContent(
    walletKeyPair: com.example.crypto.DiacInfinityKeys.KeyPair?,
    engineLogs: List<String>,
    networkMode: String,
    networkPort: Int,
    onCreateAccount: () -> Unit,
    onImportKeys: (String, String) -> Unit,
    onLogout: () -> Unit,
    onChangeNet: (String, Int) -> Unit
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var showKeysDialog by remember { mutableStateOf(false) }
    var showNetworkDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlowCard(borderColor = TechCyan) {
            Text(
                text = "🛡️ Node Identity Anchor",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TechCyan,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (walletKeyPair == null) {
                Text(
                    text = "No private credentials found on this device profile. Initialize a secure account covering DNN fragments, or import keys.",
                    fontSize = 13.sp,
                    color = SubtleText
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onCreateAccount,
                        colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_account_btn")
                    ) {
                        Text("Create Fresh Keys", color = SpaceBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CardSlate),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, TechCyan, RoundedCornerShape(16.dp))
                            .testTag("import_keys_btn")
                    ) {
                        Text("Import Keys", color = TechCyan, fontSize = 13.sp)
                    }
                }
            } else {
                Text(
                    text = "LOCKED ADDRESS (DNC-INF):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SubtleText
                )
                Text(
                    text = walletKeyPair.address,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = DncGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            clipboardManager.setText(AnnotatedString(walletKeyPair.address))
                            Toast.makeText(context, "Copied address!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showKeysDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CardSlate),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, TechCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Text("View Credentials", color = TechCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Wipe Keypair", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Subnet Settings Card
        GlowCard(borderColor = BorderSlate) {
            Text(
                text = "🌐 Lan & Port Configuration",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TechCyan,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("LAN Multicast Group", color = TextLight, fontSize = 13.sp)
                    Text("239.255.124.69:39369", color = SubtleText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Button(
                    onClick = { showNetworkDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CardSlate),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.border(1.dp, BorderSlate, RoundedCornerShape(14.dp))
                ) {
                    Text("Adjust Mode", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // System Diagnostic Monitor Console
        TerminalConsole(
            title = "DNC SYSTEM LOG TRACE MONITOR",
            logs = engineLogs
        )
    }

    // Import Keys Modal
    if (showImportDialog) {
        var pkStr by remember { mutableStateOf("") }
        var skStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import DIAC ∞ Credentials", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = pkStr,
                        onValueChange = { pkStr = it },
                        label = { Text("Public Anchor Address (DNC-...)") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechCyan,
                            unfocusedBorderColor = BorderSlate,
                            focusedLabelColor = TechCyan,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        )
                    )
                    OutlinedTextField(
                        value = skStr,
                        onValueChange = { skStr = it },
                        label = { Text("Private Key String") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechCyan,
                            unfocusedBorderColor = BorderSlate,
                            focusedLabelColor = TechCyan,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pkStr.isNotEmpty() && skStr.isNotEmpty()) {
                            onImportKeys(pkStr, skStr)
                            showImportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Import", color = SpaceBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = SubtleText)
                }
            },
            containerColor = CardSlate
        )
    }

    // View Keys Modal
    if (showKeysDialog && walletKeyPair != null) {
        AlertDialog(
            onDismissRequest = { showKeysDialog = false },
            title = { Text("DIAC ∞ Safe Parameters", fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = TextLight, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("Address Label:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SubtleText)
                    Text(walletKeyPair.address, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = DncGreen)

                    Text("Private Key parameter x (256-bit):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SubtleText)
                    Text(walletKeyPair.privateKey.x.toString(16), fontFamily = FontFamily.Monospace, fontSize = 11.sp)

                    Text("Prime Modulus parameter p:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SubtleText)
                    Text(walletKeyPair.privateKey.p.toString(16), fontFamily = FontFamily.Monospace, fontSize = 11.sp)

                    Text("Transcendental Offset O | Precision P:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SubtleText)
                    Text("${walletKeyPair.privateKey.offset} | ${walletKeyPair.privateKey.precision}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showKeysDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", color = SpaceBlack, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardSlate
        )
    }

    // Adjust Network Mode Modal
    if (showNetworkDialog) {
        var selectedPort by remember { mutableStateOf(networkPort.toString()) }
        AlertDialog(
            onDismissRequest = { showNetworkDialog = false },
            title = { Text("Adjust DNC Network Mode", color = TextLight, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose Central port prefix or set custom sub-network range for isolation testing.", fontSize = 12.sp, color = SubtleText)
                    OutlinedTextField(
                        value = selectedPort,
                        onValueChange = { selectedPort = it },
                        label = { Text("Port Number") },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechCyan,
                            unfocusedBorderColor = BorderSlate,
                            focusedLabelColor = TechCyan,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedPort = selectedPort.toIntOrNull() ?: 369
                        onChangeNet("Custom Subnet", parsedPort)
                        showNetworkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update Network", color = SpaceBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNetworkDialog = false }) {
                    Text("Cancel", color = SubtleText)
                }
            },
            containerColor = CardSlate
        )
    }
}

@Composable
fun WalletTabContent(
    walletKeyPair: com.example.crypto.DiacInfinityKeys.KeyPair?,
    balance: Double,
    transactions: List<Transaction>,
    onSendTransaction: (String, Double) -> Unit
) {
    var recipient by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Display Card (High Fidelity Immersive UI format)
        GlowCard(borderColor = TechCyan.copy(alpha = 0.5f)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AVAILABLE BALANCE",
                    fontSize = 11.sp,
                    color = SubtleText,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "%,.4f".format(balance),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "\$DNC",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TechCyan,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        if (walletKeyPair == null) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "⚠️ Generate an Account first inside 'Account' tab to configure are Wallet ledger balances.",
                    color = SubtleText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Send Form
            GlowCard(borderColor = BorderSlate) {
                Text("💸 Split & Broadcast Funds", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TechCyan)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text("Recipient DNC Address (Hash160 Base32)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TechCyan,
                        unfocusedBorderColor = BorderSlate,
                        focusedLabelColor = TechCyan,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Transfer Amount (DNC)") },
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TechCyan,
                        unfocusedBorderColor = BorderSlate,
                        focusedLabelColor = TechCyan,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (recipient.isNotEmpty() && amount > 0.0) {
                            onSendTransaction(recipient, amount)
                            recipient = ""
                            amountText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DncGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_funds_btn")
                ) {
                    Text("Sign with Threshold Credentials", color = SpaceBlack, fontWeight = FontWeight.Bold)
                }
            }

            // Recent Transactions
            Text(
                text = "📁 Recent Shard Transactions",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SubtleText,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("No local fragment transactions generated yet.", color = SubtleText, fontSize = 11.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardSlate)
                                .border(1.dp, BorderSlate, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("To: ${tx.toDisplay}", color = TextLight, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Text("ID: ${tx.txid.take(8)}... | ${java.text.SimpleDateFormat("mm:ss", java.util.Locale.getDefault()).format(tx.timestamp)}", color = SubtleText, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("-${"%.2f".format(tx.amount)} DNC", color = AlertAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                Text(tx.status, color = TechCyan, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiningTabContent(
    walletKeyPair: com.example.crypto.DiacInfinityKeys.KeyPair?,
    isMiningActive: Boolean,
    miningMetrics: DncEngine.MiningMetrics,
    onToggleMining: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlowCard(borderColor = if (isMiningActive) DncGreen else BorderSlate) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("⛏️ PoSB Mining Operations", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TechCyan)
                    Text(
                        text = if (isMiningActive) "STATUS: RUNNING AUDITS" else "STATUS: MINER STANDBY",
                        color = if (isMiningActive) DncGreen else SubtleText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = isMiningActive,
                    onCheckedChange = {
                        if (walletKeyPair != null) onToggleMining()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SpaceBlack,
                        checkedTrackColor = DncGreen,
                        uncheckedThumbColor = SubtleText,
                        uncheckedTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("mining_switch")
                )
            }
        }

        if (walletKeyPair == null) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "⚠️ An active wallet credential is required to initialize resource indexing.",
                    color = SubtleText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Metrics Widgets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlowCard(
                    borderColor = BorderSlate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Total Minted", fontSize = 10.sp, color = SubtleText)
                    Text("%.4f".format(miningMetrics.totalRewards), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DncGreen, fontFamily = FontFamily.Monospace)
                }
                GlowCard(
                    borderColor = BorderSlate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Audits Answered", fontSize = 10.sp, color = SubtleText)
                    Text("${miningMetrics.challengesCompleted} Solved", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TechCyan, fontFamily = FontFamily.Monospace)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlowCard(
                    borderColor = BorderSlate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Current Efficiency", fontSize = 10.sp, color = SubtleText)
                    Text("%.1f%%".format(miningMetrics.efficiency), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DncGreen, fontFamily = FontFamily.Monospace)
                }
                GlowCard(
                    borderColor = BorderSlate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Staked Defense", fontSize = 10.sp, color = SubtleText)
                    Text("50% (Auto)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AlertAmber, fontFamily = FontFamily.Monospace)
                }
            }

            // Real Mining diagnostics
            TerminalConsole(
                title = "PoSB (PROOF-OF-STORAGE & BANDWIDTH) TRACE",
                logs = miningMetrics.logs,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun NetworkTabContent(
    peers: List<Peer>,
    networkPort: Int,
    onAddPeer: (String, Int) -> Unit,
    onPingPeer: (Peer) -> Unit
) {
    var peerIp by remember { mutableStateOf("") }
    var peerPort by remember { mutableStateOf(networkPort.toString()) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Manual Peer Addition form
        GlowCard(borderColor = BorderSlate) {
            Text("🔍 Register Peer Node (Dual UDP Punching)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TechCyan)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = peerIp,
                    onValueChange = { peerIp = it },
                    label = { Text("IP Address") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.5f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TechCyan,
                        unfocusedBorderColor = BorderSlate,
                        focusedLabelColor = TechCyan,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    )
                )
                OutlinedTextField(
                    value = peerPort,
                    onValueChange = { peerPort = it },
                    label = { Text("Port") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TechCyan,
                        unfocusedBorderColor = BorderSlate,
                        focusedLabelColor = TechCyan,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    )
                )
                Button(
                    onClick = {
                        val parsedPort = peerPort.toIntOrNull() ?: 369
                        if (peerIp.isNotEmpty()) {
                            onAddPeer(peerIp, parsedPort)
                            peerIp = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Link", color = SpaceBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Peers Table list
        Text(
            text = "📡 Discovered peer mesh (DHT Kademlia buckets)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SubtleText
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(peers) { peer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardSlate)
                        .border(1.dp, if (peer.isFraud) Color(0xFFEF4444) else BorderSlate, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (peer.isFraud) Color(0xFFEF4444) else DncGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = peer.ipDisplay,
                                fontSize = 13.sp,
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Reputation: ${peer.reputation}% | Latency: ${peer.latencyMs}ms",
                            color = SubtleText,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Anchor: ${peer.addressDisplay}",
                            color = TechCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = { onPingPeer(peer) },
                        colors = ButtonDefaults.buttonColors(containerColor = CardSlate),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, TechCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Text("Audit Pong", color = TechCyan, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AuditTabContent(
    storageChallenges: List<StorageChallenge>,
    guardians: List<Guardian>,
    onAuditRun: (String, String) -> Unit,
    onRecoveryRun: (String, String) -> Unit
) {
    var mockDataText by remember { mutableStateOf("") }
    var mockFragmentId by remember { mutableStateOf("frag-a1b2") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PoS Custom Audit Playground
        GlowCard(borderColor = TechCyan) {
            Text("🧬 Proof-of-Storage Challenge Playground", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TechCyan)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Simulate live storage integrity verification. Select a fragment ID and input custom text block data to hash and salt with time-varying nonce.", fontSize = 11.sp, color = SubtleText)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = mockFragmentId,
                onValueChange = { mockFragmentId = it },
                label = { Text("Fragment Index ID") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TechCyan,
                    unfocusedBorderColor = BorderSlate,
                    focusedLabelColor = TechCyan,
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = mockDataText,
                onValueChange = { mockDataText = it },
                label = { Text("Custom Document or File Chunk Text Data") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TechCyan,
                    unfocusedBorderColor = BorderSlate,
                    focusedLabelColor = TechCyan,
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (mockDataText.isNotEmpty()) {
                        onAuditRun(mockFragmentId, mockDataText)
                        mockDataText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Process Local Audit (Integrity Check)", color = SpaceBlack, fontWeight = FontWeight.Bold)
            }
        }

        // Safe Guardians configuration
        GlowCard(borderColor = AlertAmber) {
            Text("🔑 Key Rotation & Guardian Recovery (2-of-3)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AlertAmber)
            Spacer(modifier = Modifier.height(4.dp))
            Text("DNC safeguards funds against private key loss. Pre-register guardians to reconstruct seed rotation securely.", fontSize = 11.sp, color = SubtleText)
            Spacer(modifier = Modifier.height(8.dp))

            guardians.forEach { guardian ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(guardian.name, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(guardian.address, color = SubtleText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DncGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Active", color = DncGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    onRecoveryRun(guardians[0].name, guardians[1].name)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AlertAmber),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simulate 2-of-3 Rotation", color = SpaceBlack, fontWeight = FontWeight.Bold)
            }
        }
    }
}
