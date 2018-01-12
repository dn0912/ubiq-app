package com.example.dnguyen.ubiq_app;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;

/**
 * Created by dnguyen on 12.01.18.
 */

public class ConsumerTest extends DefaultConsumer{
    public ConsumerTest(Channel channel){
        super(channel);
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
    }
}
