package com.nimbusline.vpn.config

import android.util.Base64
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object SubscriptionParser {
    fun fetch(url: String): List<VpnProfile> {
        require(url.startsWith("https://", true)) { "Subscription URL must use HTTPS" }
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.requestMethod = "GET"
        c.setRequestProperty("User-Agent", "NimbusLine-VPN/1.0")
        val body = c.inputStream.bufferedReader().use { it.readText() }
        c.disconnect()
        return parse(body)
    }
    fun parse(body: String): List<VpnProfile> {
        val tokens = if (body.trim().startsWith("[")) {
            val a = JSONArray(body.trim())
            (0 until a.length()).map { a.getString(it) }
        } else body.trim().lines().flatMap { it.trim().split("\\s+".toRegex()) }
        val decoded = tokens.flatMap { token ->
            val t = token.trim()
            if (t.startsWith("vless://", true) || t.startsWith("{")) listOf(t)
            else runCatching {
                String(Base64.decode(t.replace("\\s".toRegex(), ""), Base64.DEFAULT), Charsets.UTF_8)
            }.getOrNull()?.lines()?.filter { it.trim().isNotEmpty() }.orEmpty()
        }
        return decoded.mapNotNull { s -> runCatching { ConfigParser.parse(s.trim()) }.getOrNull() }
            .filter { it.transport == Transport.WS || it.transport == Transport.XHTTP }
            .map { it.copy(id = UUID.randomUUID().toString()) }
    }
}
