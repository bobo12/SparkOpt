package admm.data

import admm.util.ADMMFunctions
import spark.SparkContext
import util.Random
import cern.jet.math.tdouble.DoubleFunctions
import cern.colt.matrix.tdouble.{DoubleMatrix2D, DoubleFactory1D, DoubleMatrix1D}
import admm.data.ReutersData.ReutersSet
import admm.stats.SuccessRate.successRate

/**
 * User: jdr
 * Date: 5/2/12
 * Time: 7:14 PM
 */

object ParallelizedSyntheticData {

  case class ParSynSet(_samples: DoubleMatrix2D, _outputs: DoubleMatrix1D) extends ReutersSet {
    def outputs(tid: Int) = _outputs
    def samples = _samples
  }

  def generate_data(sc: SparkContext, nSamples: Int, nFeatures: Int, nSplits: Int, sparsityA: Double, sparsityW: Double) = {
    val sPerS = nSamples / nSplits
    val w = sc.broadcast(ADMMFunctions.sprandnvec(nFeatures,sparsityW))
    //val v = sc.broadcast(-1*w.value.zSum()/w.value.size().toDouble)
    val v = sc.broadcast(Random.nextGaussian())
    sc.parallelize(1 to nSplits).map(_ => {
      val A = ADMMFunctions.sprandnMatrix(sPerS, nFeatures, sparsityA)
      val b = {
        val bDense = A
          .zMult(w.value,null)
          .assign(DoubleFunctions.plus(v.value))
          .assign(DoubleFunctions.sign)
          .assign(DoubleFunctions.plus(1))
          .assign(DoubleFunctions.div(2))
        DoubleFactory1D.sparse.make(bDense.toArray)
      }
      ParSynSet(A,b).asInstanceOf[ReutersSet]
    })
  }
}
