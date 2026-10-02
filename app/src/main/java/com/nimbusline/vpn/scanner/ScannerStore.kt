package com.nimbusline.vpn.scanner
import android.content.Context
class ScannerStore(c:Context){private val p=c.getSharedPreferences("scanner",Context.MODE_PRIVATE);fun save(id:String,ip:String)=p.edit().putString(id,ip).apply();fun get(id:String)=p.getString(id,null)}
