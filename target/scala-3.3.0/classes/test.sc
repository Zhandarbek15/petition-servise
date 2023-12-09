import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api.Database

import scala.concurrent.duration.*
import akka.http.scaladsl.server.Directives.*
import domain.*
import repositories.*
import routes.validateCustom

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future, duration}
import scala.util.{Failure, Success}

implicit val system: ActorSystem = ActorSystem("MyAkkaHttpServer")
implicit val materializer: ActorMaterializer = ActorMaterializer()
implicit val executionContext: ExecutionContextExecutor = system.dispatcher

// Создание связи с базой и обьекты репозиториев
implicit val db: JdbcProfile#Backend#Database = Database.forConfig("slick.dbs.default")
implicit val userRepo: UserRepository = UserRepository()
implicit val petitionVotingRepo: PetitionVotingRepository = PetitionVotingRepository()
implicit val petitionRepo: PetitionRepository = PetitionRepository()
implicit val commentRepo: CommentRepository = CommentRepository()


val aa = petitionRepo.checkOldPetitionStatus(Some(1))
println(Await.result(aa,duration.Duration.Inf))

