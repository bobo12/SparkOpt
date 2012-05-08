package admm.trials

import admm.data.ParallelizedSyntheticData._
import admm.opt.{SolveValidation, ReutersSetID, SLRConfig}
import admm.stats.SuccessRate._
import spark.RDD
import admm.data.ReutersData.ReutersSet


/**
 * Created by IntelliJ IDEA.
 * User: Jojo
 * Date: 08/05/12
 * Time: 03:10
 * To change this template use File | Settings | File Templates.
 */

class KFoldTrial extends SLRLaunchable {
  def launchID = 2

  def launchWithConfig(kws: Map[String, String], conf: SLRConfig) {
    val k = kws.get("k").get.toInt
    val sparsityA = kws.get("sparsityA").get.toDouble
    val sparsityW = kws.get("sparsityW").get.toDouble
    val rddSet = ReutersSetID.rddWithIds(generate_data(sc,conf, sparsityA, sparsityW)).cache()
    val zEst = SolveValidation.kFoldCrossV(rddSet, k)
    val P : (Int,  Int,  Int,  Int) = successRate(rddSet.asInstanceOf[RDD[ReutersSet]], Some(zEst))
    println(P)
  }
}
