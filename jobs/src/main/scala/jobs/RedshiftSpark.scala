package jobs

import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by dtrinh on 5/11/16.
  */
object RedshiftSpark {

  def run(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Redshift Queries")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    val df: DataFrame = sqlContext.read
      .format("com.databricks.spark.redshift")
      .option("url", "jdbc:redshift://redshifthost:5439/database?user=username&password=pass")
      .option("dbtable", "my_table")
      .option("tempdir", "s3n://path/for/temp/data")
      .load()
  }
}
