package com.example

import sangria.relay.Node

object ChatData {
  case class Chat(id: String, name: String) extends Node
  case class Message(id: String, content: String) extends Node

  object Chats {
    val chats = Map[String, Chat]("1" -> Chat("1", "Simple chat"))
  }

  object Messages {
    val chatMessages = Map[String, List[String]](("1" -> List("1", "2", "3", "4", "5")))
    val messages = Map[String, Message](
      ("1" -> Message("1", "message 1")),
      ("2" -> Message("2", "message 2")),
      ("3" -> Message("3", "message 3")),
      ("4" -> Message("4", "message 4")),
      ("5" -> Message("5", "message 5")))
  }

  class MessageRepo {
    import Messages._
    def getMessage(id: String) = messages.get(id)
    def getChat(id: String) = Chats.chats.get(id)
    def getChatMessages(chatId: String) = chatMessages.get(chatId).map {
      _.map { id => getMessage(id) }
    }.fold { List[Option[Message]]() }(identity)
  }

}
