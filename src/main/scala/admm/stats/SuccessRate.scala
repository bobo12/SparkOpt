package admm.stats

import spark.RDD
import admm.data.ReutersData.ReutersSet
import cern.jet.math.tdouble.DoubleFunctions
import cern.colt.matrix.tdouble.{DoubleMatrix1D, DoubleFactory1D, DoubleFactory2D}
import admm.opt.{SLRConfig, SLRSparkImmutable}

/**
 * User: jdr
 * Date: 5/2/12
 * Time: 8:16 PM
 */

object SuccessRate {

  def successRate(dataset: RDD[ReutersSet], zValue: Option[DoubleMatrix1D] = None, conf: SLRConfig = new SLRConfig): (Int, Int, Int, Int) = {
    zValue match {
     // case None => successRate(dataset, Some(SLRSparkImmutable.solve(dataset, conf)), conf)
      case None => successRate(dataset, Some(SLRSparkImmutable.solve(dataset, conf).z))
      case Some(est) => {
        val sample = dataset.take(1).head.samples
        val n = sample.columns()
        val w = est.viewPart(1, n)
        val v = est.get(0)
        dataset.map(slice => {
          val bArr =  slice.outputs(0).toArray
          val aArr = slice.samples.toArray
          val pos = bArr.zipWithIndex.filter(_._1 > 0)
          val neg = bArr.zipWithIndex.filter(_._1 <= 0)
          val posB = pos.map(_._1)
          val negB = neg.map(_._1)
          val nPos = posB.size
          val nNeg = negB.size
          val posInd = pos.map(_._2)
          val negInd = neg.map(_._2)
          val posA = aArr.zipWithIndex.filter{ case (a,ind) => posInd.contains(ind)}.map(_._1)
          val negA = aArr.zipWithIndex.filter{ case (a,ind) => negInd.contains(ind)}.map(_._1)

          val posFail =
            posA.size match {
              case 0 => 0
              case _ => {
            DoubleFactory2D.sparse.make(posA)
            .zMult(w, null)
            .assign(DoubleFunctions.plus(v))
            .assign(DoubleFunctions.sign)
            .assign(DoubleFunctions.plus(1.0))
            .assign(DoubleFunctions.div(2.0))
            .assign(DoubleFactory1D.sparse.make(posB), DoubleFunctions.minus)
            .assign(DoubleFunctions.abs)
            .zSum().toInt
              }
            }
          val negFail =
          negA.size match {
            case 0 => 0
            case _ => {
              DoubleFactory2D.sparse.make(negA)
                .zMult(w, null)
                .assign(DoubleFunctions.plus(v))
                .assign(DoubleFunctions.sign)
                .assign(DoubleFunctions.plus(1.0))
                .assign(DoubleFunctions.div(2.0))
                .assign(DoubleFactory1D.sparse.make(negB), DoubleFunctions.minus)
                .assign(DoubleFunctions.abs)
                .zSum().toInt
            }
          }
          (posFail, nPos, negFail, nNeg)
        }).reduce((a,b) => (a._1 + b._1,a._2 + b._2,a._3 + b._3,a._4 + b._4))
      }
    }
  }
}
