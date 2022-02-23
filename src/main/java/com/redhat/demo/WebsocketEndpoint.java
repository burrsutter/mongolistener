package com.redhat.demo;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.server.ServerEndpoint;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
// import javax.websocket.server.PathParam;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.jboss.logging.Logger;

@ServerEndpoint("/endpoint")
@ApplicationScoped
public class WebsocketEndpoint {
    private static final Logger LOG = Logger.getLogger(WebsocketEndpoint.class);
    
    Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
      LOG.info("onOpen");
      LOG.info("onOpen ID: " + session.getId());    
      sessions.put(session.getId(),session);      
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
        sessions.values().forEach(session -> {
          session.getAsyncRemote().sendObject(message, result ->  {
              if (result.getException() != null) {
                  LOG.error("Unable to send message: " + result.getException());
              }
          });
        });
      }
  

}
