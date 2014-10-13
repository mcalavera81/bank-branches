name := """bank-branches"""

version := "1.0"

//scalaVersion := "2.11.1"
scalaVersion := "2.10.4"


resolvers += "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.6" % "test"

libraryDependencies += "org.jsoup" % "jsoup" % "1.7.2"

libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.0.0"

libraryDependencies += "org.reactivemongo" %% "reactivemongo" % "0.10.0"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.3.2"

