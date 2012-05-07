package admm.opt

import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra
import cern.jet.math.tdouble.DoubleFunctions
import cern.colt.matrix.tdouble.{DoubleFactory1D, DoubleMatrix1D, DoubleFactory2D, DoubleMatrix2D}
import admm.util.ADMMFunctions
import admm.data.ReutersData.ReutersSet
import spark.RDD
import collection.mutable.ArrayBuffer
import admm.stats.StatTracker

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

  var absTol = 0.0001//this should be tuned
  var relTol = 0.01//tuning less important because it's a relative value

  def solve(rdd: RDD[ReutersSet], _rho: Double = SLRSparkImmutable.rho, _lambda: Double = SLRSparkImmutable.lambda, _nIters: Int = nIters,
             _absTol: Double = SLRSparkImmutable.absTol, _relTol: Double = SLRSparkImmutable.relTol) =  {
    
    var algebra = new DenseDoubleAlgebra()
    val stats = new StatTracker

    object Cache {
      var prevZ: Option[DoubleMatrix1D] = None
      var curZ: Option[DoubleMatrix1D] =  None
      def stashZ(newZ:DoubleMatrix1D) {
        prevZ = curZ
        curZ = Some(newZ)
      }
    }

    val nSlices = rdd.count() // needed on master machine only

    class DataEnv(samples: DoubleMatrix2D, outputs: DoubleMatrix1D) extends Serializable {
      val rho = _rho
      val lambda = _lambda
      var algebra = new DenseDoubleAlgebra()

      val absTol = _absTol
      val relTol = _relTol

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
          stats.iters.head.xTracker.resetIter
          println("update x....")
          val xNew = {
            def gradient(x: DoubleMatrix1D): DoubleMatrix1D = {
              val expTerm = C.zMult(x, null).assign(DoubleFunctions.exp)

              val firstTerm = expTerm
                .copy()
                .assign(DoubleFunctions.plus(1.0))
                .assign(DoubleFunctions.inv)
                .assign(expTerm, DoubleFunctions.mult)

              val secondTerm = x
                .copy()
                .assign(z, DoubleFunctions.minus)
                .assign(u, DoubleFunctions.plus)
                .assign(DoubleFunctions.mult(rho))

              C.zMult(firstTerm, null, 1.0, 1.0, true).assign(secondTerm, DoubleFunctions.plus)
            }
            def loss(x: DoubleMatrix1D): Double = {
              val expTerm = C
                .zMult(x, null)
                .assign(DoubleFunctions.exp)
                .assign(DoubleFunctions.plus(1.0))
                .assign(DoubleFunctions.log)
                .zSum()

              val normTerm = math
                .pow(algebra.norm2(x
                .copy()
                .assign(z, DoubleFunctions.minus)
                .assign(u, DoubleFunctions.plus)),
                2) * rho /2.0

              expTerm + normTerm
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
                if (lhs(t) > rhs(t))
                  helper(beta * t)
                else
                  t
              }
              helper(t0)
            }
            def descent(x0: DoubleMatrix1D, maxIter: Int): DoubleMatrix1D = {
              val tol = 1e-3
              var counter = 0
              def helper(xPrev: DoubleMatrix1D): DoubleMatrix1D = {
                counter +=1
                stats.iters.head.xTracker.iters.head.resetIter
                val grad = gradient(xPrev)
                val norm = algebra.norm2(grad)
                stats.iters.head.xTracker.iters.head.iters.head.gradientNorm = norm
                val direction = grad.copy().assign(DoubleFunctions.neg)
                val t = backtracking(xPrev, direction, grad)
                val xNext = xPrev.copy().assign(direction, DoubleFunctions.plusMultSecond(t))
                if (norm < tol || (counter >= maxIter)) {
                  println("last iter: " + counter.toString)
                  stats.iters.head.xTracker.iters.head.endIter
                  stats.iters.head.xTracker.iters.head.stop
                  xNext
                }
                else {
                  helper(xNext)
                }
              }
              helper(x0)
            }

            descent(x,20)
          }
          new LearningEnv(xNew, u, z)
        }
        def uUpdateEnv = {
          println("update u....")
          stats.iters.head.uTracker.newIter
          stats.iters.head.uTracker.iters.head.start
          val uNew = {
            val newU = u.copy()
            newU.assign(x,DoubleFunctions.plus).assign(z,DoubleFunctions.minus)
            newU
          }
          stats.iters.head.uTracker.iters.head.stop
          new LearningEnv(x,uNew, z)
        }
        def zUpdateEnv(newZ: DoubleMatrix1D) = {
          new LearningEnv(x, u, newZ)
        }

        def xNorm = algebra.norm2(x)
        def uNorm = algebra.norm2(u)

        def primalResidual : Double = {
          algebra.norm2(x.copy().assign(z,DoubleFunctions.minus))
        }

      }
      def initLearningEnv = {
        new LearningEnv(DoubleFactory1D.sparse.make(n + 1),DoubleFactory1D.sparse.make(n + 1), DoubleFactory1D.sparse.make(n+1))
      }
    }

    def updateSet(oldSet: RDD[DataEnv#LearningEnv]) = {
      stats.cur.xTracker.start
      val xLS = oldSet
        .map(_.xUpdateEnv)
        .cache()
      stats.cur.xTracker.stop

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
      Cache.stashZ(z)
      stats.cur.uTracker.start
      val uLS = xLS.map(_.zUpdateEnv(z))
        .map(_.uUpdateEnv)
        .cache()
      stats.cur.uTracker.stop
      println("x track len: " + stats.iters.head.xTracker.iters.length.toString)
      uLS
    }

    /*
    Returns true iff the stopping criteria is met
     */
    def stopLearning(rdd: RDD[DataEnv#LearningEnv]): Boolean = {
      val primalResidual = rdd
        .map(ls => ls.primalResidual)
        .reduce(_+_)
      stats.cur.pRes = primalResidual

      //compute primal residual
      val xNorm = rdd.map(ls => ls.xNorm).reduce(_+_)
      println("x norm " + xNorm)
      val zNorm = algebra.norm2(Cache.curZ.get)
      println("z norm " + zNorm)
      //this is just to take care of the case where the last slice does not have the same sample size
      val avNbSamples = rdd.map(ls => ls.x.size).reduce(_+_) / nSlices.toDouble
      //epsPrimal computation uses same formula as Boyd's 3.3.1
      val epsPrimal = math.sqrt((avNbSamples+1)*nSlices)*absTol + relTol * math.max(xNorm,zNorm)
      stats.cur.pEps = epsPrimal

      val uNorm = rdd.map(ls=>ls.uNorm).reduce(_+_)
      val epsDual = math.sqrt(avNbSamples)*absTol+relTol*rho*uNorm
      //compute dualResidual
      var retour = false
      if(epsPrimal>primalResidual) {
        retour = Cache.prevZ match {
          case None => {
            println("none")
            // there is no prevZ so can't do anything!
            false
          }
          case _ => {
            println("compute dual residual")
            val dualResidual = rho*algebra.norm2(Cache.curZ.get.copy().assign(Cache.prevZ.get,DoubleFunctions.minus))
            stats.cur.dEps = epsDual
            stats.cur.dRes = dualResidual
            if(epsDual>dualResidual) true
            else false
          }
        }
      }
      retour
    }

    def iterate[A](updateFn: A => A, stopFn: A => Boolean, init: A , maxIter: Int) = {
      var iter = 0
      def helper(oldValue: A): A = {
        iter+=1
        stats.resetIter
        (stopFn(oldValue) || (iter > maxIter)) match {
          case true => {
            stats.endIter
            stats.stop
            oldValue
          }
          case _ => helper(updateFn(oldValue))
        }
      }
      helper(init)
    }

    val learningEnvs = rdd.map(split => {
      new DataEnv(split.samples, split.outputs(topicId))
    }).cache()
      .map(_.initLearningEnv)
      .cache()

    //we don't want to try the termination criteria before the first update
    stats.resetIter
    val firstStep = updateSet(learningEnvs)



    val z = iterate(updateSet,
      stopLearning,
      firstStep,
      _nIters)
      .take(1)(0)
      .z
    println(stats)
    z
  }

}

