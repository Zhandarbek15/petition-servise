package routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import domain.*
import repositories.{JsonSupport, UserRepository}

import scala.util.{Failure, Success}

class UserRoute(implicit userRepo:UserRepository)
          extends JsonSupport {

  // field параметрінің дұрыстығын тексеру үшін
  private val fields:List[String] = List(
    "id",
    "name",
    "email",
    "password",
    "status"
  )

  val route = pathPrefix("users") {
    pathEndOrSingleSlash {
      (get & parameters("field", "parameter")) {
        (field, parameter) => {
          if (!fields.contains(field)) {
            complete(StatusCodes.BadRequest,
              s"Вы ввели неправильный имя поля таблицы! Допустимые поля: ${fields.mkString(", ")}")
          }
          else {
            onComplete(userRepo.customFilter(field, parameter)) {
              case Success(queryResponse) => complete(StatusCodes.OK, queryResponse.toList)
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, s"Не удалось сделать запрос! ${ex.getMessage}")
            }
          }
        }
      } ~
      get {
        onComplete(userRepo.getAll) {
          case Success(result) =>
            val userList = result.toList
            complete(StatusCodes.OK, userList)
          case Failure(ex) => complete(StatusCodes.InternalServerError, s"Ошибка в коде: ${ex.getMessage}")
        }
      } ~
      post {
        entity(as[User]) {
          newUser => {
            onComplete(userRepo.create(newUser)) {
              case Success(newUserId) =>
                complete(StatusCodes.Created, s"ID нового пользователья ${newUserId.toString}")
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, s"Не удалось создать пользователя: ${ex.getMessage}")
            }
          }
        }
      }
    } ~
      path(IntNumber) { userId =>
        get {
          onComplete(userRepo.getById(userId)) {
            case Success(Some(user)) => complete(StatusCodes.OK, user)
            case Success(None) => complete(StatusCodes.NotFound, s"Пользователя под ID $userId не существует!")
            case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
          }
        } ~
          put {
            entity(as[UserUpdateRequest]) { updatedUser => {
              val updatedUserWithId = userRepo.userForUpdate(userId,updatedUser)
              onComplete(userRepo.update(userId, updatedUserWithId)) {
                case Success(updatedUserId) =>
                  complete(StatusCodes.OK, updatedUserId.toString)
                case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
              }
            }
            }
          } ~
          delete {
            onComplete(userRepo.delete(userId)) {
              case Success(deletedUserId) =>
                complete(StatusCodes.OK, s"Число удаленных строк ${deletedUserId.toString}")
              case Failure(ex) => complete(StatusCodes.NotFound, s"Ошибка в коде: ${ex.getMessage}")
            }
          }
      }
  }
}
