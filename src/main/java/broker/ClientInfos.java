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
        this.broker = broker;

        this.stocks = new ArrayList<>();

        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                if(message instanceof ObjectMessage) {
                    BrokerMessage msg = ((ObjectMessage) message).getObject();  //TODO fix error
                    if(msg instanceof RequestListMessage) {
                        try {
                            ObjectMessage returnMessage = broker.session.createObjectMessage(new ListMessage(broker.getStocks()));
                            producer.send(returnMessage);
                        } catch (JMSException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else if(msg instanceof SellMessage){
                        sellingStockClient((SellMessage) msg);
                    }
                    else if(msg instanceof BuyMessage) {
                        buyingStockClient((BuyMessage) msg);
                    }
                }
            }
        });
    }

    private void sellingStockClient(SellMessage msg){
        BrokerMessage answerMsg;

        Stock stockToBeChanged = null;
        for (Stock s : stocks) {
            if (s.getName().equals(msg.getStockName())) {
                stockToBeChanged = s;
                break;
            }
        }
        if(stockToBeChanged == null) {
            answerMsg = new BrokerMessage(BrokerMessage.Type.SYSTEM_ERROR) {
            };
        }else{

            int result = 0;
            try {
                result = broker.sell(msg.getStockName(), msg.getAmount());
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }

            if(result == 1){
                answerMsg = msg;

                //update list of bought stocks
                Stock changedStock = null;
                for(Stock s : stocks){
                    if (s.getName().equals(msg.getStockName())){
                        changedStock = s;
                        break;
                    }
                }
                if(changedStock != null){
                    changedStock.setStockCount(changedStock.getStockCount() - msg.getAmount());
                }
            }else{
                answerMsg = new BrokerMessage(BrokerMessage.Type.SYSTEM_ERROR) {
                };
            }
        }

        //sending msg to client
        ObjectMessage returnMessage = null;
        try {
            returnMessage = broker.session.createObjectMessage(answerMsg);
            producer.send(returnMessage);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    private void buyingStockClient(BuyMessage msg){
        BrokerMessage answerMsg;

        //run buy function in broker and checking if it was successful or not
        try {
            if(broker.buy(((BuyMessage) msg).getStockName(), ((BuyMessage) msg).getAmount()) == 1){
                answerMsg = msg;

                //updates list of bought stocks by client or adds stock
                Stock changedStock = null;
                for(Stock s : stocks){
                    if (s.getName().equals(((BuyMessage) msg).getStockName())){
                        changedStock = s;
                        break;
                    }
                }
                if(changedStock != null){
                    changedStock.setStockCount(changedStock.getStockCount() + ((BuyMessage) msg).getAmount());
                }else{
                    stocks.add(new Stock(((BuyMessage) msg).getStockName(), ((BuyMessage) msg).getAmount(), 0.0));
                }

            }else{
                answerMsg = new BrokerMessage(BrokerMessage.Type.SYSTEM_ERROR) {
                };
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        //sending msg to client
        ObjectMessage returnMessage = null;
        try {
            returnMessage = broker.session.createObjectMessage(answerMsg);
            producer.send(returnMessage);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
