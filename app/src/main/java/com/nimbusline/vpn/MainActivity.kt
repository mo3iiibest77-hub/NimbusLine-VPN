package com.nimbusline.vpn
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nimbusline.vpn.config.*
import com.nimbusline.vpn.vpn.NimbusVpnService
class MainActivity:ComponentActivity(){
 private lateinit var repo:ProfileRepository
 override fun onCreate(b:Bundle?){super.onCreate(b);repo=ProfileRepository(this);setContent{Ui()}}
 @Composable fun Ui(){var list by remember{mutableStateOf(repo.all())};var raw by remember{mutableStateOf("")};var error by remember{mutableStateOf("")};MaterialTheme{Scaffold(topBar={TopAppBar(title={Text("NimbusLine VPN")})}){pad->LazyColumn(Modifier.padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("VLESS + WS/XHTTP + TLS",style=MaterialTheme.typography.titleMedium);OutlinedTextField(raw,{raw=it},Modifier.fillMaxWidth().heightIn(min=140.dp),label={Text("Paste VLESS URI")});Button(onClick={runCatching{repo.save(ConfigParser.parse(raw));list=repo.all();raw="";error=""}.onFailure{error=it.message.orEmpty()}){Text("Import")};if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)};items(list,key={it.id}){p->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(p.remark,style=MaterialTheme.typography.titleMedium);Text(p.transport.toString()+" • "+p.address+":"+p.port);Text("SNI: "+p.tls.serverName);Row{Button(onClick={val q=VpnService.prepare(this@MainActivity);if(q!=null)startActivityForResult(q,100)else connect(p.id)}){Text("Connect")};Spacer(Modifier.width(8.dp));OutlinedButton(onClick={repo.delete(p.id);list=repo.all()}){Text("Delete")}}}}}}}}}
 private fun connect(id:String){startForegroundService(Intent(this,NimbusVpnService::class.java).apply{action=NimbusVpnService.ACTION_START;putExtra(NimbusVpnService.EXTRA_PROFILE,id)})}
}
