package etlflow.etlsteps

import etlflow.log.EtlLogger._
import etlflow.utils.LoggingLevel
import etlflow.{JobEnv, StepEnv}
import org.slf4j.{Logger, LoggerFactory}
import zio.{RIO, Task, ZIO}

trait EtlStep[IPSTATE,OPSTATE] { self =>
  val etl_logger: Logger = LoggerFactory.getLogger(getClass.getName)

  val name: String
  val step_type: String = this.getClass.getSimpleName

  def process(input_state: =>IPSTATE): RIO[JobEnv, OPSTATE]
  def getExecutionMetrics: Map[String,Map[String,String]] = Map()
  def getStepProperties(level:LoggingLevel  = LoggingLevel.INFO): Map[String,String] = Map()

  final def execute(input_state: =>IPSTATE): ZIO[StepEnv, Throwable, OPSTATE] = {
    val env = StepLoggerResourceEnv.live >>> StepLoggerImpl.live(self)
    val step = for {
      step_start_time <- Task.succeed(System.currentTimeMillis())
      _   <- logInit(step_start_time)
      op  <- process(input_state).tapError{ex =>
               logError(step_start_time, ex)
             }
      _   <- logSuccess(step_start_time)
    } yield op
    step.provideSomeLayer[StepEnv](env)
  }
}