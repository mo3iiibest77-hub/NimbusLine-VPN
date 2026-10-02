package com.nimbusline.vpn.scanner
import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
object ScannerScheduler{fun ensure(c:Context){val r=PeriodicWorkRequestBuilder<ScannerWorker>(2,TimeUnit.HOURS).build();WorkManager.getInstance(c).enqueueUniquePeriodicWork("nimbus-cloudflare-scan",ExistingPeriodicWorkPolicy.UPDATE,r)}}
