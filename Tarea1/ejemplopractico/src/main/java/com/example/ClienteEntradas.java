package com.example;

import java.util.Map;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class ClienteEntradas {

    private static final String QUEUE_NAME = "cola_entradas";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
                Channel channel = connection.createChannel()) {

            Map<String, Object> args = Map.of("x-queue-type", "quorum");
            channel.queueDeclare(QUEUE_NAME, true, false, false, args);

            String message = (argv.length < 1) ? "Entrada VIP para Carlos..." : String.join(" ", argv);

            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            System.out.println(" [x] Solicitud enviada a la cola virtual: '" + message + "'");
        }
    }
}
