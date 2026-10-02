package com.nimbusline.vpn.config
object ConfigValidator{
 fun validate(p:VpnProfile){
  require(p.uuid.isNotBlank()){"UUID is required"}
  require(p.address.isNotBlank()){"Address is required"}
  require(p.port in 1..65535){"Invalid port"}
  require(p.transport==Transport.WS||p.transport==Transport.XHTTP){"Only VLESS WS/XHTTP TLS is supported"}
  require(p.tls.serverName.isNotBlank()){"TLS SNI is required"}
 }
}
