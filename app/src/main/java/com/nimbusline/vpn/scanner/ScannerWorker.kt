package com.nimbusline.vpn.scanner
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nimbusline.vpn.config.ProfileRepository
class ScannerWorker(c:Context,p:WorkerParameters):CoroutineWorker(c,p){override suspend fun doWork():Result{val repo=ProfileRepository(applicationContext);val ranges=CloudflareRanges.fetch();if(ranges.isEmpty())return Result.retry();val s=CloudflareScanner();repo.all().forEach{p->s.best(p,ranges)?.let{repo.save(p.copy(address=it.ip));ScannerStore(applicationContext).save(p.id,it.ip)}};return Result.success()}}
