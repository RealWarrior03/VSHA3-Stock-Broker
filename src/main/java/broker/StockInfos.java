package broker;

import javax.jms.*;
import common.*;

public class StockInfos extends Stock{
    Stock stock;
    MessageProducer producer;

    public StockInfos(Stock stock, MessageProducer producer) {
        super(stock.getName(), stock.getStockCount(), stock.getPrice());
        this.stock = stock;
        this.producer = producer;
    }
}
