package admm.opt


import cern.jet.math.tdouble.DoubleFunctions
import cern.colt.matrix.tdouble.{ DoubleFactory1D}
import spark.{RDD, SparkContext}
import admm.data.ReutersData._
import SLRSparkImmutable.{solve}
import admm.data.ReutersData


/**
 * Created by IntelliJ IDEA.
 * User: Jojo
 * Date: 01/05/12
 * Time: 06:30
 * To change this template use File | Settings | File Templates.
 */


object SolveValidation {

  def kFoldCrossV (rdd: RDD[ReutersSetID], K: Int ) : Double = {

    // s is the percentage of error
    val s = DoubleFactory1D.sparse.make(1)

    for (i <- 1 to K) {

      val rddTrain = rdd.filter(ls => ls.id != i)
      val rddValid = rdd.filter(ls => ls.id == i)

      val zSol = solve(rddTrain)

      s.assign(
        rddValid.map( ls => {
          val A = ls.samples
          val b = ls.outputs
          val m = A.rows()
          val n = A.columns()
          
          val y = DoubleFactory1D.sparse.make(m)
          A.zMult(zSol.viewPart(1,n),y)
          y.assign(DoubleFunctions.greater(0)).assign(b, DoubleFunctions.minus).assign(DoubleFunctions.abs)
          val diff = (y.zSum()).assign(DoubleFunctions.div(m))
          diff
        }
        ).reduce(
          (a, b) => {
            a.assign(b, DoubleFunctions.plus)
            a
          }), DoubleFunctions.plus)
    }

    s.assign(DoubleFunctions.div(K))

    println("s")
    println(s)
  }

  def main(args: Array[String]) {
    val host = args(0)
    val nDocs = args(1).toInt
    val nFeatures = args(2).toInt
    val nSplits = args(3).toInt
    val topicIndex = args(4).toInt
    val K = args(5).toInt
    val hdfsPath = "/root/persistent-hdfs"
    val filePath = "/user/root/data"
    SolveValidation.kFoldCrossV(ReutersData.slicedReutersRDD(new SparkContext(host, "test"),filePath,hdfsPath,nDocs,nFeatures,nSplits,topicIndex),K)
  }



}
