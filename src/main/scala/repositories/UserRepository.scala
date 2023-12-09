package repositories

import domain.UserStatus.Unblocked
import slick.jdbc.{GetResult, JdbcProfile}
import slick.lifted.TableQuery
import domain.{User, UserStatus, UserTable, UserUpdateRequest}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

class UserRepository(implicit val db: JdbcProfile#Backend#Database, val ec: ExecutionContext) {

  import slick.jdbc.MySQLProfile.api._

  private val users = TableQuery[UserTable]


  def create(user: User): Future[Option[Int]]  = {
    val insertAction = (users returning users.map(_.id)) += user
    db.run(insertAction)
  }

  def getById(id: Int): Future[Option[User]] = {
    val action = users.filter(_.id === id).result.headOption
    db.run(action)
  }

  def getAll: Future[Seq[User]] = {
    val action = users.result
    db.run(action)
  }

  def doesUserExist(desiredId: Option[Int]): Future[Boolean] = {
    val action = users.filter(_.id === desiredId.get).exists.result
    db.run(action)
  }

  def customFilter(field: String, parameter: String): Future[Seq[User]] = {
    val sqlQuery = sql"""SELECT * FROM user WHERE #$field = $parameter"""
    db.run(sqlQuery.as[User])
  }

  def checkUserStatus(idForCheck: Option[Int]): Future[Boolean] = {
    val action = users.filter(_.id === idForCheck).map(_.status).result.headOption
    val resultFuture: Future[Option[UserStatus]] = db.run(action)

    resultFuture.map {
      case Some(status) => status == Unblocked
      case None => false // Возможно, вам нужно определить поведение в случае отсутствия пользователя
    }
  }
  
  def userForUpdate(id:Int,u:UserUpdateRequest):User = {
    val oldUser = Await.result(getById(id),2.second).get
    User(
      id = Some(id),
      name = u.name.getOrElse(oldUser.name), 
      email = u.email.getOrElse(oldUser.email),
      password = u.password.getOrElse(oldUser.password),
      status = u.status.getOrElse(oldUser.status)
    )
  }

  def update(id: Int, updatedUser: User): Future[Int] = {
    val updateAction = users.filter(_.id === id).update(updatedUser)
    db.run(updateAction.transactionally)
  }

  def delete(id: Int): Future[Int] = {
    val deleteAction = users.filter(_.id === id).delete
    db.run(deleteAction)
  }

  implicit val getUserResult: GetResult[User] = GetResult(r =>
    User(r.nextIntOption(),
      r.nextString(),
      r.nextString(),
      r.nextString(),
      if (r.nextString() == "Blocked") UserStatus.Blocked else UserStatus.Unblocked
    )
  )
}
