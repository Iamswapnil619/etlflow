import sbt._

object Dependencies {
  private val SparkVersion = "2.4.4"
  private val GcpBqVersion = "1.80.0"
  private val GcpDpVersion = "0.122.1"
  private val GcpGcsVersion = "1.108.0"
  private val ScoptVersion = "3.7.1"
  private val ZioVersion = "1.0.0-RC18-2"
  private val ZioCatsInteropVersion = "2.0.0.0-RC11"
  private val DoobieVersion = "0.8.8"
  private val CalibanVersion = "0.7.5"
  private val FlywayVersion = "6.4.1"
  private val AwsS3Version = "2.13.23"
  private val LogbackVersion = "1.2.3"

  private val ScalaTestVersion = "3.0.5"
  private val TestContainerVersion = "1.11.2"
  private val SparkBQVersion = "0.16.1"
  private val HadoopGCSVersion = "1.6.1-hadoop2"
  private val PgVersion = "42.2.8"

  lazy val googleCloudLibs = List(
    "com.google.cloud" % "google-cloud-bigquery" % GcpBqVersion,
    "com.google.cloud" % "google-cloud-dataproc" % GcpDpVersion,
    "com.google.cloud" % "google-cloud-storage" % GcpGcsVersion,
  )

  lazy val awsLibs = List(
    "software.amazon.awssdk" % "s3" % AwsS3Version
  )

  lazy val sparkLibs = List(
    "org.apache.spark" %% "spark-sql" % SparkVersion,
  )

  lazy val dbLibs = List(
    "org.tpolecat" %% "doobie-core"     % DoobieVersion,
    "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
    "org.tpolecat" %% "doobie-h2"       % DoobieVersion,
    "org.tpolecat" %% "doobie-hikari"   % DoobieVersion,
    "org.tpolecat" %% "doobie-quill"    % DoobieVersion,
    "org.flywaydb" % "flyway-core"      % FlywayVersion,
  )

  lazy val zioLibs = List(
    "dev.zio" %% "zio" % ZioVersion,
    "dev.zio" %% "zio-interop-cats" % ZioCatsInteropVersion,
  )

  lazy val miscLibs = List(
    "com.github.scopt" %% "scopt" % ScoptVersion
  )

  lazy val caliban = List(
    "com.github.ghostdogpr" %% "caliban" % CalibanVersion,
    "com.github.ghostdogpr" %% "caliban-http4s" % CalibanVersion,
    "eu.timepit" %% "fs2-cron-core" % "0.2.2",
  )

  lazy val jwt = List(
    "com.pauldijou" %% "jwt-core" % "4.2.0"
  )

  lazy val testLibs = List(
    "org.scalatest" %% "scalatest" % ScalaTestVersion,
    "org.testcontainers" % "postgresql" % TestContainerVersion,
    "dev.zio" %% "zio-test"     % ZioVersion,
    "dev.zio" %% "zio-test-sbt" % ZioVersion,
    "com.google.cloud.spark" %% "spark-bigquery-with-dependencies" % SparkBQVersion,
    "com.google.cloud.bigdataoss" % "gcs-connector" % HadoopGCSVersion,
    "ch.qos.logback" % "logback-classic" % LogbackVersion,
    "org.postgresql" % "postgresql" % PgVersion,
  ).map(_ % Test)
}
