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
  case class Chat(id: String, name: String) extends Node
  case class Message(id: String, createdAt: Long, content: String) extends Node
  trait Event
  case class MessageAdded(message: Message) extends Event
  case class Register(client: ActorRef)

  object Chats {
    val chats = Map[String, Chat]("1" -> Chat("1", "Simple chat"))
  }

  object Messages {
    var messages = SortedMap[Int, Message]()
    val lastId = Agent(-1)
  }

  class MessageRepo(system: ActorSystem) {
    import Messages._
    val distributor = system.actorOf(Props(new Actor with ActorLogging {
      var clients = Set[ActorRef]()
      def receive = {
        case Register(client) =>
          context.watch(client)
          clients = clients + client
        case m: Message =>
          log.info(s"spreading message to ${clients.size} clients")
          clients.foreach { _ ! m }
        case Terminated(s) => clients = clients - s
      }
    }))
    def getMessage(id: String) = messages.get(id.toInt)
    def getChat(id: String) = Chats.chats.get(id)
    def addMessage(chatId: String, content: String) = {
      lastId.alter(_ + 1)
        .map { iId =>
          val m = Message(iId.toString, System.currentTimeMillis, content)
          distributor ! m
          messages = messages + (iId -> m)
          m
        }
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
      val client = system.actorOf(Props(new Client[Message]))
      distributor ! Register(client)
      ActorPublisher[Message](client)
    }
  }

}
