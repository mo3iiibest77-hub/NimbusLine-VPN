package com.nimbusline.vpn.xray
import android.content.Context
import android.util.Log
import com.nimbusline.vpn.config.XrayConfigBuilder
import com.nimbusline.vpn.config.VpnProfile
import libv2ray.*
class XrayRuntime(private val c:Context,private val support:V2RayVPNServiceSupportsSet){
 private var point:V2RayPoint?=null
 fun start(profile:VpnProfile){stop();val p=Libv2ray.newV2RayPoint();p.packageName=c.packageName;p.callbacks=object:V2RayCallbacks{override fun onEmitStatus(l:Long,s:String?):Long{Log.i("NimbusXray",s.orEmpty());return 0}};p.setVpnSupportSet(support);p.configureFile="NimbusLine/Config";p.configureFileContent=XrayConfigBuilder.build(profile);point=p;p.runLoop()}
 fun stop(){point?.let{runCatching{it.stopLoop()}};point=null}
 fun running()=point?.isRunning==true
}
