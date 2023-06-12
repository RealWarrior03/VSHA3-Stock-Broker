package broker;

import common.Stock;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import java.util.ArrayList;

public class ClientInfos {
    MessageConsumer consumer;
    MessageProducer producer;
    String clientName;
    ArrayList<Stock> stocks;

    public ClientInfos(MessageConsumer consumer, MessageProducer producer, String clientName) {
        this.consumer = consumer;
        this.producer = producer;
        this.clientName = clientName;
        this.stocks = new ArrayList<>();
    }
}
