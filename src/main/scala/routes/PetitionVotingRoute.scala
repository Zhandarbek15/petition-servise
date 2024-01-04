package routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import domain.*
import repositories.{JsonSupport, PetitionRepository, PetitionVotingRepository, UserRepository}

import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

class PetitionVotingRoute(implicit
                          val petitionVotingRepo: PetitionVotingRepository,
                          val petitionRepo: PetitionRepository,
                          val userRepo: UserRepository,
                          val ex:ExecutionContext)
extends JsonSupport {
  // field параметрінің дұрыстығын тексеру үшін
  private val fields: List[String] = List(
    "id",
    "petition_id",
    "user_id",
    "date_voting"
  )

  val route = pathPrefix("petitionVotings") {
    pathEndOrSingleSlash {
      (get & parameters("field", "parameter")) {
        (field, parameter) => {
          if (!fields.contains(field)) {
            complete(StatusCodes.BadRequest,
              s"Вы ввели неправильный имя поля таблицы! Допустимые поля: ${fields.mkString(", ")}")
          }
          else{
            onComplete(petitionVotingRepo.customFilter(field, parameter)) {
              case Success(queryResponse) => complete(StatusCodes.OK, queryResponse.toList)
              case Failure(_) => complete(StatusCodes.InternalServerError, "Не удалось сделать запрос!")
            }
          }
        }
      } ~
      get {
        onComplete(petitionVotingRepo.getAll) {
          case Success(result) =>
            val petitionVotingList = result.toList
            complete(StatusCodes.OK, petitionVotingList)
          case Failure(ex) => complete(StatusCodes.NotFound, s"${ex.getMessage}")
        }
      } ~
      post {
        entity(as[PetitionVotingCreateRequest]) {
          newPetitionVoting => {
            val newValidPetitionVoting = petitionVotingRepo.fromCreateRequest(newPetitionVoting)
            val validation = validateCustom(
              // Проверка на повторное голосование
              petitionVotingRepo.checkForRevoting(newValidPetitionVoting)
                -> "Повторное голосование запрещено!",
              // Проверка на правильность ID пользователя и петиций
              userRepo.doesUserExist(newValidPetitionVoting.userId)
                -> "Не правильно указали ID пользователья",
              petitionRepo.doesPetitionExist(newValidPetitionVoting.petitionId)
                -> "Не правильно указали ID петиций",
              // Проверка статуса пользователя
              userRepo.checkUserStatus(newValidPetitionVoting.userId)
                -> s"Этот пользователь под ID ${newValidPetitionVoting.userId} не может менять голос!",
              petitionRepo.checkPetitionStatus(newValidPetitionVoting.petitionId)
                -> "На эту петицию нельзя голосовать!"
            )
             onComplete(validation) {
               case Success(true,_)=>
                 onComplete(petitionVotingRepo.create(newValidPetitionVoting)) {
                  case Success(newCommentId) =>
                    complete(StatusCodes.Created, s"ID нового голоса ${newCommentId.get.toString}")
                  case Failure(_) => complete(StatusCodes.InternalServerError, "Не удалось создать голос!")
                 }
               case Success(false, message) => complete(StatusCodes.BadRequest, message)
               case Failure(_) => complete(StatusCodes.InternalServerError, "Проверка не выполнилось!")
            }
          }
        }
      }
    } ~
    path(IntNumber) { petitionVotingId =>
      get {
        onComplete(petitionVotingRepo.getById(petitionVotingId)) {
          case Success(Some(petitionVoting)) => complete(StatusCodes.OK, petitionVoting)
          case Success(None) => complete(StatusCodes.BadRequest, s"Неправильный айди для чтения $petitionVotingId!")
          case Failure(ex) => complete(StatusCodes.NotFound, s"${ex.getMessage}")
        }
      } ~
      put {
        entity(as[PetitionVotingUpdateRequest]) {
          updatedPetitionVoting => {
            val validPetitionVoting =
              petitionVotingRepo.petitionVotingForUpdate(petitionVotingId, updatedPetitionVoting)
            validPetitionVoting match {
              case Some(validPetitionVoting) =>
                // Проверка на правильность ID пользователя и петиций
                val validation = validateCustom(
                  userRepo.doesUserExist(validPetitionVoting.userId)
                    -> "Не правильно указали ID пользователья!",
                  petitionRepo.doesPetitionExist(validPetitionVoting.petitionId)
                    -> "Не правильно указали ID петицию!",
                  // Проверка статуса пользователя
                  userRepo.checkUserStatus(validPetitionVoting.userId)
                    -> s"Этот пользователь под ID ${validPetitionVoting.userId} не может менять голос!",
                  // Проверка на повторное голосование
                  petitionVotingRepo.checkForRevoting(validPetitionVoting)
                    -> "Повторное голосование запрещена!",
                  petitionRepo.checkPetitionStatus(validPetitionVoting.petitionId)
                    -> s"На эту петицию под айди ${validPetitionVoting.petitionId.get} нельзя голосовать!",
                  petitionRepo.checkOldPetitionStatus(Some(petitionVotingId))
                    -> s"Из петиций под айди $petitionVotingId нельзя убрать голос!"
                )
                onComplete(validation) {
                  case Success(true, _) =>
                    onComplete(petitionVotingRepo.update(petitionVotingId, validPetitionVoting)) {
                      case Success(updatedPetitionVotingId) => complete(StatusCodes.OK, updatedPetitionVotingId.toString)
                      case Failure(ex) => complete(StatusCodes.NotFound, s"${ex.getMessage}")
                    }
                  case Success(false, message) => complete(StatusCodes.BadRequest, message)
                  case Failure(_) => complete(StatusCodes.InternalServerError, "Проверка не выполнилось!")
                }
              case None => complete(StatusCodes.BadRequest, s"Неправильный айди для изменения $petitionVotingId!")
            }
          }
        }
      } ~
      delete {
        val validation = validateCustom(
          petitionRepo.checkOldPetitionStatus(Some(petitionVotingId))
          -> s"Голос под айди $petitionVotingId нельзя убрать из-за статуса петиций!"
        )
        onComplete(validation){
          case Success(true,_)=>
            onComplete(petitionVotingRepo.delete(petitionVotingId)) {
              case Success(deletedPetitionVotingId) =>
                complete(StatusCodes.OK, s"Число удаленных строк ${deletedPetitionVotingId.toString}")
              case Failure(ex) => complete(StatusCodes.NotFound, s"${ex.getMessage}")
            }
          case Success(false, message) => complete(StatusCodes.BadRequest, message)
          case Failure(_) => complete(StatusCodes.InternalServerError, "Проверка не выполнилось!")
        }
      }
    }
  }
}
