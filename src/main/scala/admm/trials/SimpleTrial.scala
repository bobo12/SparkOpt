package admm.trials

import admm.opt.{SLRSparkImmutableOld, SLRSparkImmutable, SLRConfig}
import admm.stats.{SuccessTracker, SuccessRate}
import admm.data.{StandardizedData, ParallelizedSyntheticData}

/**
 * User: jdr
 * Date: 5/9/12
 * Time: 12:48 AM
 */

class SimpleTrial extends SLRLaunchable {
  def launchID = 200

  def launchWithConfig(kws: Map[String, String], conf: SLRConfig) {
    val rdd = StandardizedData.slicedLocalStandard(sc, "etc/A.data" ,"etc/b.data",conf).cache()
    val accel = SLRSparkImmutable.solve(rdd, conf)
    val asuc = SuccessRate.successRate(rdd, Some(accel.z), conf = conf)
    val accelST = new SuccessTracker
    accelST.stat = accel
    accelST.successResult(asuc)
    accelST.dumpToFile
  }
}

class SimpleHDFSTrial extends SLRLaunchable {
  def launchID = 201

  def launchWithConfig(kws: Map[String, String], conf: SLRConfig) {
    val aPath = kws("apath")
    val bPath = kws("bpath")
    val hdfsPath = kws("hdfs")
    val rdd = StandardizedData.slicedStandardizedSet(sc,aPath,bPath,hdfsPath,conf).cache()
    val accel = SLRSparkImmutable.solve(rdd, conf)
    val asuc = SuccessRate.successRate(rdd, Some(accel.z), conf = conf)
    val accelST = new SuccessTracker
    accelST.stat = accel
    accelST.successResult(asuc)
    accelST.dumpToFile
    sc.stop()
  }
}
