package jobs

import ingestor.GithubArchiveIngestor
import org.apache.hadoop.io.compress.GzipCodec
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.io.SnappyCompressionCodec

object GithubDataToS3 extends App {
  val sc = new SparkContext(new SparkConf().setAppName("Github Data To S3"))
  val dayToMove = args(0)

  val hdfsPath = s"/$dayToMove/"
  val githubData = sc.binaryFiles(hdfsPath)
  githubData.saveAsHadoopFile(s"s3://github-archive-data$hdfsPath")
}