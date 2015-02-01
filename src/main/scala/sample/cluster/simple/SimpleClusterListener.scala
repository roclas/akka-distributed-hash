package sample.cluster.simple

import java.util

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.ActorLogging
import akka.actor.Actor
import akka.cluster.MemberStatus.Up
import spray.can.Http
import spray.http._
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

  def getParamsMap(s: String) = {
    s.split('&') map { str=>
      val pair = str.split('=')
      (pair(0) -> pair(1))
    } toMap
  }

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


    case delete(data:String)=>
      log.info("deleting{}",data)
      hash.remove(data)
      
    case h:util.HashMap[_,_]=>
      log.info("receiving {}",h)
      //updating hash
      for((k,v)<-h){
        log.info("updating {} {}",k,v)
        hash.put(k.asInstanceOf[String],v.asInstanceOf[String])
      }
    case req@HttpRequest(HttpMethods.PUT, Uri.Path("/ping"), headers, entity, protocol) =>{
      val data=req.entity.asString(HttpCharsets.`UTF-8`)
      val hashdata=getParamsMap(data)
      for((k,v)<-hashdata){
        log.info("Receiving PUT request query param: {} {}", k,v)
        hash.put(k,v)
      }
      sender ! HttpResponse(entity = hash.toString)
      for (m<-cluster.state.members.filter(_.status == Up)){
        context.actorSelection(m.address+"/user/clusterListener") ! hash
        log.info("\n\n\nsending hash to"+m.address)
      }
    }
    case req@HttpRequest(HttpMethods.DELETE, Uri.Path("/ping"), headers, entity, protocol) =>{
      val data=req.entity.asString(HttpCharsets.`UTF-8`)
      log.info("Receiving DELETE request query param: {} ", data)
      hash.remove(data)
      sender ! HttpResponse(entity = hash.toString)
      for (m<-cluster.state.members.filter(_.status == Up)){
        context.actorSelection(m.address+"/user/clusterListener") ! delete(data)
      }
    }
    case req@HttpRequest(HttpMethods.GET, Uri.Path("/ping"), headers, entity, protocol) =>
      val somekey=req.uri.query.get("key")
      val key=somekey match {
        case None=>" "
        case null=>" "
        case Some(name)=>name
      }
      log.info("receiving get {}",key)
      sender ! HttpResponse(entity = hash.get(key.toString))
    case r:HttpRequest =>
      sender ! HttpResponse(entity = "this page doesn't exist")
    case c:akka.io.Tcp.Connected =>
      sender ! Http.Register(self)
  }
}
