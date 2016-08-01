package com.example

import sangria.relay._
import sangria.schema._
import ChatData.{ Chat, Message, MessageRepo }

object ChatSchema {

  val NodeDefinition(nodeInterface, nodeField) = Node.definition(
    (id: GlobalId, ctx: Context[MessageRepo, Unit]) => {
      if (id.typeName == "Chat") ctx.ctx.getChat(id.id)
      else if (id.typeName == "Message") ctx.ctx.getMessage(id.id)
      else None
    }, Node.possibleNodeTypes[MessageRepo, Node](MessageType))

  def idFields[T: Identifiable] = fields[Unit, T](
    Node.globalIdField,
    Field("rawId", StringType, resolve = ctx => implicitly[Identifiable[T]].id(ctx.value))
  )

  val MessageType: ObjectType[Unit, Message] = ObjectType(
    "Message",
    "Chat Message",
    interfaces[Unit, Message](nodeInterface),
    idFields[Message] ++
    fields[Unit, Message](
      Field("content", StringType, Some("Message content"), resolve = _.value.content)))

  val ConnectionDefinition(_, messageConnection) =
    Connection.definition[MessageRepo, Connection, Option[Message]](
      "Message", OptionType(MessageType))

  val ChatType: ObjectType[MessageRepo, Chat] = ObjectType(
    "Chat",
    "Example chat",
    interfaces[MessageRepo, Chat](nodeInterface),
    fields[MessageRepo, Chat](
      Node.globalIdField[MessageRepo, Chat],
      Field("name", OptionType(StringType), Some("The name of the Chat"), resolve = _.value.name),
      Field("messages", OptionType(messageConnection), arguments = Connection.Args.All,
        resolve = (ctx) => {
          Connection.connectionFromSeq(ctx.ctx.getChatMessages(ctx.value.id), ConnectionArgs(ctx))
        })))

  val QueryType = ObjectType("Query", fields[MessageRepo, Unit](
    Field("chat", OptionType(ChatType), resolve = _.ctx.getChat("1")),
    nodeField))

  val schema = Schema(QueryType)
}
