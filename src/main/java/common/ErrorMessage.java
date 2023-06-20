package common;

public class ErrorMessage extends BrokerMessage {
    private String stockName;
    private int amount;

    public ErrorMessage(Type type, String stockName, int amount) {
        super(type);
        this.stockName = stockName;
        this.amount = amount;
    }

    public String getStockName() {
        return stockName;
    }

    public int getAmount() {
        return amount;
    }
}
