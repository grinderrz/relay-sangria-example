package com.example

import sangria.relay.{ Node, Edge }
import scala.concurrent.ExecutionContext.Implicits.global
import akka.agent.Agent
import scala.collection.SortedMap

object ChatData {
  case class Chat(id: String, name: String) extends Node
  case class Message(id: String, createdAt: Long, content: String) extends Node
  trait Event
  case class MessageAdded(message: Message) extends Event

  object Chats {
    val chats = Map[String, Chat]("1" -> Chat("1", "Simple chat"))
  }

  object Messages {
    var messages = SortedMap[Int, Message](
      (1 -> Message("1", System.currentTimeMillis, "message 1")),
      (2 -> Message("2", System.currentTimeMillis, "message 2")),
      (3 -> Message("3", System.currentTimeMillis, "message 3")),
      (4 -> Message("4", System.currentTimeMillis, "message 4")),
      (5 -> Message("5", System.currentTimeMillis, "message 5")))
    val lastId = Agent(5)
  }

  class MessageRepo {
    import Messages._
    def getMessage(id: String) = messages.get(id.toInt)
    def getChat(id: String) = Chats.chats.get(id)
    def addMessage(chatId: String, content: String) = {
      lastId.alter(_ + 1)
        .map { iId =>
          val m = Message(iId.toString, System.currentTimeMillis, content)
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
        val _after = after.getOrElse(0)
        val _before = before.getOrElse(size)
        //println(s"_before: ${_before}")
        //println(s"_after: ${_after}")
        //println(s"first: ${first}, last: ${last}")
        val (from, until) = (first, last) match {
          case (Some(f), _) => (_after, math.min(f + _after, _before))
          case (_, Some(l)) => (math.max(size - l, _after), _before)
        }
        //println(s"from: ${from}, until: ${until}")
        val res = messages.slice(from, until)
        DefaultConnection(PageInfo(
            startCursor = res.headOption.map { case (i, _) => Connection.offsetToCursor(i) },
            endCursor = res.lastOption.map { case (i, _) => Connection.offsetToCursor(i) },
            hasPreviousPage = from > 0,
            hasNextPage = until < messages.size),
          res.map { case (i, m) => Edge(m, Connection.offsetToCursor(i)) }.toSeq)
    }
  }

}
