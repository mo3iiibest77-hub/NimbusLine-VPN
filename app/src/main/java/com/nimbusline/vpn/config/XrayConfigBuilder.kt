package com.nimbusline.vpn.config
import org.json.JSONArray
import org.json.JSONObject
object XrayConfigBuilder{
 fun build(p:VpnProfile):String{
  ConfigValidator.validate(p)
  val user=JSONObject().put("id",p.uuid).put("encryption",p.encryption); if(p.flow.isNotBlank())user.put("flow",p.flow)
  val stream=JSONObject().put("network",if(p.transport==Transport.XHTTP)"xhttp" else "ws").put("security","tls").put("tlsSettings",tls(p))
  if(p.finalMask.isNotBlank())stream.put("finalmask",JSONObject(p.finalMask))
  if(p.transport==Transport.WS)stream.put("wsSettings",JSONObject().put("path",p.path).put("headers",JSONObject().apply{if(p.host.isNotBlank())put("Host",p.host)}))
  else stream.put("xhttpSettings",JSONObject().put("host",p.host).put("path",p.path).put("mode",p.xhttpMode).apply{if(p.xhttpExtra.isNotBlank())put("extra",JSONObject(p.xhttpExtra))})
  val out=JSONObject().put("protocol","vless").put("settings",JSONObject().put("vnext",JSONArray().put(JSONObject().put("address",p.address).put("port",p.port).put("users",JSONArray().put(user))))).put("streamSettings",stream)
  return JSONObject().put("log",JSONObject().put("loglevel","warning")).put("inbounds",JSONArray()).put("outbounds",JSONArray().put(out).put(JSONObject().put("protocol","freedom").put("tag","direct"))).toString()
 }
 private fun tls(p:VpnProfile)=JSONObject().put("serverName",p.tls.serverName).put("allowInsecure",p.tls.allowInsecure).put("fingerprint",p.tls.fingerprint).put("alpn",JSONArray(p.tls.alpn)).apply{if(p.tls.cipherSuites.isNotBlank())put("cipherSuites",p.tls.cipherSuites)}
}
