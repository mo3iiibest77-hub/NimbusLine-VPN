package com.v2ray.ang.nimbus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.handler.IspManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.senpai.CandidateResult
import com.v2ray.ang.senpai.CloudflareScanner
import com.v2ray.ang.senpai.ScanCallback
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

class NimbusScanWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo("NimbusLine: Cloudflare scan"))

        CoreNativeManager.initCoreEnv(applicationContext)

        val profiles = supportedProfiles()
        if (profiles.isEmpty()) return Result.success()

        val base = selectBaseProfile(profiles) ?: return Result.success()
        NimbusPolicy.prepare(base)
        MmkvManager.encodeServerConfig(baseGuid, base)

        val ispKey = NimbusPolicy.SCAN_ISP_KEY
        val stored = IspManager.getProfile(applicationContext, ispKey)

        if (stored == null || stored.goodCidrs.isEmpty()) {
            discover(base.guidForWorker(), ispKey)
        }

        val best = scan(base.guidForWorker(), ispKey) ?: return Result.retry()

        profiles.forEach { (guid, profile) ->
            NimbusPolicy.prepare(profile)
            profile.server = best.ip
            MmkvManager.encodeServerConfig(guid, profile)
        }

        return Result.success()
    }

    private var baseGuid: String = ""

    private fun supportedProfiles(): List<Pair<String, ProfileItem>> {
        return MmkvManager.decodeSubscriptions()
            .flatMap { sub ->
                MmkvManager.decodeServerList(sub.guid).mapNotNull { guid ->
                    MmkvManager.decodeServerConfig(guid)?.let { profile ->
                        if (NimbusPolicy.isSupported(profile)) guid to profile else null
                    }
                }
            }
            .distinctBy { it.first }
    }

    private fun selectBaseProfile(profiles: List<Pair<String, ProfileItem>>): ProfileItem? {
        val selected = MmkvManager.getSelectServer()
        val selectedProfile = profiles.firstOrNull { it.first == selected }
        val pair = selectedProfile ?: profiles.firstOrNull() ?: return null
        baseGuid = pair.first
        return pair.second
    }

    private suspend fun discover(guid: String, ispKey: String) {
        val result = CompletableDeferred<List<String>>()
        CloudflareScanner.discoverGoodCidrs(
            context = applicationContext,
            ispName = ispKey,
            guid = guid,
            onProgress = { done, total, _, _ ->
                setProgress(androidx.work.workDataOf("phase" to "discovery", "done" to done, "total" to total))
            },
            onFinish = { cidrs ->
                if (!result.isCompleted) result.complete(cidrs)
            }
        )
        withTimeout(TimeUnit.MINUTES.toMillis(25)) { result.await() }
    }

    private suspend fun scan(guid: String, ispKey: String): CandidateResult? {
        val result = CompletableDeferred<CandidateResult?>()
        CloudflareScanner.scanForIsp(
            context = applicationContext,
            guid = guid,
            ispName = ispKey,
            callback = object : ScanCallback {
                override fun onProgress(result: CandidateResult, done: Int, total: Int) {
                    setProgress(androidx.work.workDataOf(
                        "phase" to "scan", "done" to done, "total" to total,
                        "ip" to result.ip, "uploadKBps" to result.uploadKBps
                    ))
                }

                override fun onFinish(best: CandidateResult?) {
                    if (!result.isCompleted) result.complete(best)
                }

                override fun onCancelled() {
                    if (!result.isCompleted) result.cancel()
                }
            }
        )
        return withTimeout(TimeUnit.MINUTES.toMillis(15)) { result.await() }
    }

    private fun createForegroundInfo(text: String): ForegroundInfo {
        val channelId = "nimbus_scanner"
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, "NimbusLine Scanner", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = Notification.Builder(applicationContext, channelId)
            .setContentTitle("NimbusLine")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setOngoing(true)
            .build()
        return ForegroundInfo(
            19001,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun ProfileItem.guidForWorker(): String = baseGuid
}
