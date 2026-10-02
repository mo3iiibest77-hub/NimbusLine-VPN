package com.nimbusline.vpn.vpn
import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.*
import androidx.core.app.NotificationCompat
import com.nimbusline.vpn.MainActivity
import com.nimbusline.vpn.config.ProfileRepository
import com.nimbusline.vpn.xray.XrayRuntime
import libv2ray.*
class NimbusVpnService:VpnService(){
 companion object{const val ACTION_START="com.nimbusline.vpn.START";const val ACTION_STOP="com.nimbusline.vpn.STOP";const val EXTRA_PROFILE="profile";private const val CH="vpn"}
 private var tun:ParcelFileDescriptor?=null;private lateinit var runtime:XrayRuntime
 override fun onCreate(){super.onCreate();getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CH,"VPN",NotificationManager.IMPORTANCE_LOW));runtime=XrayRuntime(this,Support())}
 override fun onStartCommand(i:Intent?,f:Int,s:Int):Int{when(i?.action){ACTION_STOP->stopVpn();ACTION_START->{val id=i.getStringExtra(EXTRA_PROFILE)?:return START_NOT_STICKY;val p=ProfileRepository(this).get(id)?:return START_NOT_STICKY;startForeground(7,note(p.remark));runtime.start(p)}};return START_STICKY}
 private fun setup(x:String){val b=Builder().setSession("NimbusLine VPN").setMtu(1500);x.split(" ").filter{it.isNotBlank()}.forEach{a->val p=a.split(",");when(p.firstOrNull()?.firstOrNull()){'m'->b.setMtu(p.getOrNull(1)?.toIntOrNull()?:1500);'a'->if(p.size>2)b.addAddress(p[1],p[2].toInt());'r'->if(p.size>2)b.addRoute(p[1],p[2].toInt());'s'->if(p.size>1)b.addSearchDomain(p[1])}};tun?.close();tun=b.establish()?:error("VPN interface failed")}
 private fun stopVpn(){runtime.stop();tun?.close();tun=null;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
 override fun onDestroy(){stopVpn();super.onDestroy()}
 private fun note(t:String)=NotificationCompat.Builder(this,CH).setSmallIcon(android.R.drawable.stat_sys_warning).setContentTitle("NimbusLine VPN").setContentText(t).setOngoing(true).setContentIntent(PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build()
 private inner class Support:V2RayCallbacks,V2RayVPNServiceSupportsSet{
  override fun shutdown():Long=0
  override fun getVPNFd():Long=tun?.fd?.toLong()?:-1
  override fun prepare():Long=0
  override fun protect(l:Long):Long=if(protect(l.toInt()))0 else 1
  override fun onEmitStatus(l:Long,s:String?):Long=0
  override fun setup(s:String):Long=runCatching{this@NimbusVpnService.setup(s);0L}.getOrElse{-1L}
 }
}
