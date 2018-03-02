package info.bitrich.xchangestream.btcmarkets.dto;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BTCMarketWebsocketTradesUpdate extends BTCMarketsWebsocketUpdate
{
  private String agency;
  private List<BTCMarketWebsocketTrade> trades;
  private long id;
  private long marketId;
  
  public BTCMarketWebsocketTradesUpdate (
      @JsonProperty("agency") String agency,
      @JsonProperty("trades") List<BTCMarketWebsocketTrade> trades,
      @JsonProperty("instrument") String instrument,
      @JsonProperty("currency") String currency,
      
      //????
      @JsonProperty("id") long id,
      @JsonProperty("timestamp") Date timestamp,
      @JsonProperty("marketId") long marketId) {
    super(currency, instrument, timestamp);
    this.agency = agency;
    this.id = id;
    this.marketId = marketId;
    this.trades = trades;
  }
  
  public String getAgency()
  {
    return agency;
  }

  public void setAgency(String agency)
  {
    this.agency = agency;
  }

  public List<BTCMarketWebsocketTrade> getTrades()
  {
    return trades;
  }

  public void setTrades(List<BTCMarketWebsocketTrade> trades)
  {
    this.trades = trades;
  }

  public long getId()
  {
    return id;
  }

  public void setId(long id)
  {
    this.id = id;
  }

  public long getMarketId()
  {
    return marketId;
  }

  public void setMarketId(long marketId)
  {
    this.marketId = marketId;
  }
}