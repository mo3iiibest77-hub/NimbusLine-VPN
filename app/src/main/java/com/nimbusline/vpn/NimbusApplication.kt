package com.nimbusline.vpn
import android.app.Application
import com.nimbusline.vpn.scanner.ScannerScheduler
class NimbusApplication:Application(){ override fun onCreate(){super.onCreate();ScannerScheduler.ensure(this)} }
