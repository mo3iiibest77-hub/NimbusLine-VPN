package com.v2ray.ang.nimbus

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.handler.MmkvManager
import java.net.Inet4Address
import java.net.InetAddress

/**
 * NimbusLine's connection policy.
 *
 * Keep all owner-supplied hardcoded transport values in this file.
 */
object NimbusPolicy {
    const val SCAN_ISP_KEY = "nimbus-default-network"

    // Owner values currently inherited from the scanner's production validation.
    // Replace these constants when the final NimbusLine values are supplied.
    const val FINGERPRINT = "unsafe"
    const val FINAL_MASK = """{"tcp":[{"type":"fragment","settings":{"packets":"tlshello","lengths":["5","94","1"],"delays":["0"],"maxSplit":"0"}},{"type":"fragment","settings":{"packets":"1-1","lengths":["109","1"],"delays":["1"],"maxSplit":"355"}}]}"""
    const val CIPHER_SUITES =
        "TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256:TLS_AES_128_GCM_SHA256:" +
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384:TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384:" +
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256:" +
        "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256:TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256:" +
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA:TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA:" +
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256:TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"

    private val CLOUDFLARE_CIDRS = listOf(
        "173.245.48.0/20", "103.21.244.0/22", "103.22.200.0/22",
        "103.31.4.0/22", "141.101.64.0/18", "108.162.192.0/18",
        "190.93.240.0/20", "188.114.96.0/20", "197.234.240.0/22",
        "198.41.128.0/17", "162.158.0.0/15", "104.16.0.0/13",
        "104.24.0.0/14", "172.64.0.0/13", "131.0.72.0/22"
    )

    fun isSupported(profile: ProfileItem): Boolean {
        val network = profile.network?.trim()?.lowercase() ?: return false
        val security = profile.security?.trim()?.lowercase() ?: return false
        if (security != "tls") return false
        if (network != "ws" && network != "xhttp") return false

        val port = profile.serverPort?.toIntOrNull() ?: return false
        return port == 443 || port == 53
    }

    /**
     * Apply Nimbus values and preserve the hostname needed after replacing server with a
     * Cloudflare edge IP. Xray TLS/SNI and HTTP Host are distinct from the TCP destination.
     */
    fun prepare(profile: ProfileItem): ProfileItem {
        if (!isSupported(profile)) return profile

        val originalServer = profile.server?.trim().orEmpty()
        val hostname = when {
            profile.sni?.isNullOrBlank() == false -> profile.sni!!.trim()
            !isIpLiteral(originalServer) && originalServer.isNotBlank() -> originalServer
            else -> null
        }

        if (profile.sni.isNullOrBlank() && !hostname.isNullOrBlank()) {
            profile.sni = hostname
        }
        if (profile.host.isNullOrBlank() && !hostname.isNullOrBlank()) {
            profile.host = hostname
        }

        profile.fingerPrint = FINGERPRINT
        profile.finalMask = FINAL_MASK
        profile.cipherSuites = CIPHER_SUITES
        return profile
    }

    fun isCloudflareAddress(address: String): Boolean {
        val ip = runCatching { InetAddress.getByName(address) }.getOrNull() ?: return false
        return ip is Inet4Address && CLOUDFLARE_CIDRS.any { cidrContains(it, ip.address) }
    }

    fun isLikelyCloudflareHostname(host: String): Boolean {
        if (host.isBlank() || isIpLiteral(host)) return isCloudflareAddress(host)
        return runCatching {
            InetAddress.getAllByName(host).any { addr ->
                addr is Inet4Address && CLOUDFLARE_CIDRS.any { cidrContains(it, addr.address) }
            }
        }.getOrDefault(false)
    }

    /**
     * Keep only the Nimbus-supported profiles in a subscription.
     * DNS resolution is deliberately not required here: a subscription can be imported while
     * offline; scanner validation is the authoritative connectivity test.
     */
    fun filterSubscription(subId: String): Int {
        val guids = MmkvManager.decodeServerList(subId)
        if (guids.isEmpty()) return 0

        val unsupported = mutableListOf<String>()
        var kept = 0

        guids.forEach { guid ->
            val profile = MmkvManager.decodeServerConfig(guid)
            if (profile == null || !isSupported(profile)) {
                unsupported += guid
            } else {
                prepare(profile)
                MmkvManager.encodeServerConfig(guid, profile)
                kept++
            }
        }

        if (unsupported.isNotEmpty()) {
            MmkvManager.removeServers(unsupported, subId)
        }
        return kept
    }

    fun filterAllSubscriptions(): Int =
        MmkvManager.decodeSubscriptions().sumOf { filterSubscription(it.guid) }

    private fun isIpLiteral(value: String): Boolean =
        value.matches(Regex("^\\d{1,3}(?:\\.\\d{1,3}){3}$")) || value.contains(':')

    private fun cidrContains(cidr: String, address: ByteArray): Boolean {
        val parts = cidr.split("/")
        if (parts.size != 2 || address.size != 4) return false
        val base = parts[0].split(".").mapNotNull { it.toIntOrNull() }
        val prefix = parts[1].toIntOrNull() ?: return false
        if (base.size != 4 || prefix !in 0..32) return false
        var baseLong = 0L
        var ipLong = 0L
        repeat(4) {
            baseLong = (baseLong shl 8) or base[it].toLong()
            ipLong = (ipLong shl 8) or (address[it].toInt() and 0xff).toLong()
        }
        val mask = if (prefix == 0) 0L else (-1L shl (32 - prefix)) and 0xffffffffL
        return (baseLong and mask) == (ipLong and mask)
    }
}
