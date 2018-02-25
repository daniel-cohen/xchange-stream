package info.bitrich.xchangestream.btcmarkets.dto;

import java.util.List;

//import org.knowm.xchange.btcmarkets.dto.marketdata.BTCMarketsOrderBook;
import com.fasterxml.jackson.annotation.JsonProperty;



public class BTCMarketsOrderbookWebsocketUpdate extends BTCMarketsWebsocketUpdate {

  // For now we don't need to return an orderbook object for EACH transaction. it's just a diff
  //private final BTCMarketsOrderBook orderBook;

  public BTCMarketsOrderbookWebsocketUpdate(
          @JsonProperty("currency") String currency,
          @JsonProperty("instrument") String instrument,
          @JsonProperty("timestamp") long timestamp,
          @JsonProperty("marketId") long marketId,
          @JsonProperty("snapshotId") long snapshotId,
          @JsonProperty("bids") List<BTCMarketsOrderBookLevel> _bids,
          @JsonProperty("asks") List<BTCMarketsOrderBookLevel> _asks
  ) {
      super(currency, instrument, timestamp);
      //orderBook = new BTCMarketsOrderBook(timestamp, _bids, _asks);
  }

//  public BTCMarketsOrderBook getOrderBook() {
//      return orderBook;
//  }
}