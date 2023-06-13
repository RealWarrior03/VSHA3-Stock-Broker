package broker;

import common.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;


public class SimpleBroker {
    /* TODO: variables as needed */
    ActiveMQConnectionFactory conFactory;
    Connection con;
    Session session;
    SimpleBroker myself = this;

    ArrayList<Topic> topics;
    ArrayList<ClientInfos> clientInfos;

    private final MessageListener listener = new MessageListener() {
        @Override
        public void onMessage(Message msg) {

            if(msg instanceof RegisterMessage) {
                try {
                    MessageConsumer consumer = session.createConsumer(session.createQueue(((RegisterMessage) msg).getClientName()+"Out"));
                    MessageProducer producer = session.createProducer(session.createQueue(((RegisterMessage) msg).getClientName()+"In"));
                    ClientInfos newClient = new ClientInfos(consumer, producer, ((RegisterMessage) msg).getClientName(), myself);
                    clientInfos.add(newClient);

                } catch (JMSException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    };
    
    public SimpleBroker(List<Stock> stockList) throws JMSException {
        /* TODO: initialize connection, sessions, etc. */
        conFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        con = conFactory.createConnection();
        con.start();
        session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue registrationQueue = session.createQueue("registrationQueue");
        MessageConsumer consumer = session.createConsumer(registrationQueue);
        consumer.setMessageListener(listener);

        /*prepare stocks as topics */
        for(Stock stock : stockList) {
            topics.add(session.createTopic(stock.getName()));
        }
    }
    
    public void stop() throws JMSException {
        //TODO
    }
    
    public synchronized int buy(String stockName, int amount) throws JMSException {
        //TODO
        return -1;
    }
    
    public synchronized int sell(String stockName, int amount) throws JMSException {
        //TODO
        return -1;
    }
    
    public synchronized List<Stock> getStockList() {
        List<Stock> stockList = new ArrayList<>();

        /* TODO: populate stockList */

        return stockList;
    }
}
