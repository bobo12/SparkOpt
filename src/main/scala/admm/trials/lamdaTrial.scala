package admm.trials

import admm.data.ParallelizedSyntheticData._
import admm.opt.{SLRSparkImmutable, SLRConfig}
import admm.stats.{SuccessRate, SuccessTracker}
import admm.data.StandardizedData._
import spark.SparkContext

/**
 * Created by IntelliJ IDEA.
 * User: Boris
 * Date: 08/05/12
 * Time: 20:37
 * To change this template use File | Settings | File Templates.
 */

class lamdaTrial extends SLRLaunchable{

  def launchID = 14

  def launchWithConfig(kws: Map[String, String], conf: SLRConfig) {
    /*val rdd = generate_data(sc,conf,.5,.5,math.sqrt(.1))
    val stats = SLRSparkImmutable.solve(rdd, conf)

    //val statsSR = computeSuccessRates()
    val successTracker = new SuccessTracker
    successTracker.stat = stats
    val suc = SuccessRate.successRate(rdd, Some(stats.z), conf = conf)
    successTracker.successResult(suc)
    successTracker.dumpToFile*/
    val Apath = "etc/A.data"
    val Bpath = "etc/b.data"

    val lambda = 0.001
    conf.lam = lambda
    val rdd = slicedLocalStandard(new SparkContext("local","test"),Apath, Bpath,conf)
    val stats = SLRSparkImmutable.solve(rdd, conf)
    val successTracker = new SuccessTracker
    successTracker.stat = stats
    val suc = SuccessRate.successRate(rdd, Some(stats.z), conf = conf)
    successTracker.successResult(suc)
    successTracker.dumpToFile
  }

}
