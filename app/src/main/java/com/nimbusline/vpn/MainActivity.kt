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
 @Composable private fun Field(label:String,value:String,onValue:(String)->Unit)=OutlinedTextField(value,onValue,Modifier.fillMaxWidth(),label={Text(label)},singleLine=true)
 @Composable private fun Ui(){
  var list by remember{mutableStateOf(repo.all())};var raw by remember{mutableStateOf("")};var err by remember{mutableStateOf("")};var editing by remember{mutableStateOf<VpnProfile?>(null)}
  MaterialTheme{Scaffold(topBar={TopAppBar(title={Text("NimbusLine VPN")})}){pad->
   LazyColumn(Modifier.padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    item{Text("Standalone Xray VPN",style=MaterialTheme.typography.headlineSmall);Text("Only VLESS + WebSocket + TLS and VLESS + XHTTP + TLS");OutlinedTextField(raw,{raw=it},Modifier.fillMaxWidth().heightIn(min=140.dp),label={Text("Paste VLESS URI or Xray JSON")});Row{Button(onClick={runCatching{repo.save(ConfigParser.parse(raw));list=repo.all();raw="";err=""}.onFailure{err=it.message.orEmpty()}){Text("Import")};Spacer(Modifier.width(8.dp));OutlinedButton(onClick={raw="";err=""}){Text("Clear")}};if(err.isNotBlank())Text(err,color=MaterialTheme.colorScheme.error)}
    editing?.let{p->
     item{Text("Profile fields",style=MaterialTheme.typography.titleLarge)}
     item{var v by remember(p.id){mutableStateOf(p.remark)};Field("Remark",v){v=it;editing=p.copy(remark=it)}}
     item{var v by remember(p.id){mutableStateOf(p.address)};Field("Address",v){v=it;editing=p.copy(address=it)}}
     item{var v by remember(p.id){mutableStateOf(p.port.toString())};Field("Port",v){v=it;editing=p.copy(port=it.toIntOrNull()?:p.port)}}
     item{var v by remember(p.id){mutableStateOf(p.uuid)};Field("UUID",v){v=it;editing=p.copy(uuid=it)}}
     item{var v by remember(p.id){mutableStateOf(p.encryption)};Field("Encryption",v){v=it;editing=p.copy(encryption=it)}}
     item{var v by remember(p.id){mutableStateOf(p.flow)};Field("Flow",v){v=it;editing=p.copy(flow=it)}}
     item{Text("Network: "+p.transport+" • Security: TLS")}
     item{var v by remember(p.id){mutableStateOf(p.host)};Field("Host",v){v=it;editing=p.copy(host=it)}}
     item{var v by remember(p.id){mutableStateOf(p.path)};Field("Path",v){v=it;editing=p.copy(path=it)}}
     if(p.transport==Transport.XHTTP){item{var v by remember(p.id){mutableStateOf(p.xhttpMode)};Field("XHTTP mode",v){v=it;editing=p.copy(xhttpMode=it)}};item{var v by remember(p.id){mutableStateOf(p.xhttpExtra)};Field("XHTTP extra JSON",v){v=it;editing=p.copy(xhttpExtra=it)}}}
     item{var v by remember(p.id){mutableStateOf(p.tls.serverName)};Field("TLS SNI",v){v=it;editing=p.copy(tls=p.tls.copy(serverName=it))}}
     item{var v by remember(p.id){mutableStateOf(p.tls.fingerprint)};Field("TLS fingerprint",v){v=it;editing=p.copy(tls=p.tls.copy(fingerprint=it))}}
     item{var v by remember(p.id){mutableStateOf(p.tls.alpn.joinToString(","))};Field("TLS ALPN",v){v=it;editing=p.copy(tls=p.tls.copy(alpn=it.split(",")))}}
     item{var v by remember(p.id){mutableStateOf(p.tls.cipherSuites)};Field("TLS cipher suites",v){v=it;editing=p.copy(tls=p.tls.copy(cipherSuites=it))}}
     item{var v by remember(p.id){mutableStateOf(p.finalMask)};Field("FinalMask JSON",v){v=it;editing=p.copy(finalMask=it)}}
     item{Button(onClick={editing?.let{repo.save(it);list=repo.all()};editing=null}){Text("Save profile")}}
    }
    items(list,key={it.id}){p->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(p.remark,style=MaterialTheme.typography.titleMedium);Text(p.transport.toString()+" • "+p.address+":"+p.port);Text("SNI: "+p.tls.serverName);Row{Button(onClick={val q=VpnService.prepare(this@MainActivity);if(q!=null)startActivityForResult(q,100)else connect(p.id)}){Text("Connect")};Spacer(Modifier.width(6.dp));OutlinedButton(onClick={editing=p}){Text("Edit")};Spacer(Modifier.width(6.dp));OutlinedButton(onClick={repo.delete(p.id);list=repo.all()}){Text("Delete")}}}}}
   }
  }}
 }
 private fun connect(id:String){startForegroundService(Intent(this,NimbusVpnService::class.java).apply{action=NimbusVpnService.ACTION_START;putExtra(NimbusVpnService.EXTRA_PROFILE,id)})}
}
