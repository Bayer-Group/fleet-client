package com.monsanto.arch.fleet

import akka.actor.ActorSystem
import com.monsanto.arch.fleet.FleetPipelines.Groupinator
import spray.client.pipelining._
import spray.http._
import spray.httpx.unmarshalling._
import spray.json.{JsonFormat, JsString, JsonParser}
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Standard spray pipelines for fleet client
 */
trait FleetPipelines {
  val config: FleetConfig
  implicit val system: ActorSystem
  implicit def ec: ExecutionContextExecutor = system.dispatcher

  /**
   * A pipeline where no response is required
   * @return Request => Future of status code
   */
  def noResponsePipeline: HttpRequest => Future[Int] =
    addHeaders(config.headers) ~> sendReceive ~> checkStatus ~> toStatusCode

  /**
   * A pipeline for a response returning anything with typeclass
   * spray.httpx.FromResponseUnmarshaller defined.
   * @tparam T type to unmarshall (requires FromResponseUnmarshaller type class)
   * @return Request => Future of T
   */
  def pipeline[T : FromResponseUnmarshaller]: HttpRequest => Future[T] =
    addHeaders(config.headers) ~> sendReceive ~> unmarshal[T]

  /**
   * A pipeline for returning multiple Ts where T has all of FromResponseUnmarshaller,
   * JsonFormat and Groupinator type classes defined. Allows paginated accumulation
   * of Ts by caller.
   *
   * Example:
   * {{{
   * paginatedPipeline[StateResponse].apply(Get(config.URLs.state).map(_.stateSeq)
   * }}}
   * @tparam T generic type with FromResponseUnmarshaller, JsonFormat and Groupinator
   *           type classes supplied
   * @return Request => Future of T
   */
  def paginatedPipeline[T : FromResponseUnmarshaller : JsonFormat : Groupinator]: HttpRequest => Future[T] = { request =>
    val firstPass = paginatedPipelineHelper.apply(request)
    firstPass.flatMap { x => accumulateResponse(request, x._1, x._2) }
  }


  private def checkStatus: HttpResponse => HttpResponse =
    response =>
      if (response.status.isSuccess)
        response
      else throw OperationFailedException(response)

  private def toStatusCode: HttpResponse => Int =
    response => response.status.intValue

  private def checkForNext(response: HttpResponse): Option[String] = {
    val bodyAsJson = response.entity.toOption.map(x => JsonParser(x.asString(defaultCharset = HttpCharsets.`UTF-8`)))
    bodyAsJson.flatMap(x=>x.asJsObject.getFields("nextPageToken").headOption match {
      case Some(JsString(token)) => Some(token)
      case _ => None
    })
  }

  private def unmarshalWithNextToken[T: FromResponseUnmarshaller]: HttpResponse ⇒ (T, Option[String]) =
    response => (response ~> unmarshal[T], checkForNext(response))

  private def paginatedPipelineHelper[T: FromResponseUnmarshaller : JsonFormat]: HttpRequest => Future[(T, Option[String])] =
    addHeaders(config.headers) ~> sendReceive ~> unmarshalWithNextToken[T]

  private def addNextPageToken(req:HttpRequest, token:String): HttpRequest = {
    val query = ("nextPageToken",token) +: req.uri.query
    req.copy(uri= req.uri.copy(query = query))
  }

  private def accumulateResponse[T : FromResponseUnmarshaller : JsonFormat : Groupinator](request: HttpRequest, accumulated:T, next:Option[String]): Future[T] = {
    if(next.isEmpty) {
      Future.successful(accumulated)
    } else {
      val gr = implicitly[Groupinator[T]]
      val nextPage = paginatedPipelineHelper.apply( addNextPageToken(request, next.get) )
      nextPage.flatMap { result =>
        accumulateResponse(request, gr.add(accumulated, result._1), result._2)
      }
    }
  }
}

object FleetPipelines {
  /**
   * Type class to provide grouping of types T in a response
   */
  trait Groupinator[T] {
    def add(a:T,b:T) :T
  }
}
