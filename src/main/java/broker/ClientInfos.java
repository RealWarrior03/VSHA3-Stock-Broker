package broker;

import common.*;
import javax.jms.*;
import java.util.ArrayList;

public class ClientInfos {
    MessageConsumer consumer;
    MessageProducer producer;
    String clientName;
    ArrayList<Stock> stocks;
    SimpleBroker broker;

    public ClientInfos(MessageConsumer consumer, MessageProducer producer, String clientName, SimpleBroker broker) {
        this.consumer = consumer;
        this.producer = producer;
        this.clientName = clientName;
        this.stocks = new ArrayList<>();
        this.broker = broker;

        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) { //TODO functionality
                if(message instanceof ObjectMessage) {
                    BrokerMessage msg = ((ObjectMessage) message).getObject();
                    if(msg instanceof RequestListMessage) {
                        try {
                            ObjectMessage returnMessage = broker.session.createObjectMessage(new ListMessage(broker.getStocks()));
                            producer.send(returnMessage);
                        } catch (JMSException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else if(msg instanceof SellMessage){

                    }
                    else if(msg instanceof BuyMessage) {
                        broker.buy();

                    }
                }
            }
        });
    }
}
