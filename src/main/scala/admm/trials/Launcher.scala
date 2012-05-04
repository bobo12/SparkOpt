package admm.trials

import collection.immutable.HashMap
import spark.SparkContext
import admm.stats.SuccessRate.successRate
import admm.data.ParallelizedSyntheticData.generate_data

/**
 * User: jdr
 * Date: 5/3/12
 * Time: 2:55 PM
 */

object Launcher {

  val launchMap = HashMap(1 -> sparseTrial _)
  var host = "local"

  def sparseTrial(args: Array[String]) {
    println("args: ")
    args.foreach(println)
    val outFile = args(0)
    val sparsities = args.view(1, args.length).map(_.toDouble)
    println("sparsities are: ")
    sparsities.foreach(println)
    val nDocs = 50000
    val nFeatures = 300
    val nSlices = 50
    val sc = new SparkContext(host, "test")
    val fn = new java.io.FileWriter(outFile)
    sparsities.foreach(sp => {
      println(sp)
      val sln = successRate(generate_data(sc, nDocs, nFeatures, nSlices, sp, .5)).toString()
      println(sln)
      fn.write(sp.toString + ":" + sln)
      fn.write("\n")
    })
    fn.close()
    sc.stop()
  }


  def main(args: Array[String]) {
    host = args(0)
    val trialId = args(1).toInt
    val rest = args.length match {
      case 2 => Array[String]()
      case _ => args.view(2, args.length).toArray
    }
    launchMap(trialId)(rest)
  }

}
