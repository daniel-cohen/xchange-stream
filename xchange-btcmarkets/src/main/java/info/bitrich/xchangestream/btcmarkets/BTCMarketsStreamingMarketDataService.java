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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.json.JSONObject;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.OrderBookUpdate;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;



import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.exception.NotConnectedException;
import io.reactivex.Observable;



import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;


public class BTCMarketsStreamingMarketDataService implements StreamingMarketDataService
{
  public enum SubscriptionType{
    ORDERBOOK,
    TICKER,
    TRADES
  }
  
  private static final Logger LOG = LoggerFactory.getLogger(BTCMarketsStreamingMarketDataService.class);
  
  private String serviceUrl;

  public BTCMarketsStreamingMarketDataService(String serviceUrl) {
    this.serviceUrl = serviceUrl; 
  }

  @Override
  public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Observable<Ticker> getTicker(CurrencyPair currencyPair, Object... args)
  {
    throw new NotYetImplementedForExchangeException();
  }

  @Override
  public Observable<Trade> getTrades(CurrencyPair currencyPair, Object... args)
  {
    throw new NotYetImplementedForExchangeException();
  }
  
  
  //TODO: make private:
  public static String channelFromCurrency(CurrencyPair currencyPair, SubscriptionType subscriptionType) {
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
  
//  private Observable<OrderBook> orderBookStream(CurrencyPair currencyPair) {
//    return service.subscribeChannel(channelFromCurrency(currencyPair, SubscriptionType.ORDERBOOK))
//            .map((JsonNode s) -> depthTransaction(s.toString()))
//            .filter(transaction ->
//                    transaction.getData().getCurrencyPair().equals(currencyPair) &&
//                            transaction.getData().getEventType() == DEPTH_UPDATE)
//            .map(transaction -> {
//                DepthBinanceWebSocketTransaction depth = transaction.getData();
//
//                OrderBook currentOrderBook = orderbooks.computeIfAbsent(currencyPair, orderBook ->
//                        new OrderBook(null, new ArrayList<>(), new ArrayList<>()));
//
//                BinanceOrderbook ob = depth.getOrderBook();
//                ob.bids.forEach((key, value) -> currentOrderBook.update(new OrderBookUpdate(
//                        OrderType.BID,
//                        null,
//                        currencyPair,
//                        key,
//                        depth.getEventTime(),
//                        value)));
//                ob.asks.forEach((key, value) -> currentOrderBook.update(new OrderBookUpdate(
//                        OrderType.ASK,
//                        null,
//                        currencyPair,
//                        key,
//                        depth.getEventTime(),
//                        value)));
//                return currentOrderBook;
//            });
//}
// 
  private static X509TrustManager getAllowAllTrustManager() {
    return new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
      {
        // TODO Auto-generated method stub
        
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
      {
        // TODO Auto-generated method stub
        
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
  Socket socket;
//  protected Map<String, Subscription> channels = new ConcurrentHashMap<>();
//  
//  public Observable<T> subscribeChannel(String channelName) {
//    LOG.info("Subscribing to channel {}", channelName);
//
//    return Observable.<T>create(e -> {
//        if (socket == null || !socket.connected()) {
//            e.onError(new NotConnectedException());
//        }
//
//        if (!channels.containsKey(channelId)) {
//            Subscription newSubscription = new Subscription(e, channelName, args);
//            channels.put(channelId, newSubscription);
//            try {
//              socket.emit("join", channelName);
//            } catch (IOException throwable) {
//                e.onError(throwable);
//            }
//        }
//    }).doOnDispose(() -> {
//        if (!channels.containsKey(channelId)) {
//            sendMessage(getUnsubscribeMessage(channelId));
//            channels.remove(channelId);
//        }
//    }).share();
//  }
//----------------------------------------------------------------------------------------------------------------------
  //EXAMPLE: https://github.com/tehmou/android-chat-client-example/blob/master/app/src/main/java/com/tehmou/book/androidchatclient/ChatModel.java
  public static Observable<String> createMessageListener(final Socket socket) {
    return Observable.create(subscriber -> {
        final Emitter.Listener listener =
                args -> subscriber.onNext(args[0].toString());
        socket.on("OrderBookChange", listener);
       
        //Do we need unsubscribe ?
//        subscriber.add(BooleanSubscription.create(
//                () -> {
//                    Log.d(TAG, "unsubscribe");
//                    socket.off("join", listener);
//                }));
    });
  }
//----------------------------------------------------------------------------------------------------------------------
  
 public Socket test(String channelName) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
   X509TrustManager trustManager = getAllowAllTrustManager();
   
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] {trustManager },  new java.security.SecureRandom());
    
    //https://stackoverflow.com/questions/25509296/trusting-all-certificates-with-okhttp
    
    //HostnameVerifier myHostnameVerifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier();
    HostnameVerifier myHostnameVerifier = GetAllAllowedHostnameVerifier();
    
    
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .hostnameVerifier(myHostnameVerifier)
        //.sslSocketFactory(sslContext.getSocketFactory(), getTrustManager())
        .sslSocketFactory(sslContext.getSocketFactory(), getAllowAllTrustManager())
        .build();

      // default settings for all sockets
      IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
      IO.setDefaultOkHttpCallFactory(okHttpClient);
    
      //TURN on extra logging:
      java.util.logging.Logger.getLogger(OkHttpClient.class.getName()).setLevel(java.util.logging.Level.FINE);
      
   // set as an option
    IO.Options opts = new IO.Options();
    opts.callFactory = okHttpClient;
    opts.webSocketFactory = okHttpClient;
    opts.secure = true;
    opts.transports = new String[] { "websocket" };
   
    
    
    Socket socket = IO.socket(serviceUrl, opts);
    
    
    socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

      @Override
      public void call(Object... args) {
        LOG.info("Connected");
        socket.emit("join", channelName);
        //socket.disconnect();
      }

//    }).on("OrderBookChange", new Emitter.Listener() {
//
//      @Override
//      public void call(Object... args) {
//        LOG.info("Got OrderBookChange");
//        JSONObject obj = (JSONObject)args[0];
//        
//      }

    }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

      @Override
      public void call(Object... args) {
        LOG.info("Disconnected");
      }

    }).on(Socket.EVENT_MESSAGE, new Emitter.Listener() {

      @Override
      public void call(Object... args) {
        LOG.info("Message");
      }

    });
    socket.connect();
    
    return socket;
  }

}
