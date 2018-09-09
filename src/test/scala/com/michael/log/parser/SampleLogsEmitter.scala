package com.michael.log.parser

import java.util.concurrent.{Executors, TimeUnit}

import com.michael.log.models.{InputLog, LogLevel}
import com.michael.log.parser.service.LogKafkaProducer
import com.typesafe.scalalogging.StrictLogging
import org.json4s.jackson.Serialization


object SampleLogsEmitter extends StrictLogging {

  def main(args: Array[String]): Unit = {
    implicit val formats = SparkLogParserJob.formats

    val brokers = "localhost:9092"
    val topic = "log-parser-input-topic"
    val producer = new LogKafkaProducer(brokers)

    val sheduler = Executors.newScheduledThreadPool(1)
    logger.info("start emitting log events")
    sheduler.scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        val inputs = Seq(InputLog(System.currentTimeMillis(), "localhost", LogLevel.INFO, "test"),
          InputLog(System.currentTimeMillis(), "anotherhost", LogLevel.ERROR, "test")
        )
        val inputs2 = Seq(InputLog(System.currentTimeMillis(), "localhost", LogLevel.ERROR, "test"),
          InputLog(System.currentTimeMillis(), "anotherhost", LogLevel.ERROR, "test"),
          InputLog(System.currentTimeMillis(), "localhost", LogLevel.INFO, "test"))

        val inputs3 = Seq(InputLog(System.currentTimeMillis(), "localhost", LogLevel.DEBUG, "test"),
          InputLog(System.currentTimeMillis(), "anotherhost", LogLevel.TRACE, "test"))

        producer.send(Serialization.write(inputs), topic)
        producer.send(Serialization.write(inputs2), topic)
        producer.send(Serialization.write(inputs3), topic)
      }
    }, 0, 1000, TimeUnit.MILLISECONDS)
  }
}
