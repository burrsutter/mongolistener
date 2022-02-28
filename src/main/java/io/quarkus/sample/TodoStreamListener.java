package io.quarkus.sample;


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

@ServerEndpoint("/todostream")

@ApplicationScoped
public class TodoStreamListener {
    private static final Logger LOG = Logger.getLogger(TodoStreamListener.class);
    
    // private final List<Session> sessions = new CopyOnWriteArrayList<>();
    Map<String, Session> sessions = new ConcurrentHashMap<>();

    private Cancellable cancellable;

    static ChangeStreamDocument<Todo> SENTINEL = new ChangeStreamDocument<>(null, null, null, null, null, null, null, null, null, null);

        
    @Inject 
    ReactiveMongoClient mongoClient;

    public void init(@Observes StartupEvent event)  {
        /* Mongo Change Streams Listener Stuff */
        ReactiveMongoDatabase database = mongoClient.getDatabase("todo");
        ReactiveMongoCollection<Todo> dataCollection = database.getCollection("todo", Todo.class);
        
        
        Multi<ChangeStreamDocument<Todo>> watcher = dataCollection.watch(Todo.class);
        Multi<ChangeStreamDocument<Todo>> periodic = Multi.createFrom().ticks().every(Duration.ofSeconds(10))
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

    public void receiveChanges(ChangeStreamDocument<Todo> change) {
      LOG.info("\n\n ** Change: " + change);
      if (change != null) {
        Todo todo = change.getFullDocument();
        if (todo != null) {
          String toBeSent = todo.getTitle();
          // String toBeSent = todo.toString();
          LOG.info("toBeSent=" + toBeSent);
          broadcast(toBeSent); // sent out via the websocket connections  
        } else { // todo is null
          // LOG.error("todo is null: " + todo);
        }
      } else { // change is null
        LOG.error("change is null: " + change);
      }
    }

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
