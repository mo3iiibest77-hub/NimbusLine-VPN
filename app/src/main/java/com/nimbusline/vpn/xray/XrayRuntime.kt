package com.nimbusline.vpn.xray
import com.nimbusline.vpn.config.VpnProfile
import com.nimbusline.vpn.config.XrayConfigBuilder
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.InitCoreEnv

class XrayRuntime{
 private var core:CoreController?=null
 fun start(profile:VpnProfile,tunFd:Int){
  stop()
  InitCoreEnv("", "")
  val cb=object:CoreCallbackHandler{
   override fun Startup()=0
   override fun Shutdown()=0
   override fun OnEmitStatus(code:Int,msg:String?)=0
  }
  core=CoreController(cb)
  core!!.startLoop(XrayConfigBuilder.build(profile,tunFd),tunFd)
 }
 fun stop(){core?.stopLoop();core=null}
 fun running()=core?.isRunning==true
}
