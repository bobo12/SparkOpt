package admm.trials

import admm.data.ReutersData
import admm.opt.{SLRSparkImmutable, SLRConfig}
import admm.stats.{SuccessTracker, SuccessRate}

/**
 * User: jdr
 * Date: 5/9/12
 * Time: 5:47 PM
 */

class ReutersSimple extends SLRLaunchable{
  def launchWithConfig(kws: Map[String, String], conf: SLRConfig) {
    val filePath = kws("path")
    val hdfsPath = kws("hdfs")
    val rdd = ReutersData.slicedReutersRDD(sc,conf , filePath,hdfsPath)
    val accel = SLRSparkImmutable.solve(rdd, conf)
    val asuc = SuccessRate.successRate(rdd, Some(accel.z), conf = conf)
    println(asuc)
    val accelST = new SuccessTracker
    accelST.stat = accel
    accelST.successResult(asuc)
    accelST.dumpToFile
    sc.stop()
  }

  def launchID = 300



}
