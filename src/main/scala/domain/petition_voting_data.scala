package domain

import slick.jdbc.MySQLProfile.api.*
import slick.lifted.ProvenShape
import spray.json.{JsString, JsValue, RootJsonFormat}

import java.sql.Date
import java.time.LocalDate
import scala.annotation.targetName
// Для создания и обнавления создана другие классы в конце файла
case class PetitionVoting(
                           id: Option[Int] = None,
                           petitionId: Option[Int],
                           userId: Option[Int],
                           dateVoting: Date = Date.valueOf(LocalDate.now())
                         )

class PetitionVotings(tag: Tag) extends Table[PetitionVoting](tag, "petition_voting") {
  def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
  def petitionId = column[Option[Int]]("petition_id")
  def userId = column[Option[Int]]("user_id")
  def dateVoting = column[Date]("date_voting",O.Default(Date.valueOf(LocalDate.now())))

  def petition = foreignKey("fk_petition",petitionId,TableQuery[Petitions])(_.id)

  def * = (id, petitionId, userId, dateVoting).mapTo[PetitionVoting]
}

case class PetitionVotingCreateRequest(
                               id: Option[Int] = None,
                               petitionId: Option[Int],
                               userId: Option[Int],
                               dateVoting:Option[Date] =Some(Date.valueOf(LocalDate.now()))
                               )

case class PetitionVotingUpdateRequest(
                                id: Option[Int] = None,
                                petitionId: Option[Int] = None,
                                userId: Option[Int] = None,
                                dateVoting: Option[Date] = None
                               )

