package common;


public class WatchMessage extends BrokerMessage {
    private String stockName;

    public WatchMessage(String clientName, String stockName) {
        super(Type.SYSTEM_REGISTER);
        this.stockName = stockName;
    }

    public String getStockName(){return stockName;}
}
