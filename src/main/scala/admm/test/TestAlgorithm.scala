package admm.test

import util.Random
import cern.jet.math.tdouble.DoubleFunctions
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra
import admm.data.ReutersData.ReutersSet
import admm.data.ReutersData
import admm.util.ADMMFunctions
import admm.opt.SLRSparkImmutable
import spark.{RDD, SparkContext}
import admm.util.ListHelper._
import cern.colt.matrix.tdouble.{DoubleFactory1D, DoubleFactory2D, DoubleMatrix1D, DoubleMatrix2D}
import java.io.FileWriter


/**
 * Created by IntelliJ IDEA.
 * User: Boris
 * Date: 30/04/12
 * Time: 13:16
 * To change this template use File | Settings | File Templates.
 */

object TestAlgorithm {

  class TestSet(aMatrix: DoubleMatrix2D, bVector: DoubleMatrix1D) extends ReutersSet with Serializable{
    val samples = aMatrix
    def outputs(topicId: ReutersData.TopicId) = {
      if (topicId!=0) {
        throw new IllegalArgumentException("use 0 as topic ID when you have just one topic!")
      }
      bVector
    }
  }

  class BaseLineDataSet(A:DoubleMatrix2D, w:DoubleMatrix1D,  v:Double, noise: DoubleMatrix1D){
    val aux = A.zMult(w,null).assign(DoubleFunctions.plus(v))
    val bTrue = aux.copy().assign(DoubleFunctions.sign).assign(DoubleFunctions.plus(1.0)).assign(DoubleFunctions.div(2.0))
    val bNoise = aux.copy().assign(noise,DoubleFunctions.plus).assign(DoubleFunctions.sign).assign(DoubleFunctions.plus(1.0)).assign(DoubleFunctions.div(2.0))
    def dataSet(implicit nSlices: Int = 1) = {
      val chunkSize = A.rows() / nSlices
      val splitA = A.toArray.toList.chunk(chunkSize)
      val splitB = bNoise.toArray.toList.chunk(chunkSize)
      splitA.zip(splitB).map{ case (a,b) => {
        new TestSet(DoubleFactory2D.sparse.make(a.toArray),DoubleFactory1D.sparse.make(b.toArray))
      }}
    }
    def singleSet = List(new TestSet(A, bNoise))
    val matrix = A
    val parameter = w
    val offset = v
  }
  /*
    @param m number of lines (=number of samples)
    @param n number of columns (=number of features)
    @param sparsityA sparsity for each line of A
    @param sparsityW sparsity for w, the parameter vector
    @param noiseStd : standard deviation of the noise that we add to the true label vector A*w + v
    @returns a BaseLineDataSet
   */
  def createData(m :Int, n:Int, sparsityA : Double, sparsityW : Double, noiseStd : Double) : BaseLineDataSet =  {
    val w = ADMMFunctions.sprandnvec(n,sparsityW)
    val v = Random.nextGaussian()
    val A = ADMMFunctions.sprandnMatrix(m,n, sparsityA)
    //noise
    val noise = ADMMFunctions.sprandnvec(m,1.0).assign(DoubleFunctions.mult(noiseStd))
    val aux = A.zMult(w,null).assign(DoubleFunctions.plus(v))
    val out = new BaseLineDataSet(A,w,v,noise)
    out
  }

  def main(args: Array[String]) {
    val m = args(0).toInt //nbDocs
    val n = args(1).toInt //nbFeatures
    val sparsityA = args(2).toDouble //sparsity in lines of feature matrix A
    val sparsityW = args(3).toDouble //sparsity in w the true feature vector parameter
    //the coefficient by which we multiply lambdaMax to get our lambda (lambdaMax is calculated from A, b)
    val coefflambda = args(4).toDouble
    val rho = args(5).toDouble
    val proportion = args(6).toDouble //proportion of positive results (rare events)
    //for now we don't use termination criteria, just do the max iterations
    val maxIter = args(7).toInt //for now
    val nSplits = args(8).toInt
    val outFile = args(9)
    val host = args(10)
    val fn = new FileWriter(outFile)

    val stdNoise = DoubleFunctions.sqrt(0.1)
    val data = createData(m,n,sparsityA,sparsityW,stdNoise)

    //calculate lambda max
    //val actualProportion = calculateProportion()
    val posProportion = data.bNoise.zSum() / n
    val negProportion = 1.0 - posProportion

    val bTilde = data.bNoise.copy()
    bTilde.assign(
      bTilde.toArray().map(
        bi => {
          bi match {
            case 1=> {negProportion}
            case 0=> {-posProportion}
          }
        }
      )
    )

    val algebra = new DenseDoubleAlgebra()

    val At = algebra.transpose(data.matrix)
    val lambdaMax = algebra.normInfinity(At.zMult(bTilde,null).assign(DoubleFunctions.abs))
    println("lmax: " + lambdaMax.toString)
    val lambda = coefflambda * lambdaMax
    println("lam : " + lambda.toString)

    val sc = new SparkContext(host, "testing")
    val rddSet: RDD[ReutersSet] = sc.parallelize(data.dataSet(nSplits),nSplits)

    val xEst = SLRSparkImmutable.solve(rddSet, rho, lambda, maxIter)

    val wtrue = data.parameter
    val vtrue = data.offset

    val wEst = xEst.viewPart(1,n)
    val vEst = xEst.getQuick(0)

    val bEst = data.matrix.zMult(wEst,null).assign(DoubleFunctions.plus(vEst)).assign(DoubleFunctions.sign).assign(DoubleFunctions.plus(1.0)).assign(DoubleFunctions.div(2.0))

    //TODO difference between wEst, vEst and

    val diffParam = algebra.norm2(wtrue.assign(wEst,DoubleFunctions.minus))
    val diffOffset = vtrue - vEst

    //calculate predictions
    //see if the classifier does well
    val bEquals = bEst.copy()
    bEquals.assign(data.bNoise,DoubleFunctions.equals)
    fn.write("bEquals\n")
    bEquals.toArray().foreach(b=> fn.write(b.toString + "\n" ))
    val totalSuccess = bEquals.zSum() / m

    val newbPos = bEst.toArray().zip(data.bNoise.toArray()).filter(_._2 == 1).map(_._1)
    var nbPos = 0
    newbPos.foreach(b=> b match {
      case 1 => {nbPos+=1}
      case _ => {}
    })
    fn.write("original number of positive\n")
    fn.write(newbPos.size.toString + "\n")
    val positiveSuccess = (1.0*nbPos)/newbPos.size

    val newbNeg = bEst.toArray().zip(data.bNoise.toArray()).filter(_._2 == 0).map(_._1)
    var nbNeg = 0
    newbNeg.foreach(b=> {b match {
      case 0 => {nbNeg+=1}
      case _ => {}
    }})
    fn.write("original number of negative" + "\n")
    fn.write(newbNeg.size.toString + "\n")
    val negativeSuccess = (1.0*nbNeg)/newbNeg.size

    fn.write("---------------------------------------------------------"+ "\n")
    fn.write("norm 2 of difference between wTrue and wEst"+ "\n")
    fn.write(diffParam.toString+ "\n")
    fn.write("difference between vTrue and vEst"+ "\n")
    fn.write(diffOffset.toString+ "\n")
    fn.write("total Success Rate"+ "\n")
    fn.write(totalSuccess.toString+ "\n")
    fn.write("positive success rate"+ "\n")
    fn.write(positiveSuccess.toString+ "\n")
    fn.write("negative success rate"+ "\n")
    fn.write(negativeSuccess.toString+ "\n")
    fn.close()
    sc.stop()
  }

}
