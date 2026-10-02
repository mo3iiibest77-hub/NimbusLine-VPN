package com.nimbusline.vpn.config
import kotlinx.serialization.Serializable
@Serializable enum class Transport{WS,XHTTP}
@Serializable data class TlsSettings(val serverName:String,val allowInsecure:Boolean=false,val fingerprint:String="unsafe",val alpn:List<String> = listOf("http/1.1","h2"),val cipherSuites:String="")
@Serializable data class VpnProfile(val id:String,val remark:String,val address:String,val port:Int,val uuid:String,val encryption:String="none",val flow:String="",val transport:Transport,val host:String="",val path:String="/",val xhttpMode:String="auto",val xhttpExtra:String="",val finalMask:String="",val browserDialer:Boolean=false,val tls:TlsSettings,val originalAddress:String=address)
