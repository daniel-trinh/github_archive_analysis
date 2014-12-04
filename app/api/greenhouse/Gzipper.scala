package githubarchive

import java.io._
import java.util.zip.{GZIPOutputStream, GZIPInputStream}

case class BufferedReaderIterator(reader: BufferedReader) extends Iterator[String] {
  override def hasNext() = reader.ready
  override def next() = reader.readLine()
}


object Gzipper {

  def decompressFromFile(file: java.io.File, encoding: String = "UTF-8"): Iterator[String] = {
    impl(new FileInputStream(file), encoding)
  }
  def decompressFromBytes(bytes: Array[Byte], encoding: String = "UTF-8"): Iterator[String] = {
    impl(new ByteArrayInputStream(bytes), encoding)
  }
  def impl(stream: InputStream, encoding: String) = {
    new BufferedReaderIterator(
      new BufferedReader(
        new InputStreamReader(
          new GZIPInputStream(stream), encoding)))
  }
}