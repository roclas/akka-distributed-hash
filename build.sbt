name := "A distributed hash in akka"

version := "0.1"

scalaVersion := "2.10.4"

resolvers += "spray repo" at "http://repo.spray.io"// release builds

libraryDependencies ++= Seq( 
  "com.typesafe.akka" %% "akka-cluster" % "2.3.3"
  ,"io.spray" %%  "spray-can" % "1.3.1"
)
