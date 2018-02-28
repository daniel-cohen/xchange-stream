package info.bitrich.xchangestream.btcmarkets;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.knowm.xchange.btcmarkets.BTCMarketsExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;

import io.reactivex.Completable;

public class BTCMarketStreamingExchange extends BTCMarketsExchange implements StreamingExchange
{
  private static final Logger LOG = LoggerFactory.getLogger(BTCMarketStreamingExchange.class);
  
  private static final String API_BASE_URI = "https://socket.btcmarkets.net";
  
  private SocketIOStreamingService streamingService;
  
  private BTCMarketsStreamingMarketDataService streamingMarketDataService;
  
  public BTCMarketStreamingExchange() {
    streamingService  = null;
    try
    {
      streamingService = new SocketIOStreamingService(API_BASE_URI);
    } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e)
    {
      // TODO handle errors
      LOG.error("TCMarketStreamingExchange() error", e);
    }
  }
  
  
  @Override
  protected void initServices() {
      super.initServices();
      streamingMarketDataService = new BTCMarketsStreamingMarketDataService(streamingService);
  }
  
  
  @Override
  public Completable connect(ProductSubscription... args) {
      return streamingService.connect();
  }

  @Override
  public Completable disconnect() {
      return streamingService.disconnect();
  }

  @Override
  public StreamingMarketDataService getStreamingMarketDataService() {
      return streamingMarketDataService;
  }

  @Override
  public boolean isAlive() {
      return this.streamingService.isSocketOpen();
  }


}
