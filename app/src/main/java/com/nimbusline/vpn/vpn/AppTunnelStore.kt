package com.nimbusline.vpn.vpn
import android.content.Context
class AppTunnelStore(context: Context) {
    private val p = context.getSharedPreferences("app_tunnel", Context.MODE_PRIVATE)
    fun enabled() = p.getBoolean("enabled", false)
    fun setEnabled(v: Boolean) = p.edit().putBoolean("enabled", v).apply()
    fun packages() = p.getStringSet("packages", emptySet()) ?: emptySet()
    fun setPackages(v: Set<String>) = p.edit().putStringSet("packages", v).apply()
}
