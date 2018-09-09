package com.michael.log.parser

import com.michael.log.models._
import com.michael.log.parser.service.LogKafkaProducer
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.json4s.jackson.{JsonMethods, Serialization}

import scala.util.{Failure, Success, Try}

object SparkLogParserJob extends StrictLogging {

  implicit val formats = org.json4s.DefaultFormats + new org.json4s.ext.EnumNameSerializer(LogLevel)

  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf()
    val batchDuration = Seconds(sparkConf.get("spark.log-parser.batch-duration", "5").toInt)
    val kafkaInputTopic = sparkConf.get("spark.log-parser.kafka.input-topic")
    val kafkaOutputAggreationTopic = sparkConf.get("spark.log-parser.kafka.output-aggregation-topic")
    val kafkaOutputAlertTopic = sparkConf.get("spark.log-parser.kafka.output-alert-topic")
    val kafkaBrokers = sparkConf.get("spark.log-parser.kafka.brokers")
    val kafkaGroupId = sparkConf.get("spark.log-parser.kafka.group")
    val windowDuration = sparkConf.getInt("spark.log-parser.window.duration.seconds", 60)

    lazy val logKafkaProducer = new LogKafkaProducer(kafkaBrokers)

    val ssc = new StreamingContext(sparkConf, batchDuration)
    val kafkaParams = createKafkaParams(kafkaBrokers, kafkaGroupId)

    val inputStream = KafkaUtils.createDirectStream(
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](Seq(kafkaInputTopic), kafkaParams)
    )

    val resultStream = logic(inputStream.map(record => record.value()), windowDuration)

    resultStream.foreachRDD(rdd => rdd.foreach { case (aggregate, maybeAlert) =>
      val outputAgregate = Serialization.write(aggregate)
      logger.info("writing to kafka:" + outputAgregate)
      logKafkaProducer.send(outputAgregate, kafkaOutputAggreationTopic)

      maybeAlert.foreach(alert => {
        val outputAlert = Serialization.write(alert)
        logger.error("writing to kafka:" + outputAlert)
        logKafkaProducer.send(outputAgregate, kafkaOutputAlertTopic)
      })
    })

    ssc.start()
    ssc.awaitTermination()
  }

  def logic(kafkaStream: DStream[String], windowDuration: Int): DStream[(OutputLogAggregate, Option[Alert])] = {
    val logStream = kafkaStream.flatMap(json => parseLogs(json))

    val windowStream = logStream.map(l => InputLogKey(l.host, l.level) -> 1)
      .reduceByKeyAndWindow((c1, c2) => c1 + c2, Seconds(windowDuration))

    windowStream.map { case (logkey, count) =>
      val rate = count.toDouble / windowDuration
      val logAggregate = OutputLogAggregate(logkey.host, logkey.level, count, rate)
      val maybeAlert = if (logkey.level == LogLevel.ERROR && rate > 1.0) Some(Alert(logkey.host, rate)) else None
      (logAggregate, maybeAlert)
    }
  }

  def createKafkaParams(kafkaBrokers: String, groupId: String): Map[String, String] = {
    Map[String, String](
      "bootstrap.servers" -> kafkaBrokers,
      "key.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "value.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "group.id" -> groupId,
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> "true"
    )
  }

  def parseLogs(line: String): Seq[InputLog] = {
    Try(JsonMethods.parse(line).extract[Seq[InputLog]]) match {
      case Success(logs) => logs
      case Failure(e) => logger.warn(s"error parsing message: $line, error: ${e.getLocalizedMessage}"); Seq.empty
    }
  }
}



