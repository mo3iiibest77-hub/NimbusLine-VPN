package com.nimbusline.vpn.config
import android.net.Uri
import org.json.JSONObject
import java.net.URLDecoder
import java.util.UUID

object ConfigParser {
 fun parse(input:String):VpnProfile {
  val s=input.trim()
  return if(s.startsWith("{")) parseJson(s) else parseUri(s)
 }
 private fun parseUri(s:String):VpnProfile {
  val u=Uri.parse(s); require(u.scheme.equals("vless",true)){"Only VLESS is supported"}
  val uuid=u.userInfo?.substringBefore(":")?:error("Missing UUID"); val address=u.host?:error("Missing address")
  val port=u.port.takeIf{it>0}?:error("Missing port"); val q=u.queryParameterNames.associateWith{u.getQueryParameter(it).orEmpty()}
  require(q["security"].orEmpty().lowercase()=="tls"){"Only TLS is supported"}
  val t=when(q["type"].orEmpty().lowercase()){"ws","websocket"->Transport.WS;"xhttp","splithttp"->Transport.XHTTP;else->error("Only WS and XHTTP are supported")}
  val dec={v:String->URLDecoder.decode(v,"UTF-8")}; val host=q["host"].orEmpty(); val sni=q["sni"].orEmpty().ifBlank{host.ifBlank{address}}
  return VpnProfile(UUID.randomUUID().toString(),u.fragment?.let(dec).orEmpty().ifBlank{address},address,port,uuid,q["encryption"].orEmpty().ifBlank{"none"},q["flow"].orEmpty(),t,host,dec(q["path"].orEmpty().ifBlank{"/"}),q["mode"].orEmpty().ifBlank{"auto"},q["extra"].orEmpty().let{if(it.isBlank())"" else dec(it)},q["fm"].orEmpty(),q["browserDialer"]=="1",TlsSettings(sni,q["allowInsecure"]=="1",q["fp"].orEmpty().ifBlank{"unsafe"},q["alpn"].orEmpty().ifBlank{"http/1.1,h2"}.split(","),q["ciphers"].orEmpty()),address)
 }
 private fun parseJson(s:String):VpnProfile {
  val root=JSONObject(s); val outs=root.optJSONArray("outbounds")?:error("No outbounds")
  var o:JSONObject?=null
  for(i in 0 until outs.length()){val x=outs.optJSONObject(i);if(x?.optString("protocol")=="vless"){o=x;break}}
  o?:error("No VLESS outbound")
  val vnext=o!!.optJSONObject("settings")?.optJSONArray("vnext")?.optJSONObject(0)?:error("Invalid VLESS settings")
  val user=vnext.optJSONArray("users")?.optJSONObject(0)?:error("Missing VLESS user")
  val stream=o!!.optJSONObject("streamSettings")?:error("Missing streamSettings")
  require(stream.optString("security")=="tls"){"Only TLS is supported"}
  val network=stream.optString("network").lowercase()
  val transport=when(network){"ws"->Transport.WS;"xhttp"->Transport.XHTTP;else->error("Only WS and XHTTP are supported")}
  val tls=stream.optJSONObject("tlsSettings")?:error("Missing tlsSettings")
  val ws=stream.optJSONObject("wsSettings"); val xh=stream.optJSONObject("xhttpSettings")
  val host=ws?.optString("headers")?.let{runCatching{JSONObject(it).optString("Host")}.getOrNull()}.orEmpty().ifBlank{xh?.optString("host").orEmpty()}
  val path=ws?.optString("path").orEmpty().ifBlank{xh?.optString("path").orEmpty().ifBlank{"/"}}
  val fm=stream.optJSONObject("finalmask")?.toString().orEmpty()
  val extra=xh?.optJSONObject("extra")?.toString().orEmpty()
  return VpnProfile(UUID.randomUUID().toString(),root.optString("remarks").ifBlank{"Nimbus profile"},vnext.optString("address"),vnext.optInt("port"),user.optString("id"),user.optString("encryption","none"),user.optString("flow"),transport,host,path,xh?.optString("mode","auto")?:"auto",extra,fm,false,TlsSettings(tls.optString("serverName"),tls.optBoolean("allowInsecure",false),tls.optString("fingerprint","unsafe"),(0 until (tls.optJSONArray("alpn")?.length()?:0)).map{tls.getJSONArray("alpn").getString(it)}.ifEmpty{listOf("http/1.1","h2")},tls.optString("cipherSuites")),vnext.optString("address"))
 }
}
