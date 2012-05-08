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

    val rddSet = ReutersSetID.rddWithIds(generate_data(sc, m , n, nSplits, sparsityA, sparsityW)).cache() // cqche?

    val zEst = SolveValidation.kFoldCrossV(rddSet, K)

    println("zEst found")
    println(zEst)

    val P : (Int,  Int,  Int,  Int) = successRate(rddSet.asInstanceOf[RDD[ReutersSet]], Some(zEst))

    fn.write("P" + "\n")
    fn.write(P.toString())
    fn.close()

    println(P)

  }
}
