package admm.trials

import admm.data.ParallelizedSyntheticData._
import admm.opt.{SLRSparkImmutable, SLRConfig}
import admm.stats.SuccessTracker

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



        stats.dumpToFile
    }

    /*def computeSuccessRates():SuccessTracker{
        val P1 : (Int,  Int,  Int,  Int) = successRate(rdd, Some(zEstKFold), conf)
        val P2 : (Int,  Int,  Int,  Int) = successRate(rddSetValid.asInstanceOf[RDD[ReutersSet]], Some(zEstReg), conf)
    }*/
}
