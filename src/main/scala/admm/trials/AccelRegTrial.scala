package admm.trials

import admm.data.ParallelizedSyntheticData
import admm.opt.{SLRSparkImmutableOld, SLRSparkImmutable, SLRConfig}
import admm.stats.{SuccessTracker, SuccessRate}

/**
 * User: jdr
 * Date: 5/9/12
 * Time: 12:48 AM
 */

class AccelRegTrial extends SLRLaunchable {
  def launchID = 100

  def launchWithConfig(kws: Map[String, String], conf: SLRConfig) {
    val rdd = ParallelizedSyntheticData.generate_data(sc, conf, .5, .5).cache()
    val confCopy: SLRConfig = conf.copy
    confCopy.setOutput("/Users/jdr/Desktop/reg")
    conf.setOutput("/Users/jdr/Desktop/accel")
    val accel = SLRSparkImmutable.solve(rdd, conf)
    val reg = SLRSparkImmutableOld.solve(rdd, confCopy)
    val asuc = SuccessRate.successRate(rdd, Some(accel.z), conf = conf)
    val rsuc = SuccessRate.successRate(rdd, Some(reg.z), conf = confCopy)
    val accelST = new SuccessTracker
    accelST.stat = accel
    accelST.successResult(asuc)
    accelST.dumpToFile
    val regST = new SuccessTracker
    regST.stat = reg
    regST.successResult(rsuc)
    regST.dumpToFile
  }
}
