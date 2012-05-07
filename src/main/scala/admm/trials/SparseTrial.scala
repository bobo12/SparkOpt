package admm.trials

/**
 * User: jdr
 * Date: 5/7/12
 * Time: 11:39 AM
 */
import admm.stats.SuccessRate.successRate
import admm.data.ParallelizedSyntheticData.generate_data

class SparseTrial extends Launchable {

  def launch(args: Array[String]) {
    val outFile = args(0)
    val sparsities = args.view(1, args.length).map(_.toDouble)
    println("sparsities are: ")
    sparsities.foreach(println)
    val nDocs = 500
    val nFeatures = 30
    val nSlices = 5
    val fn = new java.io.FileWriter(outFile)
    sparsities.foreach(sp => {
      val sln = successRate(generate_data(sc, nDocs, nFeatures, nSlices, sp, .5)).toString()
      fn.write(sp.toString + ":" + sln)
      fn.write("\n")
    })
    fn.close()
    sc.stop()
  }

  def launchID = 1

}
