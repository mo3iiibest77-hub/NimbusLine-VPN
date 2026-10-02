package com.nimbusline.vpn.config
import android.content.Context
import kotlinx.serialization.json.Json
class ProfileRepository(c: Context) {
    private val p = c.getSharedPreferences("profiles", Context.MODE_PRIVATE)
    private val j = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun all(): List<VpnProfile> = p.all.keys.sorted().mapNotNull {
        p.getString(it, null)?.let { s -> runCatching { j.decodeFromString<VpnProfile>(s) }.getOrNull() }
    }
    fun get(id: String): VpnProfile? = p.getString(id, null)?.let { runCatching { j.decodeFromString<VpnProfile>(it) }.getOrNull() }
    fun save(v: VpnProfile): VpnProfile {
        p.edit().putString(v.id, j.encodeToString(VpnProfile.serializer(), v)).apply()
        return v
    }
    fun delete(id: String) { p.edit().remove(id).apply() }
}
