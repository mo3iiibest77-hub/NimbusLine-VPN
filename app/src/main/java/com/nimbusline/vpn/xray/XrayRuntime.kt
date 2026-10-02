package com.nimbusline.vpn.xray

import com.nimbusline.vpn.config.VpnProfile
import com.nimbusline.vpn.config.XrayConfigBuilder
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

class XrayRuntime {
    private var core: CoreController? = null

    fun start(profile: VpnProfile, tunFd: Int) {
        stop()
        Libv2ray.initCoreEnv("", "")
        val callback = object : CoreCallbackHandler {
            override fun startup(): Long = 0L
            override fun shutdown(): Long = 0L
            override fun onEmitStatus(code: Long, msg: String?): Long = 0L
        }
        core = Libv2ray.newCoreController(callback)
        core!!.startLoop(XrayConfigBuilder.build(profile, tunFd), tunFd)
    }

    fun stop() {
        runCatching { core?.stopLoop() }
        core = null
    }

    fun running(): Boolean = core?.isRunning == true
}
