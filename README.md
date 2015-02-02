# akka-distributed-hash
a distributed hash in a cluster in akka


How it works:

1- start the first node of the cluster (on port 2551): sbt "run 2551"   

2- check the hash on the fist node through an http GET request: curl http://localhost:8551
    (you'll see nothing; the hash is empty)

3- put elements into the hash through an http PUT request: curl -X PUT http://localhost:8551 --data "hello=123&world=456"
    (this will insert some more data in that node's hash)

4- check the hash again now: curl http://localhost:8551

5- start the second node of the cluster (on port 2552): sbt "run 2552"

6- check that the hash has been copied to the second (actually to all) node(s) of the cluster: 

	curl http://localhost:8552/hello

	curl http://localhost:8552/world

	or simply

	curl http://localhost:8552
	

AND YOU ARE DONE!!

*** http deletes (Eg: " curl -X DELETE http://localhost:8551 --data 'hello' ") are supported

*** try now to delete something from one of the nodes and check that it's been deleted in the other nodes


//TODO: the second node has to sincronize on initialization

//TODO: syncronize maps every second ???
//TODO: ...
