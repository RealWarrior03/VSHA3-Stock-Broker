package broker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;
import common.Stock;

public class JmsBrokerServer {
    public static void main(String[] args) {
        try {
            List<Stock> stocks = new ArrayList<>();
            stocks.add(new Stock("Heckler&Koch", 74, 32));
            stocks.add(new Stock("Nestle", 49, 666));
            stocks.add(new Stock("Tencent", 83, 65));
            /*
            stocks.add(new Stock("Coca-Cola", 31, 17));
            stocks.add(new Stock("ExxonMobil", 92, 47));
            stocks.add(new Stock("Volkswagen", 88, 72));
            stocks.add(new Stock("Phillip Morris", 420, 94));
            stocks.add(new Stock("Boeing", 98, 58));
            stocks.add(new Stock("Meta", 41, 69));

             */
            
            SimpleBroker broker = new SimpleBroker(stocks);
            System.in.read();
            broker.stop();
        } catch (JMSException ex) {
            Logger.getLogger(JmsBrokerServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JmsBrokerServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
