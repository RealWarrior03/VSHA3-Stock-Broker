package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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
    MessageListener consumerListener;
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
        consumerListener = new MessageListener() {
            public void onMessage(Message message) throws Error {
                if (message instanceof ObjectMessage) {
                    BrokerMessage msg;  //TODO fix error
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
                    if (msg instanceof BuyMessage) {
                        try {
                            buy_response((BuyMessage) msg);
                        } catch (JMSException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (msg instanceof SellMessage) {
                        try {
                            sell_response((SellMessage) msg);
                        } catch (JMSException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (msg instanceof ErrorMessage) {
                        if (msg.getType().equals(BrokerMessage.Type.STOCK_BUY_ERR)) {
                            System.out.println("Buying "+ ((ErrorMessage) msg).getAmount() +" units of Stock "+ ((ErrorMessage) msg).getStockName()+" didn't work.");
                        }
                        if (msg.getType().equals(BrokerMessage.Type.STOCK_SELL_ERR)) {
                            System.out.println("Selling "+ ((ErrorMessage) msg).getAmount() +" units of Stock "+ ((ErrorMessage) msg).getStockName()+" didn't work.");
                        }
                    }
                }
            }
        };
        consumer.setMessageListener(consumerListener);
        topicConsumers = new ArrayList<>();

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
    
    public void buy(String stockName, int amount) throws JMSException {
        double price = getPriceOfStock(stockName);
        if (amount < 0) {
            System.out.println("You can't buy a negative amount of stocks, but nice try!");
            return;
        }
        if (budget >= price*amount) {
            BuyMessage buyMsg = new BuyMessage(stockName, amount);
            ObjectMessage msg = session.createObjectMessage(buyMsg);
            producer.send(msg);
        } else {
            System.out.println("you better earn some money, budget is too low");
        }
    }

    public void buy_response(BuyMessage response) throws  JMSException {
        budget -= getPriceOfStock(response.getStockName())* response.getAmount();
        System.out.println("Buying "+ response.getAmount() +" units of "+ response.getStockName() +" was succesfull! Your new budget is "+ budget);
    }

    public void sell(String stockName, int amount) throws JMSException {
        if (amount < 0) {
            System.out.println("You can't sell a negative amount of stocks.");
            return;
        }
        SellMessage sellMsg = new SellMessage(stockName, amount);
        ObjectMessage msg = session.createObjectMessage(sellMsg);
        producer.send(msg);
    }

    public void sell_response(SellMessage response) throws  JMSException {
        budget += getPriceOfStock(response.getStockName())* response.getAmount();
        System.out.println("Selling "+ response.getAmount() +" units of "+ response.getStockName() +" was succesfull! Your new budget is "+ budget);
    }
    
    public void watch(String stockName) throws JMSException {
        Topic topic = session.createTopic(stockName);
        MessageConsumer topicConsumer = session.createConsumer(topic);
        MessageListener topicListener = new MessageListener() {
            @Override
            public void onMessage(Message message) {
                if (message instanceof ObjectMessage) {
                    BrokerMessage msg;
                    try {
                        msg = (BrokerMessage) ((ObjectMessage) message).getObject();
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                    if (msg instanceof AnnouncementMessage) {
                        String announcement = ((AnnouncementMessage) msg).getAnnouncemnet();
                        System.out.println(announcement);
                    }
                }
            }
        };
        topicConsumer.setMessageListener(topicListener);
        topicConsumers.add(topicConsumer);
    }

    public void unwatch(String stockName) throws JMSException {
        // TODO: nicht alle sondern nur den mit dem namen schließen
        for (MessageConsumer tc : topicConsumers) {
            tc.close();
        }
    }
    
    public void quit() throws JMSException {
        UnregisterMessage unRegMsg = new UnregisterMessage(clientName);
        producer.send(session.createObjectMessage(unRegMsg));
        producer.close();
        consumer.close();
        for(MessageConsumer tc : topicConsumers) {
            tc.close();
        }
    }

    private double getPriceOfStock(String stockName) {
        Stock s = findStockInList(stockName);
        if (s == null) {
            System.out.println("Stock Not Found");
            return -1;
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
                        case "help":
                            System.out.println("budget, quit, list, buy [stock] [amount], sell [stock] [amount], watch [stock], unwatch [stock]");
                            break;
                        case "budget":
                            System.out.println("Your budget is: " + client.budget);
                            break;
                        case "quit":
                            client.quit();
                            System.out.println("Bye bye");
                            running = false;
                            System.exit(0);
                            break;
                        case "list":
                            client.requestList();
                            break;
                        case "buy":
                            if(task.length == 3) {
                                try{
                                    client.buy(task[1], Integer.parseInt(task[2]));
                                }catch (NumberFormatException e){
                                    System.out.println("Correct usage: sell [stock] [amount]");
                                }
                            } else {
                                System.out.println("Correct usage: buy [stock] [amount]");
                            }
                            break;
                        case "sell":
                            if(task.length == 3) {
                                try{
                                    client.sell(task[1], Integer.parseInt(task[2]));
                                }catch (NumberFormatException e){
                                    System.out.println("Correct usage: sell [stock] [amount]");
                                }
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
                            System.out.println("budget, quit, list, buy [stock] [amount], sell [stock] [amount], watch [stock], unwatch [stock]");
                    }
                }
            }
            
        } catch (JMSException | IOException ex) {
            Logger.getLogger(JmsBrokerClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
