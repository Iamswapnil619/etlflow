package examples

import etlflow.EtlJobApp
import etlflow.etljobs.EtlJob
import etlflow.utils.Config
import examples.jobs._
import examples.schema.MyEtlJobName._
import examples.schema.{MyEtlJobName, MyEtlJobProps}

object LoadData extends EtlJobApp[MyEtlJobName[MyEtlJobProps], MyEtlJobProps] {

  def toEtlJob(job_name: MyEtlJobName[MyEtlJobProps]): (MyEtlJobProps,Config) => EtlJob = {
    job_name match {
      case Job0DataprocPARQUETtoORCtoBQ => EtlJob0DefinitionDataproc
      case Job1LocalJobDPSparkStep => EtlJob1DefinitionLocal
      case Job2LocalJobGenericStep => EtlJob2DefinitionLocal
      case EtlJob2CSVtoPARQUETtoBQLocalWith3Steps => EtlJob2Definition
      case EtlJob3CSVtoCSVtoBQGcsWith2Steps => EtlJob3Definition
      case EtlJob4BQtoBQ => EtlJob4Definition
      case EtlJob5PARQUETtoJDBC => EtlJob5Definition
      case EtlJob6BQPGQuery => EtlJob6Definition
    }
  }

}