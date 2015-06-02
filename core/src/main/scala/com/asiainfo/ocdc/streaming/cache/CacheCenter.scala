package com.asiainfo.ocdc.streaming.cache

import com.asiainfo.ocdc.streaming.MainFrameConf
import com.asiainfo.ocdc.streaming.constant.CacheConstant
import com.asiainfo.ocdc.streaming.tool.CacheFactory
import scala.collection.mutable

/**
 * Created by leo on 6/1/15.
 */
object CacheCenter {

  val cacheMap = new mutable.HashMap[String, (Long, Any)]()
  val codisManager = CacheFactory.getManager
  val interval = MainFrameConf.getLong("cache_update_interval", CacheConstant.CACHE_UPDATE_INTERVAL)

  def getValue(key: String, field: String, dataType: String): Any = {

    val value = cacheMap.get(key) match {
      case Some(v) => {
        if (isOutTime(v._1)) {
          setValue(key, field, dataType)
        } else v._2
      }
      case None => setValue(key, field, dataType)
      case _ =>
        println("[WARN] unsupported value for cacheMap.get(" + key + ") = " + cacheMap.get(key))
    }
    value
  }

  def setValue(key: String, field: String, dataType: String): Any = {
    var newValue: Any = null
    if ("hash".eq(dataType)) {
      newValue = codisManager.getCommonCacheValue(key, field)
    } else if ("hashall".eq(dataType)) {
      newValue = codisManager.getHashCacheMap(key)
    }
    cacheMap.put(key, (System.currentTimeMillis(), newValue))
    newValue
  }

  def isOutTime(oldTime: Long) = oldTime + interval >= System.currentTimeMillis()
}
