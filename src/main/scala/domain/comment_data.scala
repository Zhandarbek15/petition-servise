package domain

import slick.jdbc.MySQLProfile.api.*
import spray.json.{JsString, JsValue, RootJsonFormat}

import java.sql.Date
import java.time.LocalDate
// Для создания и обнавления создана другие классы в конце файла
case class Comment(
                    id: Option[Int] = None,
                    authorId: Option[Int],
                    petitionId: Option[Int],
                    text: String,
                    dateOfCreate: Date = Date.valueOf(LocalDate.now())
                  )

class Comments(tag: Tag) extends Table[Comment](tag, "comment") {
  def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
  def authorId = column[Option[Int]]("autor_id")
  def petitionId = column[Option[Int]]("petition_id")
  def text = column[String]("text")
  def dateOfCreate = column[Date]("date_of_create",O.Default(Date.valueOf(LocalDate.now())))

  def author = foreignKey("fk_author", authorId, TableQuery[UserTable])(_.id)
  def petition = foreignKey("fk_petition", petitionId, TableQuery[Petitions])(_.id)

  def * = (id, authorId, petitionId, text, dateOfCreate).mapTo[Comment]
}

implicit object SqlDateFormat extends RootJsonFormat[Date] {
  def write(date: Date): JsString = JsString(date.toString)

  def read(json: JsValue): Date = json match {
    case JsString(s) => Date.valueOf(s)
    case _ => throw new IllegalArgumentException("Invalid Date format")
  }
}

implicit val dateColumnType: BaseColumnType[Date] =
  MappedColumnType.base[Date, Long](
    date => date.getTime,
    millis => new Date(millis)
  )


case class CommentCreateRequest(
                    id: Option[Int] = None,
                    authorId: Option[Int],
                    petitionId: Option[Int],
                    text: String,
                    dateOfCreate:Option[Date] =Some(Date.valueOf(LocalDate.now()))
                  )

case class CommentUpdateRequest(
                     id: Option[Int] = None,
                     authorId: Option[Int] = None,
                     petitionId: Option[Int] = None,
                     text: Option[String] = None,
                     dateOfCreate:Option[Date] = None
                   )
