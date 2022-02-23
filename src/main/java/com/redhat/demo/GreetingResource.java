package com.redhat.demo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        
        return "Hello RESTEasy " + LocalDateTime.now().format(formatter);
    }
}