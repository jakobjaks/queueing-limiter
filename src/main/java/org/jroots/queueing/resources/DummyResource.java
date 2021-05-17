package org.jroots.queueing.resources;

import com.codahale.metrics.annotation.Timed;
import org.jroots.queueing.api.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/dummy")
@Produces(MediaType.APPLICATION_JSON)
public class DummyResource {

    private final Logger logger = LoggerFactory.getLogger(DummyResource.class);

    public DummyResource() {
    }

    @POST
    @Timed
    public void pushMessageToQueue(Message message) {

    }
}
