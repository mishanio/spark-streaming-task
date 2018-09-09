package com.michael.log

package object models {
  object LogLevel extends Enumeration {
    type LogLevel = Value
    val TRACE, DEBUG, INFO, WARN, ERROR = Value
  }
  import LogLevel._
  case class InputLog(timestamp: Long, host: String, level: LogLevel, text: String)

  case class InputLogKey(host: String, level: LogLevel)

  case class OutputLogAggregate(host: String, level: LogLevel, count: Int, ratePerSecond: Double)

  case class Alert(host: String, error_rate: Double)

}
