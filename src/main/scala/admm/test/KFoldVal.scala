package admm.test

import java.io.FileWriter
import spark.{RDD, SparkContext}
import admm.data.ReutersData.ReutersSet
import admm.opt.{SolveValidation, ReutersSetID}
import admm.data.ParallelizedSyntheticData.{generate_data}
import admm.stats.SuccessRate.{successRate}

/**
 * Created by IntelliJ IDEA.
 * User: Jojo
 * Date: 07/05/12
 * Time: 23:35
 * To change this template use File | Settings | File Templates.
 */

object KFoldVal {

  def main(args: Array[String]) {
    val m = args(0).toInt //nbDocs
    val n = args(1).toInt //nbFeatures
    val sparsityA = args(2).toDouble //sparsity in lines of feature matrix A
    val sparsityW = args(3).toDouble //sparsity in w the true feature vector parameter
    val nSplits = args(4).toInt
    val outFile = args(5)
    val host = args(6)
    val fn = new FileWriter(outFile)
    val K = args(7).toInt

    val sc = new SparkContext(host, "testing")

    val rddSet0 : RDD[ReutersSet] = generate_data(sc, m , n, nSplits, sparsityA, sparsityW).cache()

    val rddSet: RDD[ReutersSetID] = ReutersSetID.rddWithIds(rddSet0).cache()

    val zEst = SolveValidation.kFoldCrossV(rddSet, K)
    
    println("zEst found")
    //println(zEst)

    //val P : (Int,  Int,  Int,  Int) = successRate(rddSet0, Some(zEst))

    //fn.write("P")
    //fn.write(P.toString())

  }
}
