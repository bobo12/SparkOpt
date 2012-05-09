package admm.trials

import admm.stats.SuccessRate
import admm.data.ParallelizedSyntheticData
import admm.opt.{SLRSparkImmutableOld, SLRSparkImmutable, SLRConfig}

/**
 * User: jdr
 * Date: 5/9/12
 * Time: 12:48 AM
 */

class SimpleTrial extends SLRLaunchable {
  def launchID = 100

  def launchWithConfig(kws: Map[String, String], conf: SLRConfig) {
    val rdd = ParallelizedSyntheticData.generate_data(sc, conf, .5, .5).cache()
    val accel = SLRSparkImmutable.solve(rdd, conf)
    val reg = SLRSparkImmutableOld.solve(rdd, conf)
    val asuc = SuccessRate.successRate(rdd, Some(accel.z), conf = conf)
    val rsuc = SuccessRate.successRate(rdd, Some(reg.z), conf = conf)
  }
}
