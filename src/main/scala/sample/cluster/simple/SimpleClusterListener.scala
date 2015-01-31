package sample.cluster.simple

import java.util

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.ActorLogging
import akka.actor.Actor
import akka.cluster.MemberStatus.Up
import spray.can.Http
import spray.http.{HttpResponse, Uri, HttpMethods, HttpRequest}
import scala.collection.JavaConversions._


class SimpleClusterListener extends Actor with ActorLogging {
 
  val cluster = Cluster(context.system)
 
  // subscribe to cluster changes, re-subscribe when restart 
  override def preStart(): Unit = {
    //#subscribe
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
    //#subscribe
  }
  override def postStop(): Unit = cluster.unsubscribe(self)
  
  val hash=new util.HashMap[String,String]()
  hash.put("hola","hola")

  def receive = {
    case s:String=>
      log.info("Receiving String: {}", s)
      sender ! hash.toString
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
      log.info("Current members:{}",cluster.state.members.filter(_.status == Up))

    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
      log.info("Current members:{}",cluster.state.members.filter(_.status == Up))
    case _: MemberEvent => // ignore


    case h:util.HashMap[_,_]=>
      log.info("receiving {}",h)
      //updating hash
      for((k,v)<-h){
        log.info("updating {} {}",k,v)
        hash.put(k.asInstanceOf[String],v.asInstanceOf[String])
      }
    case HttpRequest(HttpMethods.PUT, Uri.Path("/ping"), _, _, _) =>
      log.info("receiving put")
      hash.put("lalala","lalala")
      sender ! HttpResponse(entity = hash.toString)
      for (m<-cluster.state.members.filter(_.status == Up)){
        context.actorSelection(m.address+"/user/clusterListener") ! hash
        log.info("\n\n\nsending hash to"+m.address)
      }
    case HttpRequest(HttpMethods.GET, Uri.Path("/ping"), _, _, _) =>
      sender ! HttpResponse(entity = hash.toString)
    case r:HttpRequest =>
      sender ! HttpResponse(entity = "this page doesn't exist")
    case c:akka.io.Tcp.Connected =>
      sender ! Http.Register(self)
  }
}
