package info.bitrich.xchangestream.btcmarkets;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.json.JSONObject;
import org.knowm.xchange.btcmarkets.BTCMarketsAdapters;
import org.knowm.xchange.btcmarkets.dto.marketdata.BTCMarketsOrderBook;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.OrderBookUpdate;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import info.bitrich.xchangestream.btcmarkets.BTCMarketsStreamingMarketDataService.SubscriptionType;
import info.bitrich.xchangestream.btcmarkets.dto.BTCMarketWebsocketTrade;
import info.bitrich.xchangestream.btcmarkets.dto.BTCMarketWebsocketTradesUpdate;
import info.bitrich.xchangestream.btcmarkets.dto.BTCMarketsOrderbookWebsocketUpdate;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.exception.NotConnectedException;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;


public class BTCMarketsStreamingMarketDataService implements StreamingMarketDataService
{
  private static final Logger LOG = LoggerFactory.getLogger(BTCMarketsStreamingMarketDataService.class);
  private SocketIOStreamingService service; 
  private final ObjectMapper mapper = new ObjectMapper();
  
  private final String ORDERBOOK_CHANGE_EVENT_NAME = "OrderBookChange";
  private final String MARKET_TRADE_EVENT_NAME = "MarketTrade";
  
  public BTCMarketsStreamingMarketDataService(SocketIOStreamingService service) {
    this.service= service; 
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  
  public enum SubscriptionType{
    ORDERBOOK,
    TICKER,
    TRADES
  }
  
  
  
  //private String serviceUrl;


   @Override
  public Observable<Ticker> getTicker(CurrencyPair currencyPair, Object... args)
  {
    throw new NotYetImplementedForExchangeException();
  }

  @Override
  public Observable<Trade> getTrades(CurrencyPair currencyPair, Object... args)
  {
    String channelName = channelFromCurrency(currencyPair, SubscriptionType.TRADES);
    return service.subscribeChannel(channelName, MARKET_TRADE_EVENT_NAME)
    .map(s-> tradeEvent(s))
    .filter(s -> 
    {    
      LOG.debug("s.getCurrency()={} s.getInstrument()={} currencyPair.counter.getCurrencyCode()={} currencyPair.base.getCurrencyCode()={}", s.getCurrency(),s.getInstrument(), currencyPair.counter.getCurrencyCode(),currencyPair.base.getCurrencyCode());
                  return s.getCurrency().equalsIgnoreCase(currencyPair.counter.getCurrencyCode())  
                  && s.getInstrument().equalsIgnoreCase(currencyPair.base.getCurrencyCode());
    })
    //.flatMap(trade -> Observable.from(adaptTrade(trade, currencyPair)));
    .flatMapIterable(trade -> adaptTrade(trade, currencyPair));
     
  }
  
  
  
  @Override
  public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args)  {
    String channelName = channelFromCurrency(currencyPair, SubscriptionType.ORDERBOOK);
    return service.subscribeChannel(channelName, ORDERBOOK_CHANGE_EVENT_NAME)
    .map(s-> orderbookTransaction(s))
    .filter(s -> 
    {    
      LOG.debug("s.getCurrency()={} s.getInstrument()={} currencyPair.counter.getCurrencyCode()={} currencyPair.base.getCurrencyCode()={}", s.getCurrency(),s.getInstrument(), currencyPair.counter.getCurrencyCode(),currencyPair.base.getCurrencyCode());
                  return s.getCurrency().equalsIgnoreCase(currencyPair.counter.getCurrencyCode())  
                  && s.getInstrument().equalsIgnoreCase(currencyPair.base.getCurrencyCode());
    })
    .map(orderbookTransaction -> adaptOrderBook(orderbookTransaction, currencyPair));
  }
  
  
  //TODO: make private:
  private String channelFromCurrency(CurrencyPair currencyPair, SubscriptionType subscriptionType) {
    String base = currencyPair.base.toString().toUpperCase();
    String counter = currencyPair.counter.toString().toUpperCase();
    //String currency = String.join("", currencyPair.toString().split("/")).toUpperCase();
    switch(subscriptionType) {
      case ORDERBOOK:
        return "Orderbook_" + base + counter;
      case TICKER:
        return " Ticker-BTCMarkets-" + base + "-" + counter;
      case TRADES:
        return "TRADE_" + base + counter;
        default:
          throw new UnsupportedOperationException(subscriptionType.toString());
    }
  }
  
  private static X509TrustManager getAllowAllTrustManager() {
    return new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
      {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
      {
      }

      @Override
      public X509Certificate[] getAcceptedIssuers()
      {
        // TODO Auto-generated method stub
        return new X509Certificate[0];
      }
    };
  }
  
  
  private static X509TrustManager getTrustManager() {
    TrustManagerFactory trustManagerFactory = null;
    try {
      trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init((KeyStore) null);

      TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
      if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
        //throw new IllegalStateException(
        //    "Unexpected default trust managers:" + Arrays.toString(trustManagers));
        return null;
      }
      return (X509TrustManager) trustManagers[0];
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return null;
    } catch (KeyStoreException e) {
      e.printStackTrace();
      return null;
    }    
 }
  
  
  HostnameVerifier GetAllAllowedHostnameVerifier() {
    return new HostnameVerifier() {
      @Override
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
      };
  }

//----------------------------------------------------------------------------------------------------------------------
  private BTCMarketsOrderbookWebsocketUpdate orderbookTransaction(String s) {
    try {
        return mapper.readValue(s, new TypeReference<BTCMarketsOrderbookWebsocketUpdate>() {});
    } catch (IOException e) {
      throw new ExchangeException("Unable to parse order book transaction", e);
    }
  }
  //----------------------------------------------------------------------------------------------------------------------
  public BTCMarketWebsocketTradesUpdate tradeEvent(String s) {
    try {
        return mapper.readValue(s, new TypeReference<BTCMarketWebsocketTradesUpdate>() {});
    } catch (IOException e) {
      throw new ExchangeException("Unable to parse order trade  event", e);
    }
  }  
//----------------------------------------------------------------------------------------------------------------------
  public static OrderBook adaptOrderBook(BTCMarketsOrderbookWebsocketUpdate btcmarketsOrderBookUpdate, CurrencyPair currencyPair) {
   List<LimitOrder> asks = btcmarketsOrderBookUpdate.getAsks().stream()
             .map(a -> new LimitOrder(Order.OrderType.ASK, a.amount, currencyPair, null, null, a.price)).collect(Collectors.toList());
       
       //createOrders(Order.OrderType.ASK, btcmarketsOrderBookUpdate.getAsks(), currencyPair);
   List<LimitOrder> bids = btcmarketsOrderBookUpdate.getBids().stream()
       .map(a -> new LimitOrder(Order.OrderType.BID, a.amount, currencyPair, null, null, a.price)).collect(Collectors.toList());
       //createOrders(Order.OrderType.BID, btcmarketsOrderBookUpdate.getBids(), currencyPair);
   Collections.sort(bids, BTCMarketsAdapters.BID_COMPARATOR);
   Collections.sort(asks, BTCMarketsAdapters.ASK_COMPARATOR);
   return new OrderBook(btcmarketsOrderBookUpdate.getTimestamp(), asks, bids);
 }
  
  public static List<Trade> adaptTrade(BTCMarketWebsocketTradesUpdate btcmarketsTrade, CurrencyPair currencyPair) {
    
    //TODO: why do I need the type ?
    return btcmarketsTrade.getTrades().stream().map
        (t -> new Trade(OrderType.BID, t.amount, currencyPair, t.price, t.timestamp, String.valueOf(t.tid))).collect(Collectors.toList());     
  }

}
