package com.v2ray.ang.nimbus

import android.content.Context

object NimbusBootstrap {
    fun initialize(context: Context) {
        NimbusScheduler.schedule(context.applicationContext)
    }
}
