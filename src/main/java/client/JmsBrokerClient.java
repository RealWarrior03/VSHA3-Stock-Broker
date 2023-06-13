package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;

import common.*;
import org.apache.activemq.ActiveMQConnectionFactory;


public class JmsBrokerClient {
    private final String clientName;
    private Session session;
    private Queue consumerQ;
    private Queue producerQ;
    MessageConsumer consumer;
    MessageProducer producer;
    MessageListener listener;

    public JmsBrokerClient(String clientName) throws JMSException {
        this.clientName = clientName;

        ActiveMQConnectionFactory conFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection con = conFactory.createConnection();
        con.start();
        session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);

        consumerQ = session.createQueue(clientName+"In");
        producerQ = session.createQueue(clientName+"Out");
        consumer = session.createConsumer(consumerQ);
        producer = session.createProducer(producerQ);
        listener = new MessageListener() {
            public void onMessage(Message msg) {
                if(msg instanceof ObjectMessage) {

                }
            }
        };
        consumer.setMessageListener(listener);
        RegisterMessage regMsg = new RegisterMessage(clientName);
        ObjectMessage msg = session.createObjectMessage(regMsg);
        producer.send(msg);
        /* TODO: initialize connection, sessions, consumer, producer, etc. */
    }
    
    public void requestList() throws JMSException {
        //TODO
        /*ListMessage listMsg = new ListMessage();
        ObjectMessage msg = session.createObjectMessage(listMsg);
        producer.send(msg);*/
    }
    
    public void buy(String stockName, int amount) throws JMSException {
        //TODO
        BuyMessage buyMsg = new BuyMessage(stockName, amount);
        ObjectMessage msg = session.createObjectMessage(buyMsg);
        producer.send(msg);
    }
    
    public void sell(String stockName, int amount) throws JMSException {
        //TODO
        SellMessage sellMsg = new SellMessage(stockName, amount);
        ObjectMessage msg = session.createObjectMessage(sellMsg);
        producer.send(msg);
    }
    
    public void watch(String stockName) throws JMSException {
        Topic topic = session.createTopic(stockName);
        MessageConsumer topicConsumer = session.createConsumer(topic);
    }

    public void unwatch(String stockName) throws JMSException {
        session.unsubscribe(stockName);
    }
    
    public void quit() throws JMSException {
        //TODO
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter the client name:");
            String clientName = reader.readLine();
            
            JmsBrokerClient client = new JmsBrokerClient(clientName);
            
            boolean running = true;
            while(running) {
                System.out.println("Enter command:");
                String[] task = reader.readLine().split(" ");
                
                synchronized(client) {
                    switch(task[0].toLowerCase()) {
                        case "quit":
                            client.quit();
                            System.out.println("Bye bye");
                            running = false;
                            break;
                        case "list":
                            client.requestList();
                            break;
                        case "buy":
                            if(task.length == 3) {
                                client.buy(task[1], Integer.parseInt(task[2]));
                            } else {
                                System.out.println("Correct usage: buy [stock] [amount]");
                            }
                            break;
                        case "sell":
                            if(task.length == 3) {
                                client.sell(task[1], Integer.parseInt(task[2]));
                            } else {
                                System.out.println("Correct usage: sell [stock] [amount]");
                            }
                            break;
                        case "watch":
                            if(task.length == 2) {
                                client.watch(task[1]);
                            } else {
                                System.out.println("Correct usage: watch [stock]");
                            }
                            break;
                        case "unwatch":
                            if(task.length == 2) {
                                client.unwatch(task[1]);
                            } else {
                                System.out.println("Correct usage: watch [stock]");
                            }
                            break;
                        default:
                            System.out.println("Unknown command. Try one of:");
                            System.out.println("quit, list, buy, sell, watch, unwatch");
                    }
                }
            }
            
        } catch (JMSException | IOException ex) {
            Logger.getLogger(JmsBrokerClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
