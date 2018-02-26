package info.bitrich.xchangestream.btcmarkets.dto;

import java.util.Date;

import org.knowm.xchange.utils.jackson.UnixTimestampDeserializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class BTCMarketsWebsocketUpdate{
  private String currency;
  private String instrument;
  
  
  

  private Date timestamp;
  //private long timestamp;
  
  public BTCMarketsWebsocketUpdate(
      @JsonProperty("currency") String currency,
      @JsonProperty("instrument") String instrument,
      @JsonProperty("timestamp") 
      //@JsonDeserialize(using = UnixTimestampDeserializer.class) 
      Date timestamp)
  {
    this.currency = currency;
    this.instrument = instrument;
    this.timestamp = timestamp;
  }
  
  public String getCurrency()
  {
    return currency;
  }

  public void setCurrency(String currency)
  {
    this.currency = currency;
  }

  public String getInstrument()
  {
    return instrument;
  }

  public void setInstrument(String instrument)
  {
    this.instrument = instrument;
  }

  public Date getTimestamp()
  {
    return timestamp;
  }

  public void setTimestamp(Date timestamp)
  {
    this.timestamp = timestamp;
  }


  

}
