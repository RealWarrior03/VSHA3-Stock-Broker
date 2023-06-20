package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.*;

import broker.StockInfos;
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
    int budget;

    List<Stock> stocklist;
    List<MessageConsumer> topicConsumers;

    public JmsBrokerClient(String clientName) throws JMSException {
        budget = 1000;

        this.clientName = clientName;

        ActiveMQConnectionFactory conFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        conFactory.setTrustAllPackages(true);
        Connection con = conFactory.createConnection();
        con.start();
        session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);

        consumerQ = session.createQueue(clientName+"In");
        producerQ = session.createQueue(clientName+"Out");
        Queue registerQ = session.createQueue("registrationQueue");
        consumer = session.createConsumer(consumerQ);
        producer = session.createProducer(producerQ);
        MessageProducer register = session.createProducer(registerQ);
        listener = new MessageListener() {
            public void onMessage(Message message) {
                if (message instanceof ObjectMessage) {
                    BrokerMessage msg = null;  //TODO fix error
                    try {
                        msg = (BrokerMessage) ((ObjectMessage) message).getObject();
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                    if (msg instanceof ListMessage) {
                        System.out.println("Available stocks:");
                        stocklist = ((ListMessage) msg).getStocks();
                        for (Stock stock : stocklist) {
                            System.out.println(stock.toString());
                        }
                    }
                }
            }
        };
        consumer.setMessageListener(listener);

        RegisterMessage regMsg = new RegisterMessage(clientName);
        register.send(session.createObjectMessage(regMsg));

        RequestListMessage reqListMsg= new RequestListMessage();
        producer.send(session.createObjectMessage(reqListMsg));

        /* TODO: initialize connection, sessions, consumer, producer, etc. */
    }
    
    public void requestList() throws JMSException {
        RequestListMessage listMsg = new RequestListMessage();
        ObjectMessage msg = session.createObjectMessage(listMsg);
        producer.send(msg);
    }
    
    public void buy(String stockName, int amount) throws JMSException,Error {
        //TODO auf acknowledgement warten ???
        double price = getPriceOfStock(stockName);
        if (budget >= price*amount) {
            BuyMessage buyMsg = new BuyMessage(stockName, amount);
            ObjectMessage msg = session.createObjectMessage(buyMsg);
            producer.send(msg);
            budget -= price*amount;
        } else {
            throw new Error("you better earn some money, budget is too low");
        }
    }
    
    public void sell(String stockName, int amount) throws JMSException {
        //TODO Was tun wenn der verkauf nicht durchgeht? Bisher optimistisch implementiert
        int owned = 0;
        if (amount >= owned) {
            SellMessage sellMsg = new SellMessage(stockName, amount);
            ObjectMessage msg = session.createObjectMessage(sellMsg);
            producer.send(msg);
            budget += getPriceOfStock(stockName) * amount;
        } else {
            System.out.println("you don't even have that much, can't sell that");
        }
    }
    
    public void watch(String stockName) throws JMSException {
        Topic topic = session.createTopic(stockName);
        MessageConsumer topicConsumer = session.createConsumer(topic);
    }

    public void unwatch(String stockName) throws JMSException {
        session.unsubscribe(stockName);
    }
    
    public void quit() throws JMSException {
        UnregisterMessage unRegMsg = new UnregisterMessage(clientName);
        producer.send(session.createObjectMessage(unRegMsg));
    }

    private double getPriceOfStock(String stockName)throws Error{
        Stock s = findStockInList(stockName);
        if(s==null){
            throw new Error("Stock Not Found");
        }
        return s.getPrice();
    }
    private Stock findStockInList(String stockname){
        for(Stock s : stocklist){
            if (s.getName().equals(stockname)){
                return s;
            }
        }
        return null;
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
                                System.out.println("Correct usage: unwatch [stock]");
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
