package com.nimbusline.vpn.scanner
object CidrSampler{
 fun sample(c:String,n:Int):List<String>{val x=c.trim().split("/");if(x.size!=2)return emptyList();val b=x[0].split(".").mapNotNull{it.toIntOrNull()};val p=x[1].toIntOrNull()?:return emptyList();if(b.size!=4||p !in 8..32)return emptyList();val base=((b[0].toLong() shl 24)or(b[1].toLong() shl 16)or(b[2].toLong() shl 8)or b[3].toLong())and 0xffffffffL;val size=1L shl(32-p);val first=if(size<=2)base else base+1;val last=if(size<=2)base+size-1 else base+size-2;if(first>last)return emptyList();val k=minOf(n.toLong(),last-first+1).toInt();return(0 until k).map{ip(first+((last-first)*it/maxOf(1,k-1)))}} 
 private fun ip(v:Long)=listOf((v ushr 24)and 255,(v ushr 16)and 255,(v ushr 8)and 255,v and 255).joinToString(".")
}
