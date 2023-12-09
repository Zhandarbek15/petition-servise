version := "0.1.0-SNAPSHOT"

scalaVersion := "3.3.0"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.5.0-M4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.5.0-M4",
  "mysql" % "mysql-connector-java" % "8.0.33"

)

libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.5.0"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.8.0"

lazy val root = (project in file("."))
  .settings(
    name := "DB"
  )
