package mainFile

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import akka.stream.ActorMaterializer
import domain.{Comment, PetitionVoting}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api.{DBIO, Database}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}
import repositories.*
import routes.*


object Main extends App with JsonSupport {

  // Создание акторной системы
  implicit val system: ActorSystem = ActorSystem("MyAkkaHttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Создание связи с базой и обьекты репозиториев
  implicit val db: JdbcProfile#Backend#Database = Database.forConfig("slick.dbs.default")
  implicit val userRepo: UserRepository = UserRepository()
  implicit val petitionVotingRepo: PetitionVotingRepository = PetitionVotingRepository()
  implicit val petitionRepo: PetitionRepository = PetitionRepository()
  implicit val commentRepo: CommentRepository = CommentRepository()

  // Создайнеи обьектов роутинга
  private val userRoute = UserRoute()
  private val petitionRoute = PetitionRoute()
  private val commentRoute = CommentRoute()
  private val petitionVotingRoute = PetitionVotingRoute()


  // Добавление путей
  private val allRoutes = userRoute.route ~ 
                          petitionRoute.route ~
                          commentRoute.route ~
                          petitionVotingRoute.route
  
  // Старт сервера
  private val bindingFuture = Http().bindAndHandle(allRoutes, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")

  // Остановка сервера при завершении приложения
  sys.addShutdownHook {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
