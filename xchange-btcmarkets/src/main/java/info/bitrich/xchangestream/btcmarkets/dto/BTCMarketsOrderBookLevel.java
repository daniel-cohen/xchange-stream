package info.bitrich.xchangestream.btcmarkets.dto;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;


@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class BTCMarketsOrderBookLevel {
    public BigDecimal price;
    public BigDecimal amount;
    public long streamId; //TODO: not sure what this ID actually is as it's undocumented

    public BTCMarketsOrderBookLevel() {
    }

    public BTCMarketsOrderBookLevel(BigDecimal price, BigDecimal amount, long streamId) {
        this.price = price;
        this.amount = amount;
        this.streamId = streamId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public long getStreamId() {
        return streamId;
    }

//    public BitfinexLevel toBitfinexLevel() {
//        // Xchange-bitfinex adapter expects the timestamp to be seconds since Epoch.
//        return new BitfinexLevel(price, amount, new BigDecimal(System.currentTimeMillis() / 1000));
//    }
}
