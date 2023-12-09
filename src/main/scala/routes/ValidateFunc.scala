package routes

import scala.concurrent.{ExecutionContext, Future}

def validateCustom(futures: (Future[Boolean], String)*)(implicit ex:ExecutionContext): Future[(Boolean, String)] = {
  val aggregatedResults = Future.sequence(futures.map(_._1))

  aggregatedResults.map { results =>
    val success = results.forall(identity)
    val errorMessages = futures.zip(results).collect {
      case ((_, errorMessage), false) => errorMessage
    }
    (success, errorMessages.mkString(",\n"))
  }
}
