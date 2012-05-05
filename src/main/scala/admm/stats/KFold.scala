package admm.stats

import cern.colt.matrix.tdouble.{DoubleMatrix1D, DoubleMatrix2D}
import admm.data.ReutersData.ReutersSet
import spark.RDD


/**
 * User: jdr
 * Date: 5/4/12
 * Time: 5:47 PM
 */

case class ReutersSetWithId(_samples: DoubleMatrix2D, _outputs: DoubleMatrix1D, id: Int) extends ReutersSet {
  def samples = _samples
  def outputs(id: Int) = _outputs
}

object KFold {
  def solve(rdd: RDD[ReutersSetWithId], k: Int) {
    val nSlices = rdd.count()
    val ids = rdd.map(_.id).toArray()
    // assume even divide
    val slicesPerK = nSlices / k

  }
}
