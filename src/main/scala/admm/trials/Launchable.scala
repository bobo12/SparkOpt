package admm.trials

import spark.SparkContext

/**
 * User: jdr
 * Date: 5/7/12
 * Time: 11:34 AM
 */

trait Launchable {
  var sc: SparkContext = null
  def launch(args: Array[String])

  def launchWithSC(_sc: SparkContext, args: Array[String]) {
    sc = _sc
    launch(args)
  }

  def launchID: Int

}
