ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"
val akkaVersion = "2.8.0"
val akkaHttpVersion = "10.5.0"

lazy val root = project.in(file("."))
  .settings(
    name := "scala_project_api2",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      // akka streams
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      // akka http
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.slick" %% "slick" % "3.4.1",
      "org.slf4j" % "slf4j-nop" % "2.0.5", // For Slick logging
      "org.xerial" % "sqlite-jdbc" % "3.40.1.0",
    )
  )


