package com.nimbusline.vpn.scanner

import com.nimbusline.vpn.config.VpnProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class ScanResult(val ip: String, val latencyMs: Long, val ok: Boolean, val error: String? = null)

class CloudflareScanner {
    suspend fun best(p: VpnProfile, ranges: List<String>, perCidr: Int = 2): ScanResult? =
        coroutineScope {
            ranges.flatMap { CidrSampler.sample(it, perCidr) }.distinct()
                .map { ip -> async(Dispatchers.IO) { probe(p, ip) } }
                .awaitAll().filter { it.ok }.minByOrNull { it.latencyMs }
        }

    private fun probe(p: VpnProfile, ip: String): ScanResult {
        val t = System.nanoTime()
        return try {
            Socket().use { raw ->
                raw.connect(InetSocketAddress(ip, p.port), 3500)
                val ssl = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                    .createSocket(raw, p.tls.serverName, p.port, true) as SSLSocket
                ssl.use {
                    it.soTimeout = 3500
                    it.sslParameters = it.sslParameters.apply {
                        serverNames = listOf(SNIHostName(p.tls.serverName))
                    }
                    it.startHandshake()
                    val host = p.host.ifBlank { p.tls.serverName }
                    val request = if (p.transport.name == "WS")
                        "GET " + p.path + " HTTP/1.1\r\nHost: " + host +
                            "\r\nConnection: Upgrade\r\nUpgrade: websocket\r\n" +
                            "Sec-WebSocket-Version: 13\r\nSec-WebSocket-Key: dW5pcXVlLW5pbWJ1c2xpbmU=\r\n\r\n"
                    else
                        "HEAD " + p.path + " HTTP/1.1\r\nHost: " + host + "\r\nConnection: close\r\n\r\n"
                    it.outputStream.write(request.toByteArray())
                    it.outputStream.flush()
                    require(it.inputStream.read() >= 0)
                }
            }
            ScanResult(ip, (System.nanoTime() - t) / 1_000_000, true)
        } catch (e: Exception) {
            ScanResult(ip, (System.nanoTime() - t) / 1_000_000, false, e.message)
        }
    }
}
