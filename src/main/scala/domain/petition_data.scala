package domain

import domain.UserStatus.{Blocked, Unblocked}
import slick.jdbc.MySQLProfile.api.*
import spray.json.{JsString, JsValue, RootJsonFormat}

import java.sql.Date
import java.time.LocalDate


case class Petition(
                     id: Option[Int] = None,
                     autorId: Option[Int],
                     name: String,
                     description: String,
                     dateCreate: Date = Date.valueOf(LocalDate.now()),
                     goalOfVotes: Int,
                     votes: Int = 0,
                     status: PetitionStatus
                   )

class Petitions(tag: Tag) extends Table[Petition](tag, "petition") {
  def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
  def autorId = column[Option[Int]]("autor_id")
  def name = column[String]("name", O.Length(256))
  def description = column[String]("description")
  def dateCreate = column[Date]("date_create", O.Default(Date.valueOf(LocalDate.now())))
  def goalOfVotes = column[Int]("goal_of_votes")
  def votes = column[Int]("votes", O.Default(0))
  def status = column[PetitionStatus]("status")

  def user = foreignKey("fk_user", autorId, TableQuery[UserTable])(_.id)

  def * = (id, autorId, name, description, dateCreate, goalOfVotes, votes, status).mapTo[Petition]
}

sealed trait PetitionStatus
object PetitionStatus {
  case object Active extends PetitionStatus
  case object Passive extends PetitionStatus
  
  implicit object PetitionStatusFormat extends RootJsonFormat[PetitionStatus] {
    def write(obj: PetitionStatus): JsString = JsString(obj.toString)

    def read(json: JsValue): PetitionStatus = json match {
      case JsString("Active") => Active
      case JsString("Passive") => Passive
      case _ => throw new IllegalArgumentException("Unknown UserStatus")
    }
  }
  
  implicit val roleColumnType: BaseColumnType[PetitionStatus] =
    MappedColumnType.base[PetitionStatus, String](
      r => r.toString,
      s => s match {
        case "Active" => Active
        case "Passive" => Passive
        case _ => throw new IllegalArgumentException(s"Unknown status: $s")
      }
    )
}

// Доп Класс для создания петиций в которым dateCreate и votes Option типа,
// чтобы было возможность не указать если есть значение по умолчанию
// Функция который используется находиться в репозиторий (fromCreateRequest)
case class PetitionCreateRequest(
                                  id: Option[Int] = None,
                                  autorId: Option[Int],
                                  name: String,
                                  description: String,
                                  dateCreate:Option[Date] =Some(Date.valueOf(LocalDate.now())),
                                  goalOfVotes: Int,
                                  votes:Option[Int] =Some(0),
                                  status: PetitionStatus
                                )

// Доп Класс для обновлений петиций в котором все поля Option типа,
// чтобы нужно было указать точные поля, остальные поля возмуться с старой петиций
// Функция который используется находиться в репозиторий (petitionForUpdate)
case class PetitionUpdateRequest(
                                  id: Option[Int] = None,
                                  autorId: Option[Int] = None,
                                  name: Option[String] = None,
                                  description: Option[String] = None,
                                  dateCreate: Option[Date] = None,
                                  goalOfVotes: Option[Int] = None,
                                  votes: Option[Int] = None,
                                  status: Option[PetitionStatus] = None
                                )

