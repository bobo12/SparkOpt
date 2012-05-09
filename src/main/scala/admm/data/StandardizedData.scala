package admm.data

import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapred.{TextInputFormat, FileInputFormat, JobConf}
import org.apache.hadoop.io.{Text, LongWritable}
import spark.{RDD, SparkContext}
import admm.data.ReutersData.{ReutersSet}
import spark.SparkContext._
import collection.immutable.HashMap
import cern.colt.matrix.tdouble.{DoubleFactory2D, DoubleFactory1D}
import admm.opt.{SLRConfig, SLRSparkImmutable}
import admm.stats.SuccessRate.successRate

/**
 * User: jdr
 * Date: 5/8/12
 * Time: 6:43 PM
 */

object StandardizedData {

  def slicedStandardizedSet(sc: SparkContext, filePath: String, bPath: String, hdfsBase: String, conf: SLRConfig, startDoc: Int = 0, startFeature: Int = 0) = {
    val nFeatures = conf.nFeatures
    val nDocs = conf.nDocs
    val nSlices = conf.nSlices
    val jobConf = new JobConf()
    case class StandardizedReutersSet(data: Seq[(Int, Seq[(Int, Double)], Int)]) extends ReutersSet {
      val m = data.size
      val n = nFeatures
      def outputs(topicId: ReutersData.TopicId) = {
        val items = data.map(_._3.toDouble).toArray.zipWithIndex.filter(_._2 > 0)
        val out = DoubleFactory1D.sparse.make(m)
        items.foreach(pair => {
          out.set(pair._2, pair._1)
        })
        out
      }

      def samples = {
        val pairs = data.map(_._2)
        val out = DoubleFactory2D.sparse.make(m,n)
        pairs.zipWithIndex.foreach(pair => {
          val i  = pair._2
          pair._1.foreach(inPair =>{
            out.set(i, inPair._1 -1, inPair._2)
          })
        })
        out
      }
    }
    jobConf.addResource(new Path(hdfsBase + "/conf/core-site.xml"))
    jobConf.addResource(new Path(hdfsBase + "/conf/hdfs-site.xml"))
    FileInputFormat.addInputPath(jobConf, new Path(bPath))
    val bMap = HashMap(
      sc
        .hadoopRDD(jobConf, classOf[TextInputFormat], classOf[LongWritable], classOf[Text], nSlices)
        .map(pair => pair._2.toString.split(" ")
        .map(_.toInt))
        .map(lst => (lst(0), lst(1))).toArray(): _*
    )
    FileInputFormat.setInputPaths(jobConf,new Path(filePath))

    sc
      .hadoopRDD(jobConf, classOf[TextInputFormat], classOf[LongWritable], classOf[Text], nSlices)
      .map(pair => pair._2.toString.split(" ")).map(splits => {
      val i = splits(0).toInt
      val j = splits(1).toInt
      val value = splits(2).toDouble
      (i,j,value)
    })
      .filter(tup => tup._1 <= (startDoc + nDocs) && tup._2 <= (startFeature + nFeatures) && tup._1 > startDoc && tup._2 > startFeature)
      .groupBy(tup => tup._1)
      .map(tup => (tup._1, tup._2.map(trips => (trips._2, trips._3)), bMap((tup._1+1)/2)))
      .groupBy(_._1 % nSlices).map(tup => tup._2).map(StandardizedReutersSet(_))
    .asInstanceOf[RDD[ReutersSet]]
  }

  def main(args: Array[String]) {
    val hdfsroot = "/usr/local/Cellar/hadoop/1.0.1/libexec"
    val Apath = "/Users/jdr/Documents/github-projects/SparkOpt/etc/A.data"
    val Bpath = "/Users/jdr/Documents/github-projects/SparkOpt/etc/b.data"
    val fn = "/Users/jdr/Desktop/std"
    val ndocs = 200
    val nfeatures = 20
    val nslices = 2
    val conf = new SLRConfig
    conf.nDocs = ndocs
    conf.nFeatures = nfeatures
    conf.nSlices = nslices
    conf.setOutput(fn)
    val trainSet = slicedStandardizedSet(new SparkContext("local[25]","test"),Apath, Bpath,hdfsroot,conf)
    //val testSet = slicedStandardizedSet(new SparkContext("local[25]","test"),Apath, Bpath,hdfsroot,conf, startDoc = ndocs)
    println(successRate(trainSet, conf = conf))
  }

}
