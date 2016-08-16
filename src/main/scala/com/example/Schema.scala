package com.example

import sangria.relay._
import sangria.schema._
import ChatData.{ Chat, Message, Event, MessageAdded, MessageRepo }


object ChatSchema {

  val NodeDefinition(nodeInterface, nodeField) = Node.definition(
    (id: GlobalId, ctx: Context[MessageRepo, Any]) => {
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

  val ConnectionDefinition(messageEdge, messageConnection) =
    Connection.definition[MessageRepo, Connection, Message](
      "Message", MessageType)

  val ChatType: ObjectType[MessageRepo, Chat] = ObjectType(
    "Chat",
    "Example chat",
    interfaces[MessageRepo, Chat](nodeInterface),
    fields[MessageRepo, Chat](
      Node.globalIdField[MessageRepo, Chat],
      Field("name", OptionType(StringType), Some("The name of the Chat"), resolve = _.value.name),
      Field("messages", messageConnection, arguments = Connection.Args.All,
        resolve = (ctx) => {
          ctx.ctx.getChatMessages(
            ctx.value.id,
            ctx.arg(Connection.Args.Before).flatMap(Connection.cursorToOffset),
            ctx.arg(Connection.Args.After).flatMap(Connection.cursorToOffset),
            ctx.arg(Connection.Args.First),
            ctx.arg(Connection.Args.Last))
        })))

  val QueryType = ObjectType("Query", fields[MessageRepo, Any](
    Field("chat", OptionType(ChatType), resolve = _.ctx.getChat("1")),
    nodeField))

  val MessageAddedType = ObjectType(
    "MessageAdded",
    "MessageAdded event",
    fields[MessageRepo, Edge[Message]](
      Field("clientMutationId", StringType, resolve = (ctx) => "lskfjd"),
      Field("messageEdge", messageEdge, resolve = (ctx) => { ctx.value })))

  val subInputType = InputObjectType[InputObjectType.DefaultInput]("SubInput",
    fields = InputField("clientMutationId", StringType) :: Nil)
  val inputArg = Argument("input", subInputType)
  val SubscriptionType = ObjectType(
    "Subscription",
    fields[MessageRepo, Any](
      Field("messageAdded", MessageAddedType,
        arguments = inputArg :: Nil,
        resolve = _.value.asInstanceOf[Edge[Message]])))

  val schema = Schema(QueryType, None, Some(SubscriptionType))
}
