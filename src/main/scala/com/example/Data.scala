package com.example

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated }
import akka.stream.actor.{ ActorPublisher, ActorPublisherMessage }
import sangria.relay.{ Node, Edge }
import scala.concurrent.ExecutionContext.Implicits.global
import akka.agent.Agent
import scala.collection.SortedMap

class Client[T] extends Actor with ActorPublisher[T] with ActorLogging {
  import ActorPublisherMessage._
  def receive = {
    case Request(_) =>
    case Cancel => context.stop(self)
    case m: T => if (totalDemand > 0) onNext(m)
  }
}

object ChatData {
  case class Chat(id: String, name: String, messageIds: Seq[String]) extends Node
  case class Message(id: String, createdAt: Long, content: String) extends Node
  trait Event
  case class MessageAdded(message: Message, chatId: String) extends Event
  case class OtherEvent(chatId: String) extends Event
  case class Register(client: ActorRef)

  object Chats {
    var chats = Map[String, Chat]("1" -> Chat("1", "Simple chat", List()))
  }

  object Messages {
    var messages = SortedMap[Int, Message]()
    val lastId = Agent(-1)
  }

  class MessageRepo(system: ActorSystem) {
    import Messages._
    import Chats._
    val distributor = system.actorOf(Props(new Actor with ActorLogging {
      var clients = Set[ActorRef]()
      def receive = {
        case Register(client) =>
          context.watch(client)
          clients = clients + client
          /*
        case m: MessageAdded =>
          log.info(s"spreading message to ${clients.size} clients")
          clients.foreach { _ ! m }
          */
        case e: Event =>
          log.info(s"spreading Event to ${clients.size} clients")
          clients.foreach { _ ! e }
        case Terminated(s) => clients = clients - s
      }
    }))
    def getMessage(id: String) = messages.get(id.toInt)
    def getChat(id: String) = chats.get(id)
    def addMessage(chatId: String, content: String) = getChat(chatId) match {
      case Some(chat) => lastId.alter(_ + 1)
        .map { iId =>
          val m = Message(iId.toString, System.currentTimeMillis, content)
          messages = messages + (iId -> m)
          chats = chats + (chatId -> chat.copy(messageIds = (chat.messageIds :+ iId.toString)))
          distributor ! MessageAdded(m, chatId)
          m
        }
      case None => throw new Exception(s"no chat with id: $chatId")
    }
    def getChatMessages(
      id: String,
      before: Option[Int] = None,
      after: Option[Int] = None,
      first: Option[Int] = None,
      last: Option[Int] = None) = {
        import sangria.relay._
        val size = messages.size
        println(s"after: ${after}, before: ${before}")
        val _after = after.getOrElse(0)
        val _before = before.getOrElse(size)
        println(s"_after: ${_after}, _before: ${_before}")
        val (from, until) = (first, last) match {
          case (Some(f), _) => (_after, math.min(f + _after, _before))
          case (_, Some(l)) => (math.max(_before - l, _after), _before)
        }
        println(s"from: ${from}, until: ${until}")
        val res = messages.slice(from, until)
        DefaultConnection(PageInfo(
            startCursor = res.headOption.map { case (i, _) => Connection.offsetToCursor(i) },
            endCursor = res.lastOption.map { case (i, _) => Connection.offsetToCursor(i) },
            hasPreviousPage = from > 0,
            hasNextPage = until < messages.size),
          res.map { case (i, m) => Edge(m, Connection.offsetToCursor(i)) }.toSeq)
    }
    def getPublisher = {
      val client = system.actorOf(Props(new Client[Event]))
      distributor ! Register(client)
      distributor ! OtherEvent("1")
      ActorPublisher[Event](client)
    }
  }

}
