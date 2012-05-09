package admm.trials

import admm.data.ParallelizedSyntheticData._
import admm.opt.{SolveValidation, ReutersSetID, SLRConfig}
import admm.stats.SuccessRate._
import spark.RDD
import admm.data.ReutersData.ReutersSet
import admm.opt.SLRSparkImmutable._
import collection.mutable.HashMap
import java.io.FileWriter


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
    //val k = kws.get("k").get.toInt
    //val p = kws.get("p").get.toInt
    val sparsityA = kws.get("sparsityA").get.toDouble
    val sparsityW = kws.get("sparsityW").get.toDouble
    val outFile = kws.get("fn").get
    val fn = new FileWriter(outFile)
    
    // if conf.nSlices > p then :
    //assert(conf.nSlices > p)

    val rddSet = ReutersSetID.rddWithIds(generate_data(sc,conf, sparsityA, sparsityW)).cache()

    for (k <- 15 to 17 )  {
      var  p = k

      assert(conf.nSlices > p)
    
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

      println("size")
      println(rddSetTrain.count())
      println(rddSetValid.count())

      val zEstKFold = SolveValidation.kFoldCrossV(rddSetTrain, k, conf)
      val zEstReg = solve(rddSetTrain.asInstanceOf[RDD[ReutersSet]], conf).z

      val P1 : (Int,  Int,  Int,  Int) = successRate(rddSetValid.asInstanceOf[RDD[ReutersSet]], Some(zEstKFold), conf)
      val P2 : (Int,  Int,  Int,  Int) = successRate(rddSetValid.asInstanceOf[RDD[ReutersSet]], Some(zEstReg), conf)

      val Q1 = (1 - P1._1/P1._2.toDouble , 1 - P1._3/P1._4.toDouble)
      val Q2 = (1 - P2._1/P2._2.toDouble , 1 - P2._3/P2._4.toDouble)

      val TotalKFold = 1 - (P1._1 + P1._3)/(P1._2 + P1._4).toDouble
      val TotalReg = 1 - (P2._1 + P2._3)/(P2._2 + P2._4).toDouble

      println(P1)
      fn.write(P1 + "\t")
      println(P2)
      fn.write(P2 + "\t")
      println(Q1)
      fn.write(Q1 + "\t")
      println(Q2)
      fn.write(Q2 + "\t")
      println(TotalKFold)
      fn.write(TotalKFold + "\t")
      println(TotalReg)
      fn.write(TotalReg + "\t" + "\n")
      println("#######################################################################")
      println("#######################################################################")
      println("#######################################################################")
      println(k)

    }

    fn.close()



    /*println("stuff")
    println(conf.nSlices)
    println(conf.nDocs)
    println(conf.nFeatures) */
  }
}
