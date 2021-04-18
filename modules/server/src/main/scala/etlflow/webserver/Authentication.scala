package etlflow.webserver

import cats.data.{Kleisli, OptionT}
import com.github.t3hnar.bcrypt._
import etlflow.api.EtlFlowTask
import etlflow.api.Schema.{UserArgs, UserAuth}
import etlflow.jdbc.{DB, DBServerEnv}
import etlflow.log.ApplicationLogger
import etlflow.utils.{CacheHelper, Config, WebServer}
import org.http4s.dsl.Http4sDsl
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpRoutes, Request}
import pdi.jwt.{Jwt, JwtAlgorithm}
import scalacache.Cache
import zio.RIO
import zio.interop.catz._

object Authentication extends Http4sDsl[EtlFlowTask] with ApplicationLogger {
  def middleware(service: HttpRoutes[EtlFlowTask], authEnabled: Boolean, cache: Cache[String], config: Option[WebServer]): HttpRoutes[EtlFlowTask] = Kleisli {
    req: Request[EtlFlowTask] =>
      if(authEnabled) {
        req.headers.get(CaseInsensitiveString("Authorization")) match {
          case Some(value) =>
            val token = value.value
              if(Jwt.isValid(token, config.map(_.secretKey.getOrElse("secretKey")).getOrElse("secretKey"), Seq(JwtAlgorithm.HS256))) {
              CacheHelper.getKey(cache, token) match {
                case Some(_) => service(req)
                case None =>
                  logger.warn(s"Expired token $token")
                  OptionT.liftF(Forbidden())
              }
            }
            else {
              logger.warn(s"Invalid token $token")
              OptionT.liftF(Forbidden())
            }
          case None =>
            logger.warn("Header not present. Invalid Request !!")
            OptionT.liftF(Forbidden())
        }
      } else{
        //Return response as it is when authentication is disabled
        service(req)
      }
  }
  def login(args: UserArgs, cache: Cache[String], config: Option[WebServer]): RIO[DBServerEnv, UserAuth] =  {
    DB.getUser(args.user_name).fold(ex => {
      logger.error("Error in fetching user from db => " + ex.getMessage)
      UserAuth("Invalid User/Password", "")
    }, user => {
      if (args.password.isBcryptedBounded(user.password)) {
        val user_data = s"""{"user":"${user.user_name}", "role":"${user.user_role}"}""".stripMargin
        val token = Jwt.encode(user_data, config.map(_.secretKey.getOrElse("secretKey")).getOrElse("secretKey"), JwtAlgorithm.HS256)
        logger.info(s"New token generated for user ${user.user_name}")
        CacheHelper.putKey(cache, token, token, Some(CacheHelper.default_ttl))
        UserAuth("Valid User", token)
      } else {
        UserAuth("Invalid User/Password", "")
      }
    })
  }
}
