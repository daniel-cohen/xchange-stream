package info.bitrich.xchangestream.btcmarkets.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BTCMarketsWebsocketUpdate{
  public BTCMarketsWebsocketUpdate(
      @JsonProperty("currency") String currency,
      @JsonProperty("instrument") String instrument,
      @JsonProperty("timestamp") long timestamp) {
    
  }
}
