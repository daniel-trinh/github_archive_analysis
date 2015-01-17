package jobs

import ingestor.GithubArchiveIngestor
import org.apache.hadoop.io.compress.GzipCodec
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.joda.time.DateTime

class GithubDataToS3 {
  def main(args: Array[String]): Unit = {
    val sc = new SparkContext(new SparkConf().setAppName("Github Data To S3"))
    val dayToMove = args(0)
    val time = DateTime.parse(dayToMove)

    val hdfsPath = s"/${time.toString("yyyy-MM-dd")}/"
    val githubData = sc.binaryFiles(hdfsPath)
    githubData.saveAsHadoopFile(s"s3://github-archive-data$hdfsPath")
  }
}