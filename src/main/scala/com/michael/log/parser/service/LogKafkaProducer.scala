package com.michael.log.parser.service

import java.util.Properties
import java.util.concurrent.{Future => JFuture}

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}

class LogKafkaProducer(kafkaBrokers: String) {

  private val producer = createProducer(kafkaBrokers)

  def send(body: String, topic: String): JFuture[RecordMetadata] = {
    producer.send(new ProducerRecord[String, String](topic, null, body))
  }

  private def createProducer(kafkaBrokers: String): KafkaProducer[String, String] = {
    val props = new Properties()
    props.put("bootstrap.servers", kafkaBrokers)
    props.put("acks", "1")
    props.put("retries", "0")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    new KafkaProducer[String, String](props)
  }

}
