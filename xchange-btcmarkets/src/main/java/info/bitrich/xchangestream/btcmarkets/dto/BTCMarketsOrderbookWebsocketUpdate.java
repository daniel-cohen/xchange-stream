package info.bitrich.xchangestream.btcmarkets.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.knowm.xchange.btcmarkets.dto.marketdata.BTCMarketsOrderBook;
import org.knowm.xchange.utils.jackson.UnixTimestampDeserializer;

//import org.knowm.xchange.btcmarkets.dto.marketdata.BTCMarketsOrderBook;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;



public class BTCMarketsOrderbookWebsocketUpdate extends BTCMarketsWebsocketUpdate {

  private final List<BTCMarketsOrderBookLevel> bids;
  private final List<BTCMarketsOrderBookLevel> asks;
  
//  private final List<BigDecimal[]> bids;
//  private final List<BigDecimal[]> asks;
  
  private long marketId;
  private long snapshotId;
  
  // For now we don't need to return an orderbook object for EACH transaction. it's just a diff
  //private final BTCMarketsOrderBook orderBook;

  public BTCMarketsOrderbookWebsocketUpdate(
          @JsonProperty("currency") String currency,
          @JsonProperty("instrument") String instrument,
          @JsonProperty("timestamp") 
            //@JsonDeserialize(using = UnixTimestampDeserializer.class) 
                                    Date timestamp,
          @JsonProperty("marketId") long marketId,
          @JsonProperty("snapshotId") long snapshotId,
          @JsonProperty("bids") List<BTCMarketsOrderBookLevel> bids,
          @JsonProperty("asks") List<BTCMarketsOrderBookLevel> asks
          
          //@JsonProperty("bids") List<BigDecimal[]> bids,
          //@JsonProperty("asks") List<BigDecimal[]> asks
          
          
  ) {
      super(currency, instrument, timestamp);
      this.bids = bids;
      this.asks = asks;
      this.marketId = marketId;
      this.snapshotId = snapshotId;
  }
  
  public long getMarketId()
  {
    return marketId;
  }

  public void setMarketId(long marketId)
  {
    this.marketId = marketId;
  }

  public long getSnapshotId()
  {
    return snapshotId;
  }

  public void setSnapshotId(long snapshotId)
  {
    this.snapshotId = snapshotId;
  }

  public List<BTCMarketsOrderBookLevel> getBids()
  {
    return bids;
  }

  public List<BTCMarketsOrderBookLevel> getAsks()
  {
    return asks;
  }

//  public BTCMarketsOrderBook getOrderBook() {
//      return orderBook;
//  }
}