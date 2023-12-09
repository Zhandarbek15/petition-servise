package repositories

import slick.jdbc.{GetResult, JdbcProfile}
import slick.lifted.TableQuery
import domain.{Comment, CommentCreateRequest, CommentUpdateRequest, Comments}

import java.sql.Date
import java.time.LocalDate
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class CommentRepository(implicit db: JdbcProfile#Backend#Database,val ec: ExecutionContext) {

  import slick.jdbc.MySQLProfile.api._

  private val comments = TableQuery[Comments]
  

  def create(comment: Comment): Future[Option[Int]] = {
    val insertAction = (comments returning comments.map(_.id)) += comment
    db.run(insertAction)
  }

  def fromCreateRequest(c: CommentCreateRequest): Comment = {
    Comment(
      id = c.id,
      authorId = c.authorId,
      petitionId = c.petitionId,
      text = c.text,
      dateOfCreate = c.dateOfCreate.getOrElse(Date.valueOf(LocalDate.now()))
    )
  }



  def getById(id: Int): Future[Option[Comment]] = {
    val action = comments.filter(_.id === id).result.headOption
    db.run(action)
  }

  def getByPetitionId(petitionId: Int): Future[Seq[Comment]] = {
    val action = comments.filter(_.petitionId === petitionId).result
    db.run(action)
  }

  def getByUserId(userId: Int):Future[Seq[Comment]] = {
    val action = comments.filter(_.petitionId === userId).result
    db.run(action)
  }

  def doesCommentExist(desiredId: Int): Future[Boolean] = {
    val action = comments.filter(_.id === desiredId).exists.result
    db.run(action)
  }

  def customFilter(field: String, parameter: String): Future[Seq[Comment]] = {
    val sqlQuery = sql"""SELECT * FROM comment WHERE #$field = $parameter"""
    db.run(sqlQuery.as[Comment])
  }


  def getAll: Future[Seq[Comment]] = {
    val action = comments.result
    db.run(action)
  }

  def commentForUpdate(id:Int,c:CommentUpdateRequest):Comment = {
    val oldComment = Await.result(getById(id),2.second).get
    Comment(
      id = Some(id),
      authorId = c.authorId.orElse(oldComment.authorId.collect { case id: Int => id }),
      petitionId = c.petitionId.orElse(oldComment.petitionId.collect { case id: Int => id }),
      text = c.text.getOrElse(oldComment.text),
      dateOfCreate = c.dateOfCreate.getOrElse(oldComment.dateOfCreate)
    )
  }
  
  def update(id: Int, updatedComment: Comment):Future[Int] = {
    val updateAction = comments.filter(_.id === id).update(updatedComment)
    db.run(updateAction)
  }

  def delete(id: Int):Future[Int] = {
    val deleteAction = comments.filter(_.id === id).delete
    db.run(deleteAction)
  }

  implicit val getCommentResult: GetResult[Comment] = GetResult(r =>
    Comment(r.nextIntOption(),
      r.nextIntOption(),
      r.nextIntOption(),
      r.nextString(),
      r.nextDate()
    )
  )
}
