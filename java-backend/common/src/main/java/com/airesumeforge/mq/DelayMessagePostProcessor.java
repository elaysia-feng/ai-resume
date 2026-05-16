package com.airesumeforge.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

@RequiredArgsConstructor
public class DelayMessagePostProcessor implements MessagePostProcessor {
    private final long delay;

    @Override
    public Message postProcessMessage(Message message) throws AmqpException {
        message.getMessageProperties().setDelayLong(delay);

        return message;
    }

}
