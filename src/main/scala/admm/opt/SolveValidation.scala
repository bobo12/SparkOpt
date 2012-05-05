package admm.opt


import cern.jet.math.tdouble.DoubleFunctions
import spark.{RDD, SparkContext}
import admm.data.ReutersData._
import SLRSparkImmutable.{solve}
import admm.data.ReutersData
import cern.colt.matrix.tdouble.{DoubleMatrix1D, DoubleMatrix2D, DoubleFactory1D}
import collection.immutable.HashMap
import admm.util.ListHelper.list2helper


/**
 * Created by IntelliJ IDEA.
 * User: Jojo
 * Date: 01/05/12
 * Time: 06:30
 * To change this template use File | Settings | File Templates.
 */


case class ReutersSetID(_samples: DoubleMatrix2D, _outputs: DoubleMatrix1D, id: Int) extends ReutersSet {
  def samples = _samples
  def outputs(i: Int) = _outputs
}


object SolveValidation {

  def kFoldCrossV (rdd: RDD[ReutersSetID], k: Int ) : Double = {

    val ids = rdd.map(_.id).toArray()
    val nSlices = ids.size
    val slicesPerK = nSlices / k
    val pairs = ids.toList.chunk(slicesPerK).zipWithIndex.map{
      case (sIds, kId) => {
        sIds.toTraversable.map{
          sId => (sId, kId)
        }
      }
    }.flatten
    val map = HashMap(pairs)



    // s is the percentage of error
    val s = DoubleFactory1D.sparse.make(1)

    for (i <- 1 to k) {

      val rddTrain = rdd.filter(_.id != i)
      val rddValid = rdd.filter(_.id == i)

      val zSol = solve(rddTrain.asInstanceOf[RDD[ReutersSet]])

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

    s.assign(DoubleFunctions.div(k))

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
