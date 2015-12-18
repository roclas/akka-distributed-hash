package sample.cluster.simple

case class delete(s: String)
case class put(k: String,v:String)
case class syncronize(hash:java.util.HashMap[String,String])
case class syncronizeOverriding(hash:java.util.HashMap[String,String])
case class syncronizeInitial(hash:java.util.HashMap[String,String])
case class md5digest(s:String)
