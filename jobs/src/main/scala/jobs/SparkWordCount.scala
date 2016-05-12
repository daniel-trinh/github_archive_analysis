package jobs

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql._
import org.apache.spark.sql._

object SparkWordCount {

  def isHeader(line: String): Boolean = {
    line.contains("id_1")
  }

  def toDouble(s: String): Double = {
    if (s.equals("?")) Double.NaN else s.toDouble
  }

  case class MatchData(
    id1: Int,
    id2: Int,
    scores: Array[Double],
    matched: Boolean
  )

  def parse(line: String): MatchData = {
    val pieces = line.split(',')
    MatchData(
      id1 = pieces(0).toInt,
      id2 = pieces(1).toInt,
      scores = pieces.slice(2, 11).map(toDouble),
      matched = pieces(11).toBoolean
    )
  }

  import org.apache.spark.util.StatCounter
  class NAStatCounter extends Serializable {
    val stats: StatCounter = new StatCounter()
    var missing: Long = 0L

    def add(x: Double): NAStatCounter = {
      if (java.lang.Double.isNaN(x)) {
        missing += 1
      } else {
        stats.merge(x)
      }
      this
    }

    def merge(other: NAStatCounter): NAStatCounter = {
      stats.merge(other.stats)
      missing += other.missing
      this
    }

    override def toString = {
      s"stats: ${stats.toString()} NaN: $missing"
    }
  }

  object NAStatCounter extends Serializable {
    def apply(x: Double) = new NAStatCounter().add(x)
  }

  def setupS3(accessKey: String, secretKey: String, sc: SparkContext) = {
    val hadoopConf=sc.hadoopConfiguration

    hadoopConf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
    hadoopConf.set("fs.s3a.access.key",accessKey)
    hadoopConf.set("fs.s3a.secret.key",secretKey)
  }

  def run(args: Array[String]) {
    val logFile = "s3a://github-archive-data/2013/03/03/2.json.gz"
    val conf = new SparkConf().setAppName("Simple Application")
    val sc = new SparkContext(conf)

    val myAccessKey="AKIAJ6H6IE5EQFTQJYSA"
    val mySecretKey="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"


    val sqlContext = new org.apache.spark.sql.SQLContext(sc)

    // this is used to implicitly convert an RDD to a DataFrame.
    import sqlContext.implicits._

    val lines = sqlContext.read.text("/Users/dtrinh/src/oss/sparkdata/linkage").as[String]

    val filteredRDD = lines.filter(!isHeader(_)).map(parse(_)).cache()

    sqlContext.sql(
      """
        |SELECT count(type), type, hour(created_at) FROM GithubEvents
        |GROUP BY hour(created_at)
      """.stripMargin).show(500)
  }

}
