package admm.trials

import admm.data.ParallelizedSyntheticData._
import admm.opt.{SolveValidation, ReutersSetID, SLRConfig}
import admm.stats.SuccessRate._
import spark.RDD
import admm.data.ReutersData.ReutersSet
import admm.opt.SLRSparkImmutable._


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
    val p = kws.get("p").get.toInt
    val sparsityA = kws.get("sparsityA").get.toDouble
    val sparsityW = kws.get("sparsityW").get.toDouble
    
    // if conf.nSlices > p then :
    assert(conf.nSlices > p)

    val rddSet = ReutersSetID.rddWithIds(generate_data(sc,conf, sparsityA, sparsityW))
    val rddSetTrain = sc.parallelize(rddSet.take(p),p).cache()
    val rddSetValid = sc.parallelize(rddSet.toArray().takeRight(conf.nSlices - p), conf.nSlices - p).cache()
    
    //val rddSetTrain = ReutersSetID.rddWithIds(sc.parallelize(generate_data(sc,conf, sparsityA, sparsityW).take(p),p)).cache()
    //val rddSetValid = ReutersSetID.rddWithIds(sc.parallelize(generate_data(sc,conf, sparsityA, sparsityW).toArray().takeRight(conf.nSlices - p), conf.nSlices - p)).cache()

    val zEstKFold = SolveValidation.kFoldCrossV(rddSetTrain, k, conf)
    val zEstReg = solve(rddSetTrain.asInstanceOf[RDD[ReutersSet]], conf).z

    val P1 : (Int,  Int,  Int,  Int) = successRate(rddSetValid.asInstanceOf[RDD[ReutersSet]], Some(zEstKFold), conf)
    val P2 : (Int,  Int,  Int,  Int) = successRate(rddSetValid.asInstanceOf[RDD[ReutersSet]], Some(zEstReg), conf)

    println(P1)
    println(P2)

    println("stuff")
    println(conf.nSlices)
    println(conf.nDocs)
    println(conf.nFeatures)
  }
}
