package repositories

import slick.jdbc.{GetResult, JdbcProfile}
import slick.lifted.TableQuery
import domain.{PetitionVoting, PetitionVotingCreateRequest, PetitionVotingUpdateRequest, PetitionVotings}

import java.sql.Date
import java.time.LocalDate
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

class PetitionVotingRepository(implicit db: JdbcProfile#Backend#Database ,val ec: ExecutionContext) {

  import slick.jdbc.MySQLProfile.api._

  private val petitionVotings = TableQuery[PetitionVotings]


  def create(petitionVoting: PetitionVoting):Future[Option[Int]] = {
    val insertAction = (petitionVotings returning petitionVotings.map(_.id)) += petitionVoting
    db.run(insertAction)
  }

  def fromCreateRequest(v: PetitionVotingCreateRequest): PetitionVoting = {
    PetitionVoting(
      id = v.id,
      userId = v.userId,
      petitionId = v.petitionId,
      dateVoting = v.dateVoting.getOrElse(Date.valueOf(LocalDate.now()))
    )
  }


  def getById(id: Int): Future[Option[PetitionVoting]] = {
    val action = petitionVotings.filter(_.id === id).result.headOption
    db.run(action)
  }

  def getByPetitionId(petitionId: Int): Future[Seq[PetitionVoting]] = {
    val action = petitionVotings.filter(_.petitionId === petitionId).result
    db.run(action)
  }

  def getByUserId(userId: Int): Future[Seq[PetitionVoting]] = {
    val action = petitionVotings.filter(_.petitionId === userId).result
    db.run(action)
  }

  def getAll: Future[Seq[PetitionVoting]] = {
    val action = petitionVotings.result
    db.run(action)
  }

  def doesPetitionVotingExist(desiredId:Option[Int]): Future[Boolean] = {
    val action = petitionVotings.filter(_.id === desiredId).exists.result
    db.run(action)
  }

  def customFilter(field: String, parameter: String): Future[Seq[PetitionVoting]] = {
    val sqlQuery = sql"""SELECT * FROM petition_voting WHERE #$field = $parameter"""
    db.run(sqlQuery.as[PetitionVoting])
  }

  def checkForRevoting(newVote: PetitionVoting): Future[Boolean] = {
    val checkAction = petitionVotings
      .filter(v => v.petitionId === newVote.petitionId && v.userId === newVote.userId)
      .exists.result
    db.run(checkAction).map(!_) // Инвертируем результат перед возвратом
    // Проверяет есть ли голос пользовалетя на петицию. Если есть то возвращает False, в обратном случае True
  }


  def petitionVotingForUpdate(id:Int,p:PetitionVoting): PetitionVoting =
    PetitionVoting(Some(id),p.petitionId,p.userId,p.dateVoting)

  def petitionVotingForUpdate(id: Int, c: PetitionVotingUpdateRequest):Option[PetitionVoting] = {
    val oldVote = Await.result(getById(id), 2.second)
    oldVote match
      case Some(oldVote)=>
        Some( 
          PetitionVoting(
            id = Some(id),
            userId = c.userId.orElse(oldVote.userId.collect { case id: Int => id }),
            petitionId = c.petitionId.orElse(oldVote.petitionId.collect { case id: Int => id }),
            dateVoting = c.dateVoting.getOrElse(oldVote.dateVoting)
          )
        )
      case None => None

  }

  def update(id: Int, updatedPetitionVoting: PetitionVoting): Future[Int] = {
    val updateAction = petitionVotings.filter(_.id === id).update(updatedPetitionVoting)
    db.run(updateAction)
  }

  def delete(id: Int): Future[Int] = {
    val deleteAction = petitionVotings.filter(_.id === id).delete
    db.run(deleteAction)
  }

  implicit val getPetitionVotingResult: GetResult[PetitionVoting] = GetResult(r =>
    PetitionVoting(r.nextIntOption(),
      r.nextIntOption(),
      r.nextIntOption(),
      r.nextDate(),
    )
  )

}
