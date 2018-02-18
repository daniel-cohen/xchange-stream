package info.bitrich.xchangestream.binance.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FullDepthBinanceWebSocketTransaction extends DepthBinanceWebSocketTransaction
{
  long firstUpdateId;
  
  public long getFirstUpdateId()
  {
    return firstUpdateId;
  }

  public void setFirstUpdateId(long firstUpdateId)
  {
    this.firstUpdateId = firstUpdateId;
  }

  public FullDepthBinanceWebSocketTransaction(
          @JsonProperty("e") String eventType,
          @JsonProperty("E") String eventTime,
          @JsonProperty("s") String symbol,
          @JsonProperty("u") long lastUpdateId,
          @JsonProperty("U") long firstUpdateId,
          @JsonProperty("b") List<Object[]> _bids,
          @JsonProperty("a") List<Object[]> _asks
  ) {
      super(eventType, eventTime, symbol, lastUpdateId, _bids, _asks);
  }
}
