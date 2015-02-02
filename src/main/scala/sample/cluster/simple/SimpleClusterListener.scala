package sample.cluster.simple


import java.io.{ObjectOutputStream, ByteArrayOutputStream}

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.MemberStatus.Up
import java.util
import java.security.MessageDigest
import scala.collection.JavaConversions._
import spray.can.Http
import spray.http._


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
  val md5s=List()

  def getParamsMap(s: String) = {
    s.split('&') map { str=>
      val pair = str.split('=')
      (pair(0) -> pair(1))
    } toMap
  }

  def md5(m: java.util.HashMap[_,_]) = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(m)
    oos.close()
    val md = MessageDigest.getInstance("MD5")
    md.update(baos.toByteArray());
    md.digest()
  }

  def receive = {
    case MemberUp(member) =>
      context.actorSelection(member.address+"/user/clusterListener") ! syncronize(hash)
      log.info("Member is Up: {}", member.address)
      log.info("Current members:{}",cluster.state.members.filter(_.status == Up))
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
      log.info("Current members:{}",cluster.state.members.filter(_.status == Up))
    case _: MemberEvent => // ignore
    case syncronize(map)=>
      log.info("Syncronizing {} and {}",md5(hash),md5(map))
      for((k,v)<-map){ hash.put(k,v) }
      //for((k,v)<-hash){ if(map.get(k)==null)hash.remove(k) }
    case delete(data:String)=>
      log.info("deleting{}",data)
      hash.remove(data)
    case put(k:String,v:String)=>
      log.info("putting {} {}",k,v)
      hash.put(k,v)
      
    case req@HttpRequest(HttpMethods.PUT, _, _, _, _) =>{
      val data=req.entity.asString(HttpCharsets.`UTF-8`)
      val hashdata=getParamsMap(data)
      for((k,v)<-hashdata){
        log.info("Receiving PUT request query param: {} {}", k,v)
        hash.put(k,v)
        for (m<-cluster.state.members.filter(_.status == Up)){
          context.actorSelection(m.address+"/user/clusterListener") ! put(k,v)
        }
      }
      sender ! HttpResponse(entity = hash.toString)
    }
    case req@HttpRequest(HttpMethods.DELETE, _, _, _, _) =>{
      val data=req.entity.asString(HttpCharsets.`UTF-8`)
      log.info("Receiving DELETE request query param: {} ", data)
      hash.remove(data)
      sender ! HttpResponse(entity = hash.toString)
      for (m<-cluster.state.members.filter(_.status == Up)){
        context.actorSelection(m.address+"/user/clusterListener") ! delete(data)
      }
    }
    case req@HttpRequest(HttpMethods.GET, _, _, _, _) =>
      var response=""
      if(req.uri.path!=null)if(req.uri.path.toString.length>0){
        val k= req.uri.path.toString.replace("/", "")
        if(hash.get(k)!=null)response=hash.get(k)
      }
      log.info("sending response {} ",response)
      sender ! HttpResponse(entity = response)
    case r:HttpRequest =>
      sender ! HttpResponse(entity = "this page doesn't exist")
    case c:akka.io.Tcp.Connected =>
      sender ! Http.Register(self)
  }
}
