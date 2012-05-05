package admm.opt


import cern.jet.math.tdouble.DoubleFunctions
import spark.RDD
import SLRSparkImmutable.{solve}
import cern.colt.matrix.tdouble.{DoubleMatrix1D, DoubleMatrix2D}
import collection.immutable.HashMap
import admm.util.ListHelper.list2helper
import admm.stats.SuccessRate.successRate
import admm.data.ReutersData.ReutersSet


/**
 * Created by IntelliJ IDEA.
 * User: Jojo
 * Date: 01/05/12
 * Time: 06:30
 * To change this template use File | Settings | File Templates.
 */


class ReutersSetID(_samples: DoubleMatrix2D, _outputs: DoubleMatrix1D, _id: Int) extends ReutersSet {
  def samples = _samples
  def outputs(i: Int) = _outputs
  def id = _id
}

object ReutersSetID {
  def toIdSet(rs: ReutersSet) = {
    val id = scala.util.Random.nextInt(10000000)
    new ReutersSetID(rs.samples, rs.outputs(0), id)
  }
  def rddWithIds(rdd: RDD[ReutersSet]) = {
    rdd.map(toIdSet(_))
  }
}


object SolveValidation {

  def kFoldCrossV (rdd: RDD[ReutersSetID], k: Int ) : DoubleMatrix1D = {


    val kMap = {
      val nSlices = rdd.count().toInt
      val pairs = rdd.map(_.id).toArray().toList.chunk(nSlices / k).zipWithIndex.map{
        case (a,b) => (b,a)
      }
      HashMap(pairs: _*)
    }

    val slnAndWeights = (0 until k).map{  kId =>
      val kSlices = kMap.get(kId).get

      val rddTrain = rdd.filter(slice => kSlices.contains(slice.id))
      val rddValid = rdd.filter(slice => !kSlices.contains(slice.id))

      val zSol = solve(rddTrain.asInstanceOf[RDD[ReutersSet]])
      val sr = {
        val tuple = successRate(rddValid.asInstanceOf[RDD[ReutersSet]], Some(zSol))
        val total = tuple._2 + tuple._4
        val right = total - tuple._1 + tuple._3
        right.toDouble / total
      }
      (zSol, sr)
    }.toArray

    val totalWeight = {
      val weights = slnAndWeights.map(_._2)
      weights.reduce(_+_)
    }

    slnAndWeights
      .map{
      case (a,b) => {
        a.copy().assign(DoubleFunctions.mult(b / totalWeight))
      }
    }.reduce{ (a,b) => a.copy().assign(b, DoubleFunctions.plus)}
  }
}
