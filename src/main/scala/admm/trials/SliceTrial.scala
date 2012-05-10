package admm.trials

import admm.data.ParallelizedSyntheticData.generate_data
import admm.opt.{SLRSparkImmutable, SLRConfig}
import admm.stats.{SuccessRate, SuccessTracker}
import spark.SparkContext
import admm.data.StandardizedData.slicedLocalStandard

/**
 * User: jdr
 * Date: 5/8/12
 * Time: 5:48 PM
 */

class SliceTrial extends Launchable {

  def launchID = 10

  def launch(args: Array[String]) {
    val slices = args(0).split(",").map(_.toInt)
    val nd = args(1).toInt
    val fn = args(2)
    val conf = new SLRConfig
    conf.nDocs = nd
    conf.setOutput(fn)
    slices.foreach{slice =>{
      conf.nSlices = slice
      val rdd = generate_data(sc,conf,.5,.5)
      val stats = SLRSparkImmutable.solve(rdd, conf)
      stats.dumpToFile
    }}
  }
}

class SliceTrial2 extends KeywordLaunchable {

  def launchID = 11

  def launchWithKeywords(kws: Map[String, String]) {
    val slices = kws.get("slices").get.split(",").map(_.toInt)
    val nd = kws.get("nd").get.toInt
    val fn = kws.get("fn").get
    val conf = new SLRConfig
    conf.nDocs = nd
    conf.setOutput(fn)
    slices.foreach{slice =>{
      conf.nSlices = slice
      val rdd = generate_data(sc,conf,.5,.5)
      val stats = SLRSparkImmutable.solve(rdd, conf)
      stats.dumpToFile
    }}
  }
}

class SliceTrial3 extends SLRLaunchable {

  def launchID = 12

  def launchWithConfig(kws: Map[String, String], conf: SLRConfig) {
    /* val slices = kws.get("slices").get.split(",").map(_.toInt)
     val fn = kws.get("fn").get
     slices.foreach{slice =>{
       conf.setOutput(fn+slice.toString)
       conf.nSlices = slice
       val rdd = generate_data(sc,conf,.5,.5)
       val stats = SLRSparkImmutable.solve(rdd, conf)
       val successTracker = new SuccessTracker
       successTracker.stat = stats
       val suc = SuccessRate.successRate(rdd, Some(stats.z), conf = conf)
       successTracker.successResult(suc)
       successTracker.dumpToFile
     }}
   } */
    val slices = kws.get("slices").get.split(",").map(_.toInt)
    val fn = kws.get("fn").get
    val Apath = "etc/A.data"
    val Bpath = "etc/b.data"
    slices.foreach{slice =>{
      conf.setOutput(fn+slice.toString)
      conf.nSlices = slice
      val rdd = slicedLocalStandard(new SparkContext("local","test"),Apath, Bpath,conf)
      val stats = SLRSparkImmutable.solve(rdd, conf)
      val successTracker = new SuccessTracker
      successTracker.stat = stats
      val suc = SuccessRate.successRate(rdd, Some(stats.z), conf = conf)
      successTracker.successResult(suc)
      successTracker.dumpToFile
    }}
  }
}

