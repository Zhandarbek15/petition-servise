package routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import domain.*
import repositories.{JsonSupport, PetitionRepository, UserRepository}

import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

class PetitionRoute(implicit val petitionRepo:PetitionRepository, val userRepo:UserRepository,val ex:ExecutionContext)
  extends JsonSupport {
  
  // field параметрінің дұрыстығын тексеру үшін
  private val fields:List[String] = List(
    "id",
    "autor_id",
    "name",
    "description",
    "date_create",
    "goal_of_votes",
    "votes",
    "status")

  val route = pathPrefix("petitions") {
    pathEndOrSingleSlash {
      (get & parameters("field", "parameter")) {
        (field, parameter) => {
          if(!fields.contains(field)) {
            complete(StatusCodes.BadRequest,
            s"Вы ввели неправильный имя поля таблицы! Допустимые поля: ${fields.mkString(", ")}")
          }
          else {
            onComplete(petitionRepo.customFilter(field, parameter)) {
              case Success(queryResponse) => complete(StatusCodes.OK, queryResponse.toList)
              case Failure(_) => complete(StatusCodes.InternalServerError, "Не удалось сделать запрос!")
            }
          }
        }
      } ~
      get {
        onComplete(petitionRepo.getAll) {
          case Success(result) =>
            val petitionList = result.toList
            complete(StatusCodes.OK, petitionList)
          case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
        }
      } ~
      post {
        entity(as[PetitionCreateRequest]) {
          newPetition => {
            val createdPetition = petitionRepo.fromCreateRequest(newPetition)
            val future = validateCustom(
              userRepo.doesUserExist(createdPetition.autorId)
                -> "Нет пользователья под указанной ID",
              userRepo.checkUserStatus(createdPetition.autorId)
                -> "Этот пользователь не можеть создавать петиций!"
            )
            onComplete(future){
              case Success(true,_) =>
                onComplete(petitionRepo.create(createdPetition)) {
                  case Success(newPetitionId) =>
                    complete(StatusCodes.Created, s"ID новой петиций ${newPetitionId.get.toString}")
                  case Failure(_) => complete(StatusCodes.InternalServerError, "Не удалось создать петицию!")
                }
              case Success(false, message) => complete(StatusCodes.BadRequest,message)
              case Failure(_) => complete(StatusCodes.InternalServerError, "Проверка не выполнилось!")
            }
          }
        }
      }
    } ~
    path(IntNumber) { petitionId =>
      get {
        onComplete(petitionRepo.getById( petitionId )) {
          case Success(petition) => complete(StatusCodes.OK, petition)
          case Success(None) => complete(StatusCodes.BadRequest,"Не правильный ID петиций!")
          case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
        }
      } ~
      put {
        entity(as[PetitionUpdateRequest]) {
          updatedPetition => {
            val updatedPetitionWithId = petitionRepo.petitionForUpdate(petitionId, updatedPetition)
            val future = validateCustom(
              userRepo.doesUserExist(updatedPetitionWithId.autorId)->"Неправильный ID автора!")
            onComplete(future) {
              case Success(true,_)=>
                onComplete(petitionRepo.update(petitionId, updatedPetitionWithId)) {
                  case Success(updatedPetitionId) => complete(StatusCodes.OK, updatedPetitionId.toString)
                  case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
                }
              case Success(false,message) => complete(StatusCodes.BadRequest,message)
              case Failure(_) => complete(StatusCodes.InternalServerError, "Проверка не выполнилось!")
            }
          }
        }
      } ~
      delete {
        onComplete(petitionRepo.delete(petitionId)) {
          case Success(deletedPetitionId) =>
            complete(StatusCodes.OK, s"Число удаленных строк ${deletedPetitionId.toString}")
          case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
        }
      }
    }
  }
}
