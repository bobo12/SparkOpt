package admm.trials

import admm.data.ParallelizedSyntheticData._
import admm.opt.{SolveValidation, ReutersSetID, SLRConfig}
import admm.stats.SuccessRate._
import spark.RDD
import admm.data.ReutersData.ReutersSet
import admm.opt.SLRSparkImmutable._
import collection.mutable.HashMap


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

    val rddSet = ReutersSetID.rddWithIds(generate_data(sc,conf, sparsityA, sparsityW)).cache()

    val kMap = {
      val pairs1 = rddSet.map(_.id).toArray().toList.take(p)
      val pairs2 = rddSet.map(_.id).toArray().toList.takeRight(conf.nSlices - p)
      val pairs = List(pairs1, pairs2).zipWithIndex.map{
        case (a,b) => (b,a)
      }
      HashMap(pairs: _*)
    }
    
    val kSlices = kMap.get(0).get
    val rddSetTrain = rddSet.filter(slice => kSlices.contains(slice.id))
    val rddSetValid = rddSet.filter(slice => !kSlices.contains(slice.id))

    //val rddSetTrain = sc.parallelize(rddSet.take(p),p).cache()
    //val rddSetValid = sc.parallelize(rddSet.toArray().takeRight(conf.nSlices - p), conf.nSlices - p).cache()
    
    //val rddSetTrain = ReutersSetID.rddWithIds(sc.parallelize(generate_data(sc,conf, sparsityA, sparsityW).take(p),p)).cache()
    //val rddSetValid = ReutersSetID.rddWithIds(sc.parallelize(generate_data(sc,conf, sparsityA, sparsityW).toArray().takeRight(conf.nSlices - p), conf.nSlices - p)).cache()

    println("size")
    println(rddSetTrain.count())
    println(rddSetValid.count())

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
