package domain

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import slick.jdbc.MySQLProfile.api.*
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import slick.jdbc.*
import spray.json.DefaultJsonProtocol.*
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

// Тут нет отдельного класса для создания потому что нет поля с значениями по умолчанию
case class User(
         id: Option[Int] = None,
         name: String,
         email: String,
         password: String,
         status: UserStatus)

class UserTable(tag: Tag) extends Table[User](tag, "user") {
  def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def email = column[String]("email")
  def password = column[String]("password")
  def status = column[UserStatus]("status")

  def * = (id, name, email, password, status).mapTo[User]
}


sealed trait UserStatus
object UserStatus {
  case object Blocked extends UserStatus
  case object Unblocked extends UserStatus


  implicit object UserStatusFormat extends RootJsonFormat[UserStatus] {
    def write(obj: UserStatus): JsString = JsString(obj.toString)

    def read(json: JsValue): UserStatus = json match {
      case JsString("Blocked") => Blocked
      case JsString("Unblocked") => Unblocked
      case _ => throw new IllegalArgumentException("Unknown UserStatus")
    }
  }

  implicit val roleColumnType: BaseColumnType[UserStatus] =
    MappedColumnType.base[UserStatus, String](
      r => r.toString,
      s => s match {
        case "Blocked" => Blocked
        case "Unblocked" => Unblocked
        case _ => throw new IllegalArgumentException(s"Unknown status: $s")
      }
    )
}

case class UserUpdateRequest(
                              id: Option[Int] = None,
                              name: Option[String] = None,
                              email: Option[String] = None,
                              password: Option[String] = None,
                              status: Option[UserStatus] = None)



