package info.bitrich.xchangestream.btcmarkets.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.knowm.xchange.utils.jackson.BtcToSatoshi;
import org.knowm.xchange.utils.jackson.SatoshiToBtc;

import java.math.BigDecimal;


@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class BTCMarketsOrderBookLevel {
    
//  //All numbers, specifically for price and volume, must be converted to an integer for use in Trading API requests. The conversion is 100000000, or 1E8. 
    @JsonSerialize(using = BtcToSatoshi.class)
    @JsonDeserialize(using = SatoshiToBtc.class)
    public BigDecimal price;
    
    @JsonSerialize(using = BtcToSatoshi.class)
    @JsonDeserialize(using = SatoshiToBtc.class)
    public BigDecimal amount;
    
    public long streamId; //TODO: not sure what this ID actually is as it's undocumented

    public BTCMarketsOrderBookLevel() {
    }

    public BTCMarketsOrderBookLevel(BigDecimal price, BigDecimal amount, long streamId) {
        this.price = price;
        this.amount = amount;
        this.streamId = streamId;
    }

    // Price in units of 10e-8
    public BigDecimal getPrice() {
        return price;
    }

    // Amount in units of 10e-8
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
