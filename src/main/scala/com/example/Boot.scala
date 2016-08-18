package com.example

import akka.actor.{ ActorSystem, Props, ActorRef, Actor, ActorLogging }
import akka.stream.actor.{ ActorPublisher, ActorPublisherMessage }
import akka.stream.{ Materializer, ActorMaterializer }
import akka.stream.scaladsl.Source
import akka.http.scaladsl.Http
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes.{ InternalServerError, OK, BadRequest }
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import sangria.ast.OperationType
import sangria.parser.QueryParser
import scala.util.{ Success, Failure }
import sangria.execution.{ ErrorWithResolver, QueryAnalysisError, Executor, HandledException, PreparedQuery }
import sangria.marshalling.sprayJson._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import de.heikoseeberger.akkasse._
import de.heikoseeberger.akkasse.EventStreamMarshalling._
import scala.util.control.NonFatal
import scala.concurrent.duration._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import DefaultJsonProtocol._ 

object Boot extends App {
  private val config = ConfigFactory.load()
  implicit val system = ActorSystem("Example", config)
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  val logger = Logging(system, getClass)

  val messageRepo = new ChatData.MessageRepo(system)

  def eventStream(preparedQuery: PreparedQuery[ChatData.MessageRepo, Any, JsObject], messageRepo: ChatData.MessageRepo): Source[ServerSentEvent, Any] = {
    Source.fromPublisher(messageRepo.getPublisher)
      .map { m => sangria.relay.Edge(m, sangria.relay.Connection.offsetToCursor(m.id.toInt)) }
      .map { e => preparedQuery.execute(root = e).map { r => ServerSentEvent(r.compactPrint) } }
      .mapAsync(1)(identity)
      .recover {
        case NonFatal(error) =>
          logger.error(error, "Unexpected error during event stream processing.")
            ServerSentEvent(error.getMessage)
      }
  }

  val route =
    (post & path("graphql")) {
      entity(as[JsValue]) { requestJson =>
        val JsObject(fields) = requestJson
        val JsString(query) = fields("query")
        val operation = fields.get("operationName") collect {
          case JsString(op) => op
        }
        val vars = fields.get("variables") match {
          case Some(obj: JsObject) => obj
          //case Some(JsString(s)) if s.trim.nonEmpty => s.parseJson
          case _ => JsObject.empty
        }
        QueryParser.parse(query) match {
          case Success(queryAst) =>
            complete(
              Executor.execute(
                ChatSchema.schema,
                queryAst,
                messageRepo,
                variables = vars,
                operationName = operation)
              .map(OK -> _)
              .recover {
                case error: QueryAnalysisError => BadRequest -> error.resolveError
                case error: ErrorWithResolver => InternalServerError -> error.resolveError
              })
          case Failure(error) =>
            complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
        }
      }
    } ~
    (get & path("graphql")) {
      parameters('query, 'operation.?, 'variables.?) { (query, operation, variables) =>
        logger.info(s"subscription with query: $query, operation: $operation, variables: $variables")
        val vars = variables match {
          case Some(varStr) => varStr.parseJson match {
            case obj: JsObject => obj
            case _ => JsObject.empty
          }
          //case Some(JsString(s)) if s.trim.nonEmpty => s.parseJson
          case _ => JsObject.empty
        }
        QueryParser.parse(query) match {
          case Success(queryAst) =>
            complete(
              Executor.prepare(
                ChatSchema.schema,
                queryAst,
                messageRepo,
                operationName = operation,
                variables = vars)
              .map { pq => ToResponseMarshallable(eventStream(pq, messageRepo)) }
              .recover {
                case error: QueryAnalysisError =>
                  ToResponseMarshallable(BadRequest → error.resolveError)
                case error: ErrorWithResolver =>
                  ToResponseMarshallable(InternalServerError → error.resolveError)
                case error =>
                  ToResponseMarshallable(BadRequest -> error.getMessage)
              })
          case Failure(error) =>
            complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
        }
      }
    } ~
    (get & path("app")) {
      getFromFile("src/main/js/public/index.html")
    } ~
    (get & path("subTest")) {
      getFromFile("src/main/js/public/subTest.html")
    }

  val port = config.getInt("boot.port")
  val interface = config.getString("boot.interface")

  val bindingFuture = Http().bindAndHandle(
    route,
    interface,
    port)
  while(scala.io.StdIn.readLine() != null)
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
