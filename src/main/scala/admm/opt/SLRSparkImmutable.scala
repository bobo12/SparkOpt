package admm.opt

import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra
import cern.jet.math.tdouble.DoubleFunctions
import cern.colt.matrix.tdouble.{DoubleFactory1D, DoubleMatrix1D, DoubleFactory2D, DoubleMatrix2D}
import scala.util.control.Breaks._
import admm.util.ADMMFunctions
import admm.data.ReutersData.ReutersSet
import admm.data.ReutersData
import spark.{SparkContext, RDD}
import java.io.FileWriter

/**
 * User: jdr
 * Date: 4/26/12
 * Time: 12:34 PM
 */

object SLRSparkImmutable {
  var rho = 1.0
  var lambda = 0.01
  var nIters = 10
  var topicId = 0
  def solve(rdd: RDD[ReutersSet], _rho: Double = SLRSparkImmutable.rho, _lambda: Double = SLRSparkImmutable.lambda, _nIters: Int = nIters) =  {
    val nSlices = rdd.count() // needed on master machine only

    class DataEnv(samples: DoubleMatrix2D, outputs: DoubleMatrix1D) extends Serializable {
      val rho = _rho
      val lambda = _lambda
      val algebra = new DenseDoubleAlgebra()
      val C = {
        val bPrime = outputs.copy()
        bPrime.assign(DoubleFunctions.mult(2.0)).assign(DoubleFunctions.minus(1.0))
        val Aprime = DoubleFactory2D.sparse.diagonal(bPrime).zMult(samples, null)
        val C = DoubleFactory2D.sparse.appendColumns(bPrime.reshape(bPrime.size().toInt, 1), Aprime)
        C.assign(DoubleFunctions.neg)
        C
      }
      val n = samples.columns()
      val m = samples.rows()
      case class LearningEnv(x: DoubleMatrix1D, u: DoubleMatrix1D, z: DoubleMatrix1D) {
        def xUpdateEnv = {
          println("update x....")
          val xNew = {
            def gradient(x: DoubleMatrix1D): DoubleMatrix1D = {
              val expTerm = C.zMult(x, null)
              expTerm.assign(DoubleFunctions.exp)
              val firstTerm = expTerm.copy()
              firstTerm.assign(DoubleFunctions.plus(1.0))
                .assign(DoubleFunctions.inv)
                .assign(expTerm, DoubleFunctions.mult)
              val secondTerm = x.copy()
              secondTerm.assign(z, DoubleFunctions.minus)
                .assign(u, DoubleFunctions.plus)
                .assign(DoubleFunctions.mult(rho))
              val returnValue = C.zMult(firstTerm, null, 1.0, 1.0, true)
              returnValue.assign(secondTerm, DoubleFunctions.plus)
              returnValue
            }
            def loss(x: DoubleMatrix1D): Double = {
              val expTerm = C.zMult(x, null)
              expTerm.assign(DoubleFunctions.exp)
                .assign(DoubleFunctions.plus(1.0))
                .assign(DoubleFunctions.log)
              val normTerm = x.copy()
              normTerm.assign(z, DoubleFunctions.minus)
                .assign(u, DoubleFunctions.plus)
              val myRho = rho
              val alg = algebra
              val norm = alg.norm2(normTerm)
              val pow = math.pow(norm, 2)
              val sumExp = expTerm.zSum()
              val totSum = sumExp + pow * myRho / 2
              totSum
            }
            def backtracking(x: DoubleMatrix1D, dx: DoubleMatrix1D, grad: DoubleMatrix1D): Double = {
              val t0 = 1.0
              val alpha = .1
              val beta = .5
              val lossX = loss(x)
              val rhsCacheTerm = dx.zDotProduct(grad) * alpha
              def lhs(t: Double): Double = {
                val newX = x.copy()
                newX.assign(dx, DoubleFunctions.plusMultSecond(t))
                loss(newX)
              }
              def rhs(t: Double): Double = {
                lossX + t * rhsCacheTerm
              }
              def helper(t: Double): Double = {
                if (lhs(t) > rhs(t)) helper(beta * t) else t
              }
              helper(t0)
            }
            def descent(x0: DoubleMatrix1D, maxIter: Int): DoubleMatrix1D = {
              val tol = 1e-4
              breakable {
                for (i <- 1 to maxIter) {
                  val dx = gradient(x0)
                  dx.assign(DoubleFunctions.neg)
                  val t = backtracking(x, dx, gradient(x0))
                  x0.assign(dx, DoubleFunctions.plusMultSecond(t))
                  if (algebra.norm2(dx) < tol) break()
                }
              }
              x0
            }
            descent(x,10)
          }
          new LearningEnv(xNew, u, z)
        }
        def uUpdateEnv = {
          println("update u....")
          val uNew = {
            val newU = u.copy()
            newU.assign(x,DoubleFunctions.plus).assign(z,DoubleFunctions.minus)
            newU
          }
          new LearningEnv(x,uNew, z)
        }
        def zUpdateEnv(newZ: DoubleMatrix1D) = {
          new LearningEnv(x, u, newZ)
        }
      }
      def initLearningEnv = {
        new LearningEnv(DoubleFactory1D.sparse.make(n + 1),DoubleFactory1D.sparse.make(n + 1), DoubleFactory1D.sparse.make(n+1))
      }
    }

    def updateSet(oldSet: RDD[DataEnv#LearningEnv]) = {
      val xLS = oldSet
        .map(_.xUpdateEnv)
        .cache()

      val z = {

        val reduced = xLS
          .map(ls => {
          val sum = ls.x.copy()
          sum.assign(ls.u, DoubleFunctions.plus)
          sum})
          .reduce( (a, b) => {
          a.assign(b,DoubleFunctions.plus)})

        reduced
          .assign(DoubleFunctions.div(nSlices.toDouble))
          .viewPart(1,reduced.size().toInt - 1)
          .assign(ADMMFunctions.shrinkage(_lambda/_rho/nSlices.toDouble))

        reduced
      }

      xLS.map(_.zUpdateEnv(z))
        .map(_.uUpdateEnv)
        .cache()
    }

    def stopLearning(rdd: RDD[DataEnv#LearningEnv]): Boolean = {
      false
    }

    def iterate[A](updateFn: A => A, stopFn: A => Boolean, init: A , maxIter: Int) = {
      var iter = 0
      def helper(oldValue: A): A = {
        iter+=1
        (stopFn(oldValue) || (iter > maxIter)) match {
          case true => oldValue
          case _ => helper(updateFn(oldValue))
        }
      }
      helper(init)
    }


    iterate(updateSet,
      stopLearning,
      rdd.map(split => {
        new DataEnv(split.samples, split.outputs(topicId))
      }).cache()
        .map(_.initLearningEnv)
        .cache(), _nIters)
      .take(1)(0)
      .z
  }

}
