name := "spark-streaming-task"

version := "0.1"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % Versions.spark % "provided",
  "org.apache.spark" %% "spark-streaming" % Versions.spark % "provided",
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % Versions.spark exclude("org.spark-project.spark", "unused"),
  "org.apache.kafka" % "kafka-clients" % Versions.kafkaClients,
  "com.typesafe.scala-logging" %% "scala-logging" % Versions.scalaLogging,
  "org.json4s" %% "json4s-ext" % Versions.sparkJson,

  "com.holdenkarau" %% "spark-testing-base" % Versions.sparkTest % Test
)

scalaVersion := "2.11.12"

fork in Test := true
javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:+CMSClassUnloadingEnabled")
parallelExecution in Test := false

