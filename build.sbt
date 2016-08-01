lazy val root = (project in file(".")).
  settings(
    organization := "com.example",
    name := "relay-sangria-example",
    scalaVersion := "2.11.8",
    version := "0.1"
  )

val akkaVersion = "2.4.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-experimental" %  akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaVersion,
  "org.sangria-graphql" %% "sangria" % "0.7.1",
  "org.sangria-graphql" %% "sangria-relay" % "0.7.1",
  "org.sangria-graphql" %% "sangria-spray-json" % "0.3.1",
  "com.typesafe.akka" %% "akka-testkit" %  akkaVersion% "test",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

resolvers += sbtResolver.value

scalacOptions += "-deprecation"
scalacOptions += "-feature"

fork := true
