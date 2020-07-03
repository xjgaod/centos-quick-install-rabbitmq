package org.onosproject.vcpena.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import org.junit.Test;
import org.onosproject.vcpe.model.GWStatusRuntimeData;

import javax.ws.rs.client.Entity;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMqTest {
    private static final String EXCHANEG = "notification.test";
    private static final String DEFAULT_QUEUE = "notification.test.queue";
    private static ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testMqProducer() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = getConnectionFactory();
        Connection connection = connectionFactory.newConnection();
        final Channel channel = connection.createChannel();

        String notification = " <notification xmlns=\"urn:ietf:params:xml:ns:netconf:notification:1.0\">" +
                "<eventTime>2020-06-07T02:24:24Z</eventTime>" +
                "<interfacestatus xmlns=\"http://certusnet.com/nfv/flexgw/certus-flexgw-notifications\">" +
                "<ifstatus>" +
                "<ifname>WAN1</ifname>" +
                "<ipaddr>172.30.36.78</ipaddr>" +
                "<ipmask>30</ipmask>" +
                "<type>WAN</type>" +
                "<status>1</status>" +
                "<desc>WAN1__ChinaTelecom</desc>" +
                "</ifstatus>" +
                "</interfacestatus>" +
                "</notification>";
        GWStatusRuntimeData gwStatusRuntimeData = NotifyGwPaserUtils.parseIfStatus(notification, "ss");
        String  message = Entity.json( mapper.writeValueAsString( gwStatusRuntimeData ) ).getEntity();
        for (int i = 0; i<10 ; i++){
            channel.basicPublish(EXCHANEG,"notification",null, message.getBytes());
            System.out.println("Sent > " + message);
        }

    }


    @Test
    public void testMqConsumer() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = getConnectionFactory();
        Connection connection = connectionFactory.newConnection();
        final Channel channel = connection.createChannel();

        boolean autoAck = false;
        channel.basicConsume(DEFAULT_QUEUE,autoAck,new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException{
                String key = envelope.getRoutingKey();
                String message = new String(body);
                long deliveryTag = envelope.getDeliveryTag();
                if (key.startsWith("notification")) { // only consumer io warning messages
                    //consume message
                    System.out.println("consumed: " + message);
                    channel.basicAck(deliveryTag,false);

                } else { //reject other messages and requeue them
                    System.out.println("rejected: " + message);
                    channel.basicReject(deliveryTag,true);
                }
            }
        });
    }

    private ConnectionFactory getConnectionFactory() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("172.19.106.227");
        connectionFactory.setPort(5672);
        connectionFactory.setVirtualHost("/");
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        return connectionFactory;
    }
}
