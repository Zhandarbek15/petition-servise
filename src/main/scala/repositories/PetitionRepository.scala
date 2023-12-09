package repositories

import akka.http.scaladsl.server.Directives.onComplete
import domain.PetitionStatus.{Active, Passive}
import slick.jdbc.{GetResult, JdbcProfile}
import slick.lifted.TableQuery
import domain.{Petition, PetitionCreateRequest, PetitionStatus, PetitionUpdateRequest, PetitionVoting, Petitions}

import java.sql.Date
import java.time.LocalDate
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*
import scala.util.{Failure, Success}

class PetitionRepository(implicit db: JdbcProfile#Backend#Database ,
                         val petitionVotingRepo: PetitionVotingRepository,
                         val ec: ExecutionContext) {

  import slick.jdbc.MySQLProfile.api._

  private val petitions = TableQuery[Petitions]

  def create(petition: Petition): Future[Option[Int]] = {
    val insertAction = (petitions returning petitions.map(_.id)) += petition
    db.run(insertAction)
  }

  def getById(id: Int): Future[Option[Petition]] = {
    val action = petitions.filter(_.id === id).result.headOption
    db.run(action)
  }

  def getAll: Future[Seq[Petition]] = {
    val action = petitions.result
    db.run(action)
  }

  def doesPetitionExist(desiredId: Option[Int]):Future[Boolean] ={
    val action = petitions.filter(_.id === desiredId).exists.result
    db.run(action)
  }

  def customFilter(field: String, parameter: String): Future[Seq[Petition]] = {
    val sqlQuery = sql"""SELECT * FROM petition WHERE #$field = $parameter"""
    db.run(sqlQuery.as[Petition])
  }

  def checkPetitionStatus(idForCheck: Option[Int]): Future[Boolean] = {
    val action = petitions.filter(_.id === idForCheck).map(_.status).result.headOption
    db.run(action).map {
      case Some(Passive) => false
      case Some(Active) => true
      case None => true
    }
  }

  def checkOldPetitionStatus(voteId: Option[Int]): Future[Boolean] = {
    voteId.fold(Future.successful(true)) { id =>
      val futurePetitionVote = petitionVotingRepo.getById(id)

      futurePetitionVote.flatMap {
        case Some(petitionVote: PetitionVoting) =>
          val innerAction = petitions.filter(_.id === petitionVote.petitionId).map(_.status).result.headOption
          db.run(innerAction).map {
            case Some(Passive) => false
            case Some(Active) => true
            case _ => true
          }
        case None => Future.successful(true)
      }.recover {
        case _ => true
      }
    }
  }


  def fromCreateRequest(createRequest: PetitionCreateRequest): Petition = {
    Petition(
      id = createRequest.id,
      autorId = createRequest.autorId,
      name = createRequest.name,
      description = createRequest.description,
      dateCreate = createRequest.dateCreate.getOrElse(Date.valueOf(LocalDate.now())),
      goalOfVotes = createRequest.goalOfVotes,
      votes = createRequest.votes.getOrElse(0),
      status = createRequest.status
    )
  }

  def petitionForUpdate(id: Int, updateRequest: PetitionUpdateRequest): Petition = {
    val oldPetition = Await.result(getById(id), 2.second).get
    Petition(
      id = Some(id),
      autorId = updateRequest.autorId.orElse(oldPetition.autorId.collect { case id: Int => id }),
      name = updateRequest.name.getOrElse(oldPetition.name),
      description = updateRequest.description.getOrElse(oldPetition.description),
      dateCreate = updateRequest.dateCreate.getOrElse(oldPetition.dateCreate),
      goalOfVotes = updateRequest.goalOfVotes.getOrElse(oldPetition.goalOfVotes),
      votes = updateRequest.votes.getOrElse(oldPetition.votes),
      status = updateRequest.status.getOrElse(oldPetition.status)
    )
  }

  def update(id: Int, updatedPetition: Petition):Future[Int] = {
    val updateAction = petitions.filter(_.id === id).update(updatedPetition)
    db.run(updateAction)
  }

  def delete(id: Int): Future[Int] = {
    val deleteAction = petitions.filter(_.id === id).delete
    db.run(deleteAction)
  }

  implicit val getPetitionResult: GetResult[Petition] = GetResult(r =>
    Petition(r.nextIntOption(),
      r.nextIntOption(),
      r.nextString(),
      r.nextString(),
      r.nextDate(),
      r.nextInt(),
      r.nextInt(),
      if (r.nextString() == "Active") PetitionStatus.Active else PetitionStatus.Passive
    )
  )
}
