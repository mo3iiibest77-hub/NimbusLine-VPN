package com.nimbusline.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nimbusline.vpn.config.*
import com.nimbusline.vpn.vpn.*

class MainActivity : ComponentActivity() {
    private lateinit var repo: ProfileRepository
    private lateinit var apps: AppTunnelStore
    private var pendingProfile: String? = null
    override fun onCreate(b: Bundle?) {
        super.onCreate(b); repo = ProfileRepository(this); apps = AppTunnelStore(this); setContent { Ui() }
    }
    @Composable private fun Field(label: String, value: String, onValue: (String) -> Unit) =
        OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true)
    @OptIn(ExperimentalMaterial3Api::class)\n    @Composable private fun Ui() {
        var list by remember { mutableStateOf(repo.all()) }
        var raw by remember { mutableStateOf("") }
        var subscription by remember { mutableStateOf("") }
        var err by remember { mutableStateOf("") }
        var editing by remember { mutableStateOf<VpnProfile?>(null) }
        var perApp by remember { mutableStateOf(apps.enabled()) }
        var selected by remember { mutableStateOf(apps.packages()) }
        var installed by remember { mutableStateOf(AppInventory.launchable(this@MainActivity)) }
        MaterialTheme {
            Scaffold(topBar = { TopAppBar(title = { Text("NimbusLine VPN") }) }) { pad ->
                LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text("NimbusLine", style = MaterialTheme.typography.headlineSmall); Text("Native Xray client • VLESS WS/XHTTP TLS") }
                    item {
                        OutlinedTextField(subscription, { subscription = it }, Modifier.fillMaxWidth(), label = { Text("Subscription URL (HTTPS)") }, singleLine = true)
                        Button(onClick = {
                            Thread {
                                runCatching { SubscriptionParser.fetch(subscription) }.onSuccess { ps ->
                                    runOnUiThread { ps.forEach { repo.save(it) }; list = repo.all(); err = "Imported " + ps.size + " profiles" }
                                }.onFailure { e -> runOnUiThread { err = e.message.orEmpty() } }
                            }.start()
                        }) { Text("Update subscription") }
                    }
                    item {
                        OutlinedTextField(raw, { raw = it }, Modifier.fillMaxWidth().heightIn(min = 130.dp), label = { Text("VLESS URI or Xray JSON") })
                        Row { Button(onClick = {
                            runCatching { repo.save(ConfigParser.parse(raw)); list = repo.all(); raw = ""; err = "" }.onFailure { err = it.message.orEmpty() }
                        }) { Text("Import") }; Spacer(Modifier.width(8.dp)); OutlinedButton(onClick = { raw = "" }) { Text("Clear") } }
                    }
                    item {
                        Card { Column(Modifier.padding(14.dp)) {
                            Text("Per-app VPN", style = MaterialTheme.typography.titleLarge)
                            Text("When enabled, only checked applications are tunneled.")
                            Row {
                                Checkbox(perApp, { perApp = it; apps.setEnabled(it); if (it) installed = AppInventory.launchable(this@MainActivity) })
                                Text("Enable", Modifier.padding(top = 12.dp))
                            }
                            if (perApp) {
                                Row {
                                    OutlinedButton(onClick = { selected = installed.map { it.packageName }.toSet(); apps.setPackages(selected) }) { Text("Select all") }
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedButton(onClick = { selected = emptySet(); apps.setPackages(emptySet()) }) { Text("Clear all") }
                                }
                                installed.forEach { a ->
                                    Row(Modifier.fillMaxWidth()) {
                                        Checkbox(a.packageName in selected, { checked ->
                                            selected = selected.toMutableSet().apply { if (checked) add(a.packageName) else remove(a.packageName) }; apps.setPackages(selected)
                                        })
                                        Text(a.label, Modifier.padding(top = 12.dp))
                                    }
                                }
                            }
                        } }
                    }
                    if (err.isNotBlank()) item { Text(err, color = MaterialTheme.colorScheme.primary) }
                    editing?.let { p ->
                        item { Text("Profile editor", style = MaterialTheme.typography.titleLarge) }
                        item { var v by remember(p.id) { mutableStateOf(p.remark) }; Field("Remark", v) { v = it; editing = p.copy(remark = it) } }
                        item { var v by remember(p.id) { mutableStateOf(p.address) }; Field("Address", v) { v = it; editing = p.copy(address = it) } }
                        item { var v by remember(p.id) { mutableStateOf(p.port.toString()) }; Field("Port", v) { v = it; editing = p.copy(port = it.toIntOrNull() ?: p.port) } }
                        item { var v by remember(p.id) { mutableStateOf(p.uuid) }; Field("UUID", v) { v = it; editing = p.copy(uuid = it) } }
                        item { var v by remember(p.id) { mutableStateOf(p.encryption) }; Field("Encryption", v) { v = it; editing = p.copy(encryption = it) } }
                        item { var v by remember(p.id) { mutableStateOf(p.flow) }; Field("Flow", v) { v = it; editing = p.copy(flow = it) } }
                        item { Text("Network: " + p.transport + " • Security: TLS") }
                        item { var v by remember(p.id) { mutableStateOf(p.host) }; Field("Host", v) { v = it; editing = p.copy(host = it) } }
                        item { var v by remember(p.id) { mutableStateOf(p.path) }; Field("Path", v) { v = it; editing = p.copy(path = it) } }
                        if (p.transport == Transport.XHTTP) {
                            item { var v by remember(p.id) { mutableStateOf(p.xhttpMode) }; Field("XHTTP mode", v) { v = it; editing = p.copy(xhttpMode = it) } }
                            item { var v by remember(p.id) { mutableStateOf(p.xhttpExtra) }; Field("XHTTP extra JSON", v) { v = it; editing = p.copy(xhttpExtra = it) } }
                        }
                        item { var v by remember(p.id) { mutableStateOf(p.tls.serverName) }; Field("TLS SNI", v) { v = it; editing = p.copy(tls = p.tls.copy(serverName = it)) } }
                        item { var v by remember(p.id) { mutableStateOf(p.tls.fingerprint) }; Field("TLS fingerprint", v) { v = it; editing = p.copy(tls = p.tls.copy(fingerprint = it)) } }
                        item { var v by remember(p.id) { mutableStateOf(p.tls.allowInsecure.toString()) }; Field("TLS allowInsecure", v) { v = it; editing = p.copy(tls = p.tls.copy(allowInsecure = it.equals("true", true))) } }
                        item { var v by remember(p.id) { mutableStateOf(p.tls.alpn.joinToString(",")) }; Field("TLS ALPN", v) { v = it; editing = p.copy(tls = p.tls.copy(alpn = it.split(",").map(String::trim).filter(String::isNotEmpty))) } }
                        item { var v by remember(p.id) { mutableStateOf(p.tls.cipherSuites) }; Field("TLS cipher suites", v) { v = it; editing = p.copy(tls = p.tls.copy(cipherSuites = it)) } }
                        item { var v by remember(p.id) { mutableStateOf(p.finalMask) }; Field("FinalMask JSON", v) { v = it; editing = p.copy(finalMask = it) } }
                        item { var v by remember(p.id) { mutableStateOf(p.browserDialer.toString()) }; Field("Browser Dialer", v) { v = it; editing = p.copy(browserDialer = it.equals("true", true)) } }
                        item { Button(onClick = { editing?.let { repo.save(it) }; list = repo.all(); editing = null }) { Text("Save profile") } }
                    }
                    items(list, key = { it.id }) { p ->
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                            Text(p.remark, style = MaterialTheme.typography.titleMedium)
                            Text(p.transport.toString() + " • " + p.address + ":" + p.port)
                            Text("SNI: " + p.tls.serverName)
                            Row {
                                Button(onClick = { pendingProfile = p.id; val q = VpnService.prepare(this@MainActivity); if (q != null) startActivityForResult(q, 100) else connect(p.id) }) { Text("Connect") }
                                Spacer(Modifier.width(6.dp)); OutlinedButton(onClick = { editing = p }) { Text("Edit") }
                                Spacer(Modifier.width(6.dp)); OutlinedButton(onClick = { repo.delete(p.id); list = repo.all() }) { Text("Delete") }
                            }
                        } }
                    }
                    item { OutlinedButton(onClick = { startService(Intent(this@MainActivity, NimbusVpnService::class.java).setAction(NimbusVpnService.ACTION_STOP)) }) { Text("Disconnect") } }
                }
            }
        }
    }
    private fun connect(id: String) {
        startForegroundService(Intent(this, NimbusVpnService::class.java).apply { action = NimbusVpnService.ACTION_START; putExtra(NimbusVpnService.EXTRA_PROFILE, id) })
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) pendingProfile?.let(::connect)
    }
}
