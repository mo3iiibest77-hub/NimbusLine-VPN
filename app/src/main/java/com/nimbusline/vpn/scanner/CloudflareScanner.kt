package com.nimbusline.vpn.scanner
import com.nimbusline.vpn.config.VpnProfile
import kotlinx.coroutines.*
import java.net.*
import javax.net.ssl.*
data class ScanResult(val ip:String,val latencyMs:Long,val ok:Boolean,val error:String?=null)
class CloudflareScanner{
 suspend fun best(p:VpnProfile,ranges:List<String>,perCidr:Int=2):ScanResult?=coroutineScope{ranges.flatMap{CidrSampler.sample(it,perCidr)}.distinct().map{ip->async(Dispatchers.IO){probe(p,ip)}}.awaitAll().filter{it.ok}.minByOrNull{it.latencyMs}}
 private fun probe(p:VpnProfile,ip:String):ScanResult{val t=System.nanoTime();return try{Socket().use{raw->raw.connect(InetSocketAddress(ip,p.port),3500);(SSLSocketFactory.getDefault().createSocket(raw,ip,p.port,true)as SSLSocket).use{ssl->ssl.soTimeout=3500;ssl.sslParameters=ssl.sslParameters.apply{serverNames=listOf(SNIHostName(p.tls.serverName))};ssl.startHandshake();val h=p.host.ifBlank{p.tls.serverName};val req=if(p.transport.name=="WS")"GET "+p.path+" HTTP/1.1\r\nHost: "+h+"\r\nConnection: Upgrade\r\nUpgrade: websocket\r\nSec-WebSocket-Version: 13\r\nSec-WebSocket-Key: dW5pcXVlLW5pbWJ1c2xpbmU=\r\n\r\n" else "HEAD "+p.path+" HTTP/1.1\r\nHost: "+h+"\r\nConnection: close\r\n\r\n";ssl.outputStream.write(req.toByteArray());ssl.outputStream.flush();require(ssl.inputStream.read()>=0)}};ScanResult(ip,(System.nanoTime()-t)/1000000,true)}catch(e:Exception){ScanResult(ip,(System.nanoTime()-t)/1000000,false,e.message)}}
}
