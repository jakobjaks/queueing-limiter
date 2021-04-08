package org.jroots.queueing.client.producer;

import org.jroots.queueing.api.Message;

public interface QueueProducer {
    void sendMessage(Message message, long delay);
}
