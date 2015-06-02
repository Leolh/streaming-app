package com.asiainfo.ocdc.streaming

import java.text.SimpleDateFormat

import com.asiainfo.ocdc.streaming.eventsource.EventSource
import com.asiainfo.ocdc.streaming.tool.DataConvertTool
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SQLContext}

class MCEventSource() extends EventSource() {

  def formatSource(inputs: Array[String]): Option[MCSourceObject] = {
    // 事件ID,时间,LAC,CI,主叫IMEI,被叫IMEI,主叫IMSI,被叫IMSI
    try {
      if (inputs(6) == "000000000000000" && inputs(7) == "000000000000000") {
        logError(" Emsi is wrong ! ")
        None
      } else {
        val sdf = new SimpleDateFormat("yyyymmdd hh:mm:ss")
        val eventID = inputs(0).toInt
        val time = sdf.parse(inputs(1)).getTime

        // FIXME lac ci need convert to 16 , test is 10
        /*val lac = inputs(2).toInt
        val ci = inputs(3).toInt*/
        val lac = DataConvertTool.convertHex(inputs(2))
        val ci = DataConvertTool.convertHex(inputs(3))

        val eventresult = inputs(8).toInt
        val alertstatus = inputs(9).toInt
        val assstatus = inputs(10).toInt
        val clearstatus = inputs(11).toInt
        val relstatus = inputs(12).toInt
        val xdrtype = inputs(13).toInt
        val issmsalone = inputs(14).toInt

        val imei = inputs(4)

        var imsi = ""
        if (eventID == 3 || eventID == 5 || eventID == 7) {
          imsi = inputs(7)
        } else if (eventID == 8 || eventID == 9 || eventID == 10 || eventID == 26) {
          if (issmsalone == 1) imsi = inputs(6)
          else if (issmsalone == 2) imsi = inputs(7)
          else None
        } else {
          imsi = inputs(6)
        }

        Some(new MCSourceObject(eventID, time, lac, ci, imsi, imei, eventresult, alertstatus, assstatus, clearstatus, relstatus, xdrtype, issmsalone))
      }
    } catch {
      case e: Exception => {
        logError(" Source columns have wrong type ! ")
        e.printStackTrace()
        None
      }
    }
  }

  /*override def transform(source: String): Option[MCSourceObject] = {
    val inputArray = source.split(conf.get("delim"))
    if (inputArray.length != conf.getInt("formatlength")) {
      logError(" Source format is wrong ! ")
      logError(" error data --> " + source)
      None
    } else {
      formatSource(inputArray)
    }
  }*/

  override def transform(source: String): Option[(String, MCSourceObject)] = {
    var obj: Option[(String, MCSourceObject)] = None
    val inputArray = source.split(conf.get("delim"))
    if (inputArray.length == conf.getInt("formatlength")) {
      val mcobj = formatSource(inputArray)
      if (mcobj != None) obj = Some((mcobj.get.generateId, mcobj.get))
    }
    obj
  }

  override def transformDF(sqlContext: SQLContext, labeledRDD: RDD[SourceObject]): DataFrame = {
    import sqlContext.implicits.rddToDataFrameHolder
    labeledRDD.map(_.asInstanceOf[MCSourceObject]).toDF()
  }

}


