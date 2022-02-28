package com.redhat.demo;


import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;


import org.bson.conversions.Bson;
import org.jboss.logging.Logger;

import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Multi;

// @ServerEndpoint("/mongogrades")


public class MongoStreamListenerWS {
    private static final Logger LOG = Logger.getLogger(MongoStreamListenerWS.class);
    
    private final List<Session> sessions = new CopyOnWriteArrayList<>();

    @Inject 
    ReactiveMongoClient mongoClient;

    @PostConstruct
    public void setup() {
        /* Mongo Change Streams Listener Stuff */  
        

        ReactiveMongoDatabase database = mongoClient.getDatabase("sample_training");
        ReactiveMongoCollection<Grade> dataCollection = database.getCollection("grades", Grade.class);
        ChangeStreamOptions options = new ChangeStreamOptions().fullDocument(FullDocument.UPDATE_LOOKUP);

        List<Bson> pipeline = Collections.singletonList(
            Aggregates.match(
                    Filters.and(
                            Filters.eq("operationType", "update")                            
                    )
            )
        );
        
        Multi<ChangeStreamDocument<Grade>> watcher = dataCollection.watch(pipeline, Grade.class, options);

        
        watcher.subscribe().with(this.receiveChanges());
        
           
    }

    public Consumer<ChangeStreamDocument<Grade>> receiveChanges() {
      return message -> { 
        LOG.info("Message: " + message);
        Grade grade = message.getFullDocument();
        String toBeSent = grade.getStudentId().toString();
        LOG.info(toBeSent);
	      broadcast(toBeSent);
      };
    }

    /* Websockets Stuff */
    @OnOpen
    public void onOpen(Session session) {
      LOG.info("onOpen");
      LOG.info("onOpen ID: " + session.getId());  
      this.sessions.add(session);      
    }
  
    @OnClose
    public void onClose(Session session) {
        LOG.info("onClose");
        sessions.remove(session);      
    }
  
    @OnError
    public void onError(Session session, Throwable throwable) {        
        LOG.error("onError", throwable);        
    }
  
    @OnMessage
    public void onMessage(String message, Session session) {      
      LOG.info("onMessage: " + message);
      LOG.info("onMessage ID:" + session.getId());
    }    

    public void broadcast(String message) {
        sessions.forEach(session -> {
          session.getAsyncRemote().sendObject(message, result ->  {
              if (result.getException() != null) {
                  LOG.error("Unable to send message: " + result.getException());
              }
          });
        });
      }


}
