package etlflow.api

import etlflow.api.Schema._
import etlflow.executor.Executor
import etlflow.jdbc.{DB, DBServerEnv}
import etlflow.log.{JobRun, StepRun}
import etlflow.utils.{CacheHelper, Config, EtlFlowUtils, JsonJackson, QueueHelper, UtilityFunctions => UF}
import etlflow.webserver.Authentication
import etlflow.{EJPMType, DBEnv, BuildInfo => BI}
import scalacache.caffeine.CaffeineCache
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.stream.ZStream
import scala.reflect.runtime.universe.TypeTag

object Implementation extends EtlFlowUtils with Executor {

  def live[EJN <: EJPMType : TypeTag](
    cache: CaffeineCache[String]
    ,jobSemaphores: Map[String, Semaphore]
    ,jobs: List[EtlJob]
    ,jobQueue: Queue[(String,String,String,String)]
    ,config: Config
    ,ejpm_package: String
  ): ZLayer[Blocking, Throwable, APIEnv] = {
    for {
      subscribers           <- Ref.make(List.empty[Queue[EtlJobStatus]])
      activeJobs            <- Ref.make(0)
    } yield new Service {

      private def getLoginCacheStats:CacheDetails = {
        val data:Map[String,String] = CacheHelper.toMap(cache)
        val cacheInfo = CacheInfo("Login",
          cache.underlying.stats.hitCount(),
          cache.underlying.stats.hitRate(),
          cache.underlying.asMap().size(),
          cache.underlying.stats.missCount(),
          cache.underlying.stats.missRate(),
          cache.underlying.stats.requestCount(),
          data
        )
        CacheDetails("Login",JsonJackson.convertToJsonByRemovingKeysAsMap(cacheInfo,List("data")).mapValues(x => (x.toString)))
      }

      override def getJobs: ZIO[APIEnv with DBServerEnv, Throwable, List[Job]] = DB.getJobs[EJN](ejpm_package)

      override def getCacheStats: ZIO[APIEnv, Throwable, List[CacheDetails]] = Task(List(getPropsCacheStats,getLoginCacheStats))

      override def getQueueStats: ZIO[APIEnv, Throwable, List[QueueDetails]] = QueueHelper.takeAll(jobQueue)

      override def getJobLogs(args: JobLogsArgs): ZIO[APIEnv with DBServerEnv, Throwable, List[JobLogs]] = DB.getJobLogs(args)

      override def getCredentials: ZIO[APIEnv with DBServerEnv, Throwable, List[GetCredential]] = DB.getCredentials

      override def runJob(args: EtlJobArgs, submitter: String): ZIO[APIEnv with Blocking with Clock with DBServerEnv with DBEnv, Throwable, EtlJob] = {
        runActiveEtlJob[EJN](args,jobSemaphores(args.name),config,ejpm_package,submitter,jobQueue)
      }

      override def getDbStepRuns(args: DbStepRunArgs): ZIO[APIEnv with DBServerEnv, Throwable, List[StepRun]] = DB.getStepRuns(args)

      override def getDbJobRuns(args: DbJobRunArgs): ZIO[APIEnv with DBServerEnv, Throwable, List[JobRun]] = DB.getJobRuns(args)

      override def updateJobState(args: EtlJobStateArgs): ZIO[APIEnv with DBServerEnv, Throwable, Boolean] = DB.updateJobState(args)

      override def login(args: UserArgs): ZIO[APIEnv with DBServerEnv, Throwable, UserAuth] =  Authentication.login(args,cache,config.webserver)

      override def getInfo: ZIO[APIEnv, Throwable, EtlFlowMetrics] = {
        for {
          x <- activeJobs.get
          y <- subscribers.get
        } yield EtlFlowMetrics(
          x,
          y.length,
          jobs.length,
          jobs.length,
          build_time = BI.builtAtString
        )
      }

      override def getCurrentTime: ZIO[APIEnv, Throwable, CurrentTime] = UIO(CurrentTime(current_time = UF.getCurrentTimestampAsString()))

      override def addCredentials(args: CredentialsArgs): ZIO[APIEnv with DBServerEnv, Throwable, Credentials] = DB.addCredential(args)

      override def updateCredentials(args: CredentialsArgs): ZIO[APIEnv with DBServerEnv, Throwable, Credentials] = DB.updateCredential(args)

      override def notifications: ZStream[APIEnv, Nothing, EtlJobStatus] = ZStream.unwrap {
        for {
          queue <- Queue.unbounded[EtlJobStatus]
          _     <- UIO(logger.info(s"Starting new subscriber"))
          _     <- subscribers.update(queue :: _)
        } yield ZStream.fromQueue(queue).ensuring(queue.shutdown)
      }
    }
  }.toLayer
}
