package info.bitrich.xchangestream.btcmarkets;

import org.knowm.xchange.btcmarkets.BTCMarketsExchange;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Completable;

public class BTCMarketStreamingExchange extends BTCMarketsExchange implements StreamingExchange
{
  private static final String API_BASE_URI = "https://socket.btcmarkets.net";
  
  @Override
  public Completable connect(ProductSubscription... args)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Completable disconnect()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isAlive()
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public StreamingMarketDataService getStreamingMarketDataService()
  {
    // TODO Auto-generated method stub
    return null;
  }

}
