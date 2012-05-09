package admm.trials

import admm.data.ParallelizedSyntheticData._
import admm.opt.{SLRSparkImmutable, SLRConfig}
import admm.stats.{SuccessRate, SuccessTracker}

/**
 * Created by IntelliJ IDEA.
 * User: Boris
 * Date: 08/05/12
 * Time: 20:37
 * To change this template use File | Settings | File Templates.
 */

class BigTrial extends SLRLaunchable{

    def launchID = 13

    def launchWithConfig(kws: Map[String, String], conf: SLRConfig) {
        val rdd = generate_data(sc,conf,.5,.5)
        val stats = SLRSparkImmutable.solve(rdd, conf)

        //val statsSR = computeSuccessRates()
        val successTracker = new SuccessTracker
        successTracker.stat = stats
        val suc = SuccessRate.successRate(rdd, Some(stats.z), conf = conf)
        successTracker.successResult(suc)
        stats.dumpToFile
    }

}
