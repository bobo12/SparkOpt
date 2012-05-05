package admm.trials

import collection.immutable.HashMap
import spark.SparkContext
import admm.stats.SuccessRate.successRate
import admm.data.ParallelizedSyntheticData.generate_data
import admm.opt.ReutersSetID

/**
 * User: jdr
 * Date: 5/3/12
 * Time: 2:55 PM
 */

object Launcher {

  val launchMap = HashMap(
    1 -> sparseTrial _,
    2-> localTest _,
    3 -> kFoldTest _
  )
  var sc: SparkContext = null

  def sparseTrial(args: Array[String]) {
    val outFile = args(0)
    val sparsities = args.view(1, args.length).map(_.toDouble)
    println("sparsities are: ")
    sparsities.foreach(println)
    val nDocs = 50000
    val nFeatures = 300
    val nSlices = 50
    val fn = new java.io.FileWriter(outFile)
    sparsities.foreach(sp => {
      val sln = successRate(generate_data(sc, nDocs, nFeatures, nSlices, sp, .5)).toString()
      fn.write(sp.toString + ":" + sln)
      fn.write("\n")
    })
    fn.close()
    sc.stop()
  }

  def localTest(args: Array[String]) {
    val data = generate_data(sc, 5000, 100, 10, .5, .5)
    println(successRate(data))
  }

  def kFoldTest(args: Array[String]) {
    admm.opt.SolveValidation.kFoldCrossV(ReutersSetID.rddWithIds(generate_data(sc, 1000, 100, 10, .5,.5)).cache(), 2)
  }




  def main(args: Array[String]) {
    val trialId = args(1).toInt
    sc = new SparkContext(args(0), "trial " + trialId.toString)
    val rest = args.length match {
      case 2 => Array[String]()
      case _ => args.view(2, args.length).toArray
    }
    launchMap(trialId)(rest)
  }

}
