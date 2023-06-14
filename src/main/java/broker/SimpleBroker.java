package broker;

import common.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;


public class SimpleBroker {
    ActiveMQConnectionFactory conFactory;
    Connection con;
    Session session;
    SimpleBroker myself = this;

    ArrayList<StockInfos> stockList;
    ArrayList<ClientInfos> clientInfos;


    private final MessageListener listener = new MessageListener() {
        @Override
        public void onMessage(Message msg) {
            if(msg instanceof ObjectMessage) {
                message = msg.getBody();

            }

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
        conFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        con = conFactory.createConnection();
        con.start();
        session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue registrationQueue = session.createQueue("registrationQueue");
        MessageConsumer consumer = session.createConsumer(registrationQueue);
        consumer.setMessageListener(listener);

        /*prepare stocks as topics */
        for(Stock stock : stockList) {
            Topic topic = session.createTopic(stock.getName());
            MessageProducer producer = session.createProducer(topic);
            stockList.add(new StockInfos(stock, producer));
        }
    }

    public List<Stock> getStocks(){
        ArrayList<Stock> stocks;
        for(StockInfos si : stockList){
            stocks.add(si.stock);
        }

        return stocks;
    }
    
    public void stop() throws JMSException {
        //TODO
    }
    
    // returns -1 if there was a problem, returns 1 if successful
    public synchronized int buy(String stockName, int amount) throws JMSException {
        Stock stock = findStockInList(stockList, stockName);
        if(stock == null){return -1;}   //we dont have a stock with that name

        if(stock.getAvailableCount() >= amount){    //stock was found and required amount is available
            stock.setStockCount(stock.getAvailableCount() - amount);
            return 1;
        }

        return -1;
    }
    
    public synchronized int sell(String stockName, int amount) throws JMSException {
        //TODO
        return -1;
    }
    
    public synchronized List<Stock> getStockList() {
        return this.stockList;
    }

    private Stock findStockInList(ArrayList<StockInfos> stocks,String stockname){
        for(StockInfos s : stocks){
            if (s.getName().equals(stockname)){
                return s.stock;
            }
        }
        return null;
    }
}
