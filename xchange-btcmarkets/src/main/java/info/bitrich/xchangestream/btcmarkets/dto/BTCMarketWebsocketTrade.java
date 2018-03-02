package info.bitrich.xchangestream.btcmarkets.dto;

import java.math.BigDecimal;
import java.util.Date;

import org.knowm.xchange.utils.jackson.BtcToSatoshi;
import org.knowm.xchange.utils.jackson.SatoshiToBtc;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class BTCMarketWebsocketTrade
{
  public Date timestamp;
  
////All numbers, specifically for price and volume, must be converted to an integer for use in Trading API requests. The conversion is 100000000, or 1E8. 
  @JsonSerialize(using = BtcToSatoshi.class)
  @JsonDeserialize(using = SatoshiToBtc.class)
  public BigDecimal price;
  
  @JsonSerialize(using = BtcToSatoshi.class)
  @JsonDeserialize(using = SatoshiToBtc.class)
  public BigDecimal amount;
  
  /**
   * Trade ID:
   */
  public long tid;
 
  public BTCMarketWebsocketTrade() {
  }

  public BTCMarketWebsocketTrade(Date timestamp,  BigDecimal price, BigDecimal amount, long tid) {
      this.timestamp = timestamp;
      this.price = price;
      this.amount = amount;
      this.tid = tid;
  }

  

  
}
