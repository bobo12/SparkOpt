package admm.trials

import collection.immutable.HashMap
import spark.SparkContext
import admm.stats.SuccessRate.successRate
import admm.data.ParallelizedSyntheticData.generate_data
import admm.opt.ReutersSetID

/**
 * User: jdr
 * Date: 5/3/12
 * Time: 2:55 PM
 */

object Launcher {

  val registeredApps = List(new SparseTrial)
  val launchMap = HashMap(registeredApps.map(l => l.launchID -> l): _*)

  def main(args: Array[String]) {
    val launchID = args(1).toInt
    val sc = new SparkContext(args(0), "launch " + launchID.toString)
    launchMap(launchID).launchWithSC(sc, args.view(2,args.length).toArray)
  }

}
