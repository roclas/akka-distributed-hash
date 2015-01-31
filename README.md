# akka-distributed-hash
a distributed hash in a cluster in akka


How it works:

1- start the first node of the cluster (on port 2551): sbt "run 2551"   
2- start the second node of the cluster (on port 2552): sbt "run 2552"

3- check the hash on the fist node through an http get request: curl http://localhost:8551/ping
    (you'll see what the hash has)
4- check the hash on the fist node through an http get request: curl http://localhost:8552/ping
    (you'll see what the hash has)
    
5- modify the content of the first hash through an http put request: curl -X PUT http://localhost:8551/ping
    (this will insert some more data -hardcoded- in that node's hash)

6- check that the hash has been copied to the second (actually to all) node(s) of the cluster: curl http://localhost:8552/ping


AND YOU ARE DONE!!

//TODO: THE REAL THING HAS TO BE DONE

