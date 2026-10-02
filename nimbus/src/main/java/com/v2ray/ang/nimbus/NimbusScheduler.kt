package com.v2ray.ang.nimbus

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.workDataOf
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NimbusScheduler {
    private const val WORK_NAME = "nimbus-cloudflare-rotation"
    private const val IMMEDIATE_NAME = "nimbus-cloudflare-immediate"

    fun enqueueImmediate(context: Context, guid: String) {
        val request = OneTimeWorkRequestBuilder<NimbusScanWorker>()
            .setInputData(workDataOf(NimbusScanWorker.KEY_GUID to guid))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<NimbusScanWorker>(2, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
