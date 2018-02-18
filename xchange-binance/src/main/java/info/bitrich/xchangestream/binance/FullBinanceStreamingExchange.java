package info.bitrich.xchangestream.binance;

import info.bitrich.xchangestream.core.StreamingMarketDataService;

public class FullBinanceStreamingExchange extends BinanceStreamingExchange
{
  private static final String API_BASE_URI = "wss://stream.binance.com:9443/ws/";
  private final BinanceStreamingService streamingService;

  
  private FullBinanceStreamingMarketDataService streamingMarketDataService;
  
  public FullBinanceStreamingExchange() {
    super();
    this.streamingService = new BinanceStreamingService(API_BASE_URI);
  }
  
  @Override
  protected void initServices() {
      super.initServices();
      streamingMarketDataService = new FullBinanceStreamingMarketDataService(streamingService, (org.knowm.xchange.binance.service.BinanceMarketDataService)marketDataService);
  }
  
  
  @Override
  public StreamingMarketDataService getStreamingMarketDataService() {
      return streamingMarketDataService;
  }
  
}
