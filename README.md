Update application.properties 

quarkus.mongodb.connection-string=mongodb+srv://youruser:yourpassword@cluster10.lev4z.mongodb.net/test

mvn quarkus:dev

http://localhost:8080/mongofruits.html

Also run mongodb-quickstart after updating its application.properties

quarkus.http.port=8081

quarkus.mongodb.connection-string=mongodb+srv://youruser:yourpassword@cluster10.lev4z.mongodb.net/test


mvn quarkus:dev


http://localhost:8081/fruits.html


Add some Fruits

