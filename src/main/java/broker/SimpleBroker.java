package broker;

import common.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;


public class SimpleBroker {
    /* TODO: variables as needed */
    
    private final MessageListener listener = new MessageListener() {
        @Override
        public void onMessage(Message msg) {

            if(msg instanceof RegisterMessage) {
                try {
                    MessageConsumer consumer = session.createConsumer(session.createQueue(((RegisterMessage) msg).getClientName()+"Out"));
                    MessageProducer producer = session.createProducer(session.createQueue(((RegisterMessage) msg).getClientName()+"In"));
                    ClientInfos newClient = new ClientInfos(consumer, producer, ((RegisterMessage) msg).getClientName());
                    clientInfos.add(newClient);

                    consumer.setMessageListener(new MessageListener() {
                        @Override
                        public void onMessage(Message msg) { //TODO functionality
                            if(msg instanceof RequestListMessage) {

                            }
                            else if(msg instanceof SellMessage){

                            }
                            else if(msg instanceof BuyMessage) {

                            }
                        }
                    });
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
            /* TODO: prepare stocks as topics */
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
