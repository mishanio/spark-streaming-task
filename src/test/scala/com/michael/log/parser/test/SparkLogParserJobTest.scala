package com.michael.log.parser.test

import com.holdenkarau.spark.testing.StreamingSuiteBase
import com.michael.log.models.LogLevel.LogLevel
import com.michael.log.models.{Alert, InputLog, LogLevel, OutputLogAggregate}
import com.michael.log.parser.SparkLogParserJob
import org.apache.spark.streaming.dstream.DStream
import org.json4s.jackson.Serialization
import org.scalatest.FunSuite

class SparkLogParserJobTest extends FunSuite with StreamingSuiteBase {

  implicit val formats = SparkLogParserJob.formats

  def testFunc(windowDuration: Int)(stream: DStream[String]): DStream[(OutputLogAggregate, Option[Alert])] = {
    SparkLogParserJob.logic(stream, windowDuration)
  }


  test("one event produce one aggregate") {
    val windowDuration = 60
    val batch1 = Seq(rec("localhost", LogLevel.ERROR))
    val input = Seq(batch1)

    val expected = Seq(Seq((OutputLogAggregate("localhost", LogLevel.ERROR, 1, 1.0 / windowDuration), Option.empty)))

    testOperation[String, (OutputLogAggregate, Option[Alert])](input, testFunc(windowDuration) _, expected)
  }

  test("two same events produce one combined aggregate") {
    val windowDuration = 60
    val batch1 = Seq(rec("localhost", LogLevel.ERROR))
    val batch2 = Seq(rec("localhost", LogLevel.ERROR))
    val input = Seq(batch1, batch2)

    val expected = Seq(
      Seq((OutputLogAggregate("localhost", LogLevel.ERROR, 1, 1.0 / windowDuration), Option.empty)),
      Seq((OutputLogAggregate("localhost", LogLevel.ERROR, 2, 2.0 / windowDuration), Option.empty))
    )

    testOperation[String, (OutputLogAggregate, Option[Alert])](input, testFunc(windowDuration) _, expected)
  }

  test("two events from two hosts produce two aggregates") {
    val windowDuration = 60
    val batch1 = Seq(rec("localhost", LogLevel.ERROR))
    val batch2 = Seq(rec("anotherhost", LogLevel.ERROR))
    val input = Seq(batch1, batch2)

    val expected = Seq(
      Seq((OutputLogAggregate("localhost", LogLevel.ERROR, 1, 1.0 / windowDuration), Option.empty)),
      Seq(
        (OutputLogAggregate("anotherhost", LogLevel.ERROR, 1, 1.0 / windowDuration), Option.empty),
        (OutputLogAggregate("localhost", LogLevel.ERROR, 1, 1.0 / windowDuration), Option.empty)
      )
    )
    testOperation[String, (OutputLogAggregate, Option[Alert])](input, testFunc(windowDuration) _, expected)
  }

  test("two different events from one host produce two aggregates") {
    val windowDuration = 60
    val batch1 = Seq(rec("localhost", LogLevel.ERROR))
    val batch2 = Seq(rec("localhost", LogLevel.INFO))
    val input = Seq(batch1, batch2)

    val expected = Seq(
      Seq((OutputLogAggregate("localhost", LogLevel.ERROR, 1, 1.0 / windowDuration), Option.empty)),
      Seq(
        (OutputLogAggregate("localhost", LogLevel.ERROR, 1, 1.0 / windowDuration), Option.empty),
        (OutputLogAggregate("localhost", LogLevel.INFO, 1, 1.0 / windowDuration), Option.empty)
      )
    )
    testOperation[String, (OutputLogAggregate, Option[Alert])](input, testFunc(windowDuration) _, expected)
  }

  test("a lot of error events raise alert") {
    val windowDuration = 60
    val batch1 = Seq(rec("localhost", LogLevel.ERROR))
    val batch2 = for (i <- 1 to 60) yield rec("localhost", LogLevel.ERROR)
    val input = Seq(batch1, batch2)

    val expected = Seq(
      Seq((OutputLogAggregate("localhost", LogLevel.ERROR, 1, 1.0 / windowDuration), Option.empty)),
      Seq((OutputLogAggregate("localhost", LogLevel.ERROR, 61, 61.0 / windowDuration), Option(Alert("localhost", 61.0 / windowDuration))))
    )
    testOperation[String, (OutputLogAggregate, Option[Alert])](input, testFunc(windowDuration) _, expected)
  }

  private def rec(host: String, level: LogLevel) = {
    val input = InputLog(System.currentTimeMillis(), host, level, "test")
    val json = Serialization.write(Seq(input))
    json
  }

}

