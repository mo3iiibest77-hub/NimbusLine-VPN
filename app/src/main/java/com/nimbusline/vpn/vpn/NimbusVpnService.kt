package com.nimbusline.vpn.vpn
import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.*
import androidx.core.app.NotificationCompat
import com.nimbusline.vpn.MainActivity
import com.nimbusline.vpn.config.ProfileRepository
import com.nimbusline.vpn.xray.XrayRuntime

class NimbusVpnService:VpnService(){
 companion object{const val ACTION_START="com.nimbusline.vpn.START";const val ACTION_STOP="com.nimbusline.vpn.STOP";const val EXTRA_PROFILE="profile";private const val CH="vpn"}
 private var tun:ParcelFileDescriptor?=null;private var runtime=XrayRuntime()
 override fun onCreate(){super.onCreate();getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CH,"VPN",NotificationManager.IMPORTANCE_LOW))}
 override fun onStartCommand(i:Intent?,f:Int,s:Int):Int{when(i?.action){ACTION_STOP->stopVpn();ACTION_START->{val id=i.getStringExtra(EXTRA_PROFILE)?:return START_NOT_STICKY;val p=ProfileRepository(this).get(id)?:return START_NOT_STICKY;runCatching{startTunnel(p)}.onFailure{stopVpn()}}};return START_STICKY}
 private fun startTunnel(p:com.nimbusline.vpn.config.VpnProfile){
  tun?.close()
  val b=Builder().setSession("NimbusLine VPN").setMtu(1500).addAddress("10.0.0.2",16).addRoute("0.0.0.0",0).addDnsServer("1.1.1.1")
  // Nimbus' own Xray sockets bypass this VPN, preventing an outbound loop.
  b.addDisallowedApplication(packageName)
  tun=b.establish()?:error("VPN interface creation failed")
  startForeground(7,note(p.remark))
  runtime.start(p,tun!!.fd)
 }
 private fun stopVpn(){runtime.stop();tun?.close();tun=null;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
 override fun onDestroy(){stopVpn();super.onDestroy()}
 private fun note(t:String)=NotificationCompat.Builder(this,CH).setSmallIcon(android.R.drawable.stat_sys_warning).setContentTitle("NimbusLine VPN").setContentText(t).setOngoing(true).setContentIntent(PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build()
}
