package com.example.dnguyen.ubiq_app;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by dnguyen on 12.01.18.
 */

public class RabbitMQConnector {
    private ConnectionFactory factory;
    private Connection conn;
    private Channel channel;
    private ConsumerTest consumer;

    private static final String EXCHANGE_NAME = "supermarkt_duc";

    public RabbitMQConnector() {
        factory = new ConnectionFactory();
        factory.setUsername("master");
        factory.setPassword("master");
        factory.setHost("155.54.204.46");
        factory.setPort(5672);
        try {
            conn = factory.newConnection();
            channel = conn.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public void declareQueue(String productName) {
        String queueName = "queue_" + productName;
        this.consumer = new ConsumerTest(this.channel);

        try {
            this.channel.queueDeclare(queueName, true, false, true, null);
            this.channel.queueBind(queueName, EXCHANGE_NAME, "offers."+productName+".*");
            this.channel.basicConsume(queueName, true, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
