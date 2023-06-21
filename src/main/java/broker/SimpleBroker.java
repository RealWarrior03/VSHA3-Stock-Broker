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

    ArrayList<StockInfos> stockList = new ArrayList<>();
    ArrayList<ClientInfos> clientInfos;


    private final MessageListener listener = new MessageListener() {
        @Override
        public void onMessage(Message msg) {
            System.out.println("Message received");
            BrokerMessage bm = null;  //TODO fix error
            try {
                bm = (BrokerMessage) ((ObjectMessage) msg).getObject();
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }

            if(bm instanceof RegisterMessage) {
                System.out.println(((RegisterMessage) bm).getClientName());
                try {
                    MessageConsumer consumer = session.createConsumer(session.createQueue(((RegisterMessage) bm).getClientName()+"Out"));
                    MessageProducer producer = session.createProducer(session.createQueue(((RegisterMessage) bm).getClientName()+"In"));
                    ClientInfos newClient = new ClientInfos(consumer, producer, ((RegisterMessage) bm).getClientName(), myself);
                    clientInfos.add(newClient);

                } catch (JMSException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    };
    
    public SimpleBroker(List<Stock> stockList) throws JMSException {
        conFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        conFactory.setTrustAllPackages(true);
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
            this.stockList.add(new StockInfos(stock, producer));
        }

        clientInfos = new ArrayList<>();
    }

    public List<Stock> getStocks(){
        ArrayList<Stock> stocks = new ArrayList<>();
        for(StockInfos si : stockList){
            stocks.add(si.stock);
        }

        return stocks;
    }
    
    public void stop() throws JMSException {
        try {
            for (ClientInfos ci : clientInfos) {
                ci.producer.close();
                ci.consumer.close();
            }
            for (StockInfos si : stockList) {
                si.producer.close();
            }
            con.stop();
            session.close();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
    
    // returns -1 if there was a problem, returns 1 if successful
    public synchronized int buy(String stockName, int amount) throws JMSException {
        StockInfos stockinfo = findStockInList(stockList, stockName);
        if(stockinfo == null){
            return -1;
        }
        Stock stock = stockinfo.stock;

        if(stock == null){return -1;}   //we dont have a stock with that name

        if(stock.getAvailableCount() >= amount){    //stock was found and required amount is available
            stock.setAvailableCount(stock.getAvailableCount() - amount);

            informStockChange(stockName);
            return 1;
        }

        return -1;
    }

    // returns -1 if there was a problem, returns 1 if successful
    public synchronized int sell(String stockName, int amount) throws JMSException {
        StockInfos stockinfo = findStockInList(stockList, stockName);
        if(stockinfo == null){
            return -1;
        }
        Stock stock = stockinfo.stock;

        if(stock == null){return -1;}   //no such stock is offered

        if(stock.getStockCount() - stock.getAvailableCount() >= amount){    //stock was found and required amount is available
            stock.setAvailableCount(stock.getAvailableCount() + amount);

            informStockChange(stockName);
            return 1;
        }

        return -1;
    }

    private void informStockChange(String stockName){
        //inform subs of stock topic that list has changed
        ObjectMessage returnMessage = null;
        try {
            returnMessage = session.createObjectMessage(new AnnouncementMessage(stockName + " changed, the server has " + findStockInList(stockList, stockName).stock.getAvailableCount() + " available stocks"));
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
        StockInfos changedStock = findStockInList(stockList, stockName);
        changedStock.informSubs(returnMessage);
    }
    
    public synchronized List<StockInfos> getStockList() {
        return this.stockList;
    }

    public StockInfos findStockInList(ArrayList<StockInfos> stocks,String stockname){
        for(StockInfos s : stocks){
            if (s.getName().equals(stockname)){
                return s;
            }
        }
        return null;
    }

    public void deleteClientFromList(ClientInfos client){
        clientInfos.remove(client);
    }

}
