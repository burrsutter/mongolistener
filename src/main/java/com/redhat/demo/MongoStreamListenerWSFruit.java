package com.redhat.demo;


import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.mongodb.client.model.changestream.ChangeStreamDocument;

import org.jboss.logging.Logger;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;

@ServerEndpoint("/mongofruits")

@ApplicationScoped
public class MongoStreamListenerWSFruit {
    private static final Logger LOG = Logger.getLogger(MongoStreamListenerWS.class);
    
    // private final List<Session> sessions = new CopyOnWriteArrayList<>();
    Map<String, Session> sessions = new ConcurrentHashMap<>();

    private Cancellable cancellable;

    static ChangeStreamDocument<Fruit> SENTINEL = new ChangeStreamDocument<>(null, null, null, null, null, null, null, null, null, null);

        
    @Inject 
    ReactiveMongoClient mongoClient;

    public void init(@Observes StartupEvent event)  {
        /* Mongo Change Streams Listener Stuff */
        ReactiveMongoDatabase database = mongoClient.getDatabase("fruit");
        ReactiveMongoCollection<Fruit> dataCollection = database.getCollection("fruit", Fruit.class);
        
        
        Multi<ChangeStreamDocument<Fruit>> watcher = dataCollection.watch(Fruit.class);
        Multi<ChangeStreamDocument<Fruit>> periodic = Multi.createFrom().ticks().every(Duration.ofSeconds(10))
        .map(x -> SENTINEL);


        this.cancellable = Multi.createBy().merging().streams(watcher, periodic).log().subscribe().with(s -> this.receiveChanges(s), f -> LOG.error("Unable to consume the stream", f));

        // watcher.subscribe().with(this.receiveChanges());
                   
    }

    @PreDestroy
    public void cleanup() {
        if (cancellable == null) {
            cancellable.cancel();
            cancellable = null;
        }
    }

    public void receiveChanges(ChangeStreamDocument<Fruit> change) {
      LOG.info("Change: " + change);
      if (change != null) {
        Fruit fruit = change.getFullDocument();
        if (fruit != null) {
          String toBeSent = fruit.getName() + ":" + fruit.getDescription();
          LOG.info("toBeSent=" + toBeSent);
          broadcast(toBeSent); // sent out via the websocket connections  
        } else { // fruit is null
          // LOG.error("fruit is null: " + fruit);
        }
      } else { // change is null
        LOG.error("change is null: " + change);
      }
    }

    // public Consumer<ChangeStreamDocument<Fruit>> receiveChanges() {
    //   return change -> { 
    //     LOG.info("Cessage: " + change);
    //     Fruit fruit = change.getFullDocument();
    //     String toBeSent = fruit.getName() + ":" + fruit.getDescription();
    //     LOG.info(toBeSent);
	  //   broadcast(toBeSent); // sent out via the websocket connections
    //   };
    // }

    /* Websockets Stuff */
    @OnOpen
    public void onOpen(Session session) {
      LOG.info("onOpen");
      LOG.info("onOpen ID: " + session.getId());  
      this.sessions.put(session.getId(), session);
    }
  
    @OnClose
    public void onClose(Session session) {
        LOG.info("onClose");
        sessions.remove(session.getId());      
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
        LOG.info("broadcasting: " + message + " to sessions size " + sessions.size());
        
        sessions.values().forEach(session -> {
          LOG.info("to session: " + session.getId());
          session.getAsyncRemote().sendObject(message, result ->  {
              if (result.getException() != null) {
                  LOG.error("Unable to send message: " + result.getException());
              }
          });
        });
      }


}
