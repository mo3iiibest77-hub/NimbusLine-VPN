package com.nimbusline.vpn.scanner
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
object CloudflareRanges {
    suspend fun fetch(): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            URL("https://www.cloudflare.com/ips-v4").openStream().bufferedReader().use {
                it.readLines().filter(String::isNotBlank)
            }
        }.getOrDefault(emptyList())
    }
}
