package sample.cluster.simple
 
import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.io.IO
import spray.can.Http



object SimpleClusterApp extends App {
 
  // Override the configuration of the port
  // when specified as program argument
  if (args.nonEmpty) System.setProperty("akka.remote.netty.tcp.port", args(0))

  // Create an Akka system
  implicit val system = ActorSystem("ClusterSystem")

  val clusterListener = system.actorOf(Props( new SimpleClusterListener()), name = "clusterListener")

  Cluster(system).subscribe(clusterListener, classOf[ClusterDomainEvent])
  val httpPort=system.settings.config.getInt("akka.remote.netty.tcp.port")+6000
  IO(Http) ! Http.Bind(clusterListener, interface = "localhost", port = httpPort)

  /*
  val httpServer= system.actorOf(Props( new MyHttpService()), name = "httpServer")
  val httpPort=system.settings.config.getInt("akka.remote.netty.tcp.port")+6000
  IO(Http) ! Http.Bind(httpServer, interface = "localhost", port = httpPort)
  */

}
