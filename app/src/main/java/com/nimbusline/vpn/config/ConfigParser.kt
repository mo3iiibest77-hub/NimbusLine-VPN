package com.nimbusline.vpn.config
import android.net.Uri
import java.net.URLDecoder
import java.util.UUID
object ConfigParser{
 fun parse(input:String):VpnProfile{
  val u=Uri.parse(input.trim()); require(u.scheme.equals("vless",true)){"Only VLESS is supported"}
  val uuid=u.userInfo?.substringBefore(":")?:error("Missing UUID"); val address=u.host?:error("Missing address")
  val port=u.port.takeIf{it>0}?:error("Missing port")
  val q=u.queryParameterNames.associateWith{u.getQueryParameter(it).orEmpty()}
  require(q["security"].orEmpty().lowercase()=="tls"){"Only TLS is supported"}
  val t=when(q["type"].orEmpty().lowercase()){"ws","websocket"->Transport.WS;"xhttp","splithttp"->Transport.XHTTP;else->error("Only WS and XHTTP are supported")}
  val dec={s:String->URLDecoder.decode(s,"UTF-8")}; val host=q["host"].orEmpty()
  val sni=q["sni"].orEmpty().ifBlank{host.ifBlank{address}}
  return VpnProfile(UUID.randomUUID().toString(),u.fragment?.let(dec).orEmpty().ifBlank{address},address,port,uuid,
   q["encryption"].orEmpty().ifBlank{"none"},q["flow"].orEmpty(),t,host,dec(q["path"].orEmpty().ifBlank{"/"}),
   q["mode"].orEmpty().ifBlank{"auto"},q["extra"].orEmpty().let{if(it.isBlank())"" else dec(it)},q["fm"].orEmpty(),q["browserDialer"]=="1",
   TlsSettings(sni,q["allowInsecure"]=="1",q["fp"].orEmpty().ifBlank{"unsafe"},q["alpn"].orEmpty().ifBlank{"http/1.1,h2"}.split(","),q["ciphers"].orEmpty()),address)
 }
}
