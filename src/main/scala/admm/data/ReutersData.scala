package admm.data

/**
 * User: jdr
 * Date: 3/20/12
 * Time: 8:06 PM
 */


import cern.colt.matrix.tdouble.{DoubleFactory1D, DoubleFactory2D, DoubleMatrix1D, DoubleMatrix2D}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapred.{TextInputFormat, FileInputFormat, JobConf}
import org.apache.hadoop.io.{Text, LongWritable}
import spark.SparkContext._
import spark.{RDD, SparkContext}

object ReutersData {

  type IDFId = Int
  type TopicId = Int
  type IDFScore = Double
  type IDFRecord = (IDFId, IDFScore)
  type SampleSet = DoubleMatrix2D
  type OutputSet = DoubleMatrix1D

  trait ReutersRecord {
    def topics: Set[TopicId]
    def idfRecords: Iterable[IDFRecord]
    def containsTopic(topic: TopicId): Boolean = topics.contains(topic)
  }

  case class UntaggedRecord(line: String) extends ReutersRecord {
    val (_topics: Set[TopicId], _idfRecords: Iterable[IDFRecord]) = {
      val splits = line.split("  ")
      val tops: Iterable[TopicId] = splits.head.split(",").map{_.toInt}.toSet
      val recs: Iterable[IDFRecord] = splits.last.split(" ").map(pair =>{
        val splits = pair.split(":")
        (splits.head.toInt, splits.tail.head.toDouble)
      })
      (tops, recs)
    }
    def topics = _topics
    def idfRecords = _idfRecords
  }

  case class TaggedRecord(tag: Int, utRec: UntaggedRecord) extends ReutersRecord {
    val topics = utRec.topics
    val idfRecords = utRec.idfRecords
  }

  object TaggedRecord {
    def parse(line: String) = {
      val splits = line.split(" ", 2)
      TaggedRecord(splits.head.toInt, UntaggedRecord(splits.last))
    }
  }
  object ReutersRecord {
    def parse(line: String): ReutersRecord = {
      TaggedRecord.parse(line)
    }
  }

  trait ReutersSet {
    def samples: SampleSet
    def outputs(topicId: TopicId): OutputSet
    def generateReutersSet(topicId: TopicId) = (samples, outputs(topicId))
  }

  class UntaggedReutersSet(records: scala.Seq[UntaggedRecord], n: Int) extends ReutersSet with Serializable {
    val m = records.size
    def samples: SampleSet = {
      val matrix = DoubleFactory2D.sparse.make(m,n)
      records.zipWithIndex.foreach{
        case (record, recordInd) => {
          record.idfRecords.filter{case (idfId, idfScore) => idfId < n}.foreach{
            case (idfId, idfScore) => {
              matrix.setQuick(recordInd, idfId,idfScore)
            }
          }
        }
      }
      matrix
    }
    def outputs(topicId: TopicId): OutputSet = {
      val vector = DoubleFactory1D.sparse.make(m)
      records.zipWithIndex.foreach{
        case (record, recordInd) => {
          if (record.topics.contains(topicId)) vector.setQuick(recordInd,1.0)
        }}
      vector
    }
  }

  def slicedReutersRDD(sc: SparkContext, filePath: String, hdfsBase: String, nDocs: Int, nFeatures: Int, nSlices: Int, topicId: Int) = {
    val jobConf = new JobConf()
    jobConf.addResource(new Path(hdfsBase + "/conf/core-site.xml"))
    jobConf.addResource(new Path(hdfsBase + "/conf/hdfs-site.xml"))
    FileInputFormat.addInputPath(jobConf, new Path(filePath))
    val rdd = sc.hadoopRDD(jobConf, classOf[TextInputFormat], classOf[LongWritable], classOf[Text], nSlices)
    val strings = rdd.map(pair => pair._2.toString)
    val idStrings = strings.map(line => {
      val splits = line.split(" ", 2)
      val id = splits.head.toInt
      val content = splits.tail.head
      (id, content)
    })
    val limitedIds = idStrings.filter(_._1 < nDocs)
    val modIds = limitedIds.map(pair => (pair._1 % nSlices, pair._2))
    val groups = modIds.groupByKey(nSlices)
    val sets: RDD[ReutersSet] = groups.map(_._2).map(lines => new UntaggedReutersSet(lines.map(UntaggedRecord(_)), nFeatures))
    sets
  }





}
