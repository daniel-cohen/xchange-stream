package info.bitrich.xchangestream.binance;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.knowm.xchange.binance.dto.marketdata.BinanceOrderbook;
import org.knowm.xchange.binance.service.BinanceMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.OrderBookUpdate;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import info.bitrich.xchangestream.binance.dto.DepthBinanceWebSocketTransaction;
import info.bitrich.xchangestream.binance.dto.FullDepthBinanceWebSocketTransaction;
import io.reactivex.Observable;

public class FullBinanceStreamingMarketDataService extends BinanceStreamingMarketDataService
{
  private static final Logger LOG = LoggerFactory.getLogger(FullBinanceStreamingMarketDataService.class);
  
  private final ObjectMapper mapper = new ObjectMapper();
  
  private final BinanceStreamingService service;
  private final Map<CurrencyPair, OrderBook> orderbooks = new ConcurrentHashMap<>();
  
  
  //TODO: or AtomicBoolean

  volatile boolean initialized = false;

  private BinanceMarketDataService marketDataService;
  
  public FullBinanceStreamingMarketDataService(BinanceStreamingService service, org.knowm.xchange.binance.service.BinanceMarketDataService marketDataService) {
    super(service);
    
    this.service = service;
    this.marketDataService = marketDataService;
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }
  

  @Override
  public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
    
    //Test with a simple timer:
        Observable<Long> cutoff = Observable.timer(10, TimeUnit.SECONDS);
    
          // #1 subscribe to the channel and buffer the events:
          Deque<DepthBinanceWebSocketTransaction> buffer = new LinkedList<DepthBinanceWebSocketTransaction>();  
          
    
          // unlike the base, we can return here just yet:
         //Observable<OrderBook> subscription = service.subscribeChannel(currencyPair, args)
          return service.subscribeChannel(currencyPair, args)

              
              .map((JsonNode s) -> {
                
                //TODO: we don't need to waste time on converting to BinanceOrderbook (interates through all the arrays):
                  FullDepthBinanceWebSocketTransaction transaction = mapper.readValue(s.toString(), FullDepthBinanceWebSocketTransaction.class);
                  
/*                  
                  
                  if (!initialized) {
                    // Just buffer it till we initialize it:
                    
                    //Check the sequence number:
                    DepthBinanceWebSocketTransaction prevTransaction = buffer.peekLast();
                    if (prevTransaction == null) {
                      //The queue is empty
                      buffer.addLast(transaction);
                    } else {
                      //Make sure the sequence numbers are correct:
                      long expectedUpdateId = prevTransaction.getOrderBook().lastUpdateId + 1; 
                      if (expectedUpdateId == transaction.getFirstUpdateId()) {
                        buffer.addLast(transaction);                        
                      } else {
                        throw new IllegalStateException(String.format("Expected update Id=%d and recieved %d.", expectedUpdateId, transaction.getFirstUpdateId()));
                      }
                    }
                    
                    
                 } else {
                   // Return full orderbook:
                   if(!buffer.isEmpty()) {
                     //merge all the data from the buffer into the order book:
                   } else {
                     // just need to add the current entry:
                   }
                   
                 }
*/                  
                  
                  
                  LOG.info("Received orderbook event from Binance");
                  
                  OrderBook currentOrderBook = orderbooks.computeIfAbsent(currencyPair, orderBook -> new OrderBook(null, new ArrayList<LimitOrder>(), new ArrayList<LimitOrder>()));

                  BinanceOrderbook ob = transaction.getOrderBook();
                  ob.bids.entrySet().stream().forEach(e -> {
                      currentOrderBook.update(new OrderBookUpdate(
                              OrderType.BID,
                              null,
                              currencyPair,
                              e.getKey(),
                              transaction.getEventTime(),
                              e.getValue()));
                  });
                  ob.asks.entrySet().stream().forEach(e -> {
                      currentOrderBook.update(new OrderBookUpdate(
                              OrderType.ASK,
                              null,
                              currencyPair,
                              e.getKey(),
                              transaction.getEventTime(),
                              e.getValue()));
                  });
                  return currentOrderBook;
              })
              
              //.skipUntil(cutoff); // we don't want to send events out till the orderbook is initialized properly.
              .skipUntil(InitializeOrderBook(currencyPair)); // we don't want to send events out till the orderbook is initialized properly.
            
            
            
            //After we subscribe, we have to initialize the order book:
            //InitializeOrderBook(currencyPair);
            
            //return subscription;
  }
  
  /*
  How to manage a local order book correctly
  Open a stream to wss://stream.binance.com:9443/ws/bnbbtc@depth
  Buffer the events you receive from the stream
  Get a depth snapshot from **https://www.binance.com/api/v1/depth?symbol=BNBBTC&limit=1000"
  Drop any event where u is <= lastUpdateId in the snapshot
  The first processed should have U <= lastUpdateId+1 AND u >= lastUpdateId+1
  While listening to the stream, each new event's U should be equal to the previous event's u+1
  The data in each event is the absolute quantity for a price level
  If the quantity is 0, remove the price level
  Receiving an event that removes a price level that is not in your local order book can happen and is normal.
 */
//  public Observable<Object> InitializeOrderBook(CurrencyPair currencyPair) throws IOException {
////    if (initialized) {
////      return;
////    }
//    
//    OrderBook currentOrderBook = orderbooks.computeIfAbsent(currencyPair, orderBook -> new OrderBook(null, new ArrayList<LimitOrder>(), new ArrayList<LimitOrder>()));
//    
//    
//    //To do, set limit
//    BinanceOrderbook ob = marketDataService.getBinanceOrderbook(currencyPair, null);
//    
//    //return ob.lastUpdateId;
//    return ob;
//    
//    
//    //TODO: I need the original lastUpdateId from the BinanceOrderbook
//
//    
//    //initialized = true;
//  }
  
  
  Observable<Long> InitializeOrderBook(CurrencyPair currencyPair) {
    
    return Observable.fromCallable(new Callable<Long>() {
      @Override
      public Long call() throws Exception {
        Thread.sleep(10000);        
         return 0L;
      }
  });
    
    
//    Observable<BinanceOrderbook> orderbookObservable = Observable.create(subscriber -> {
//      try {
//          LOG.info("InitializeOrderBook ");
//        
//          BinanceOrderbook ob = marketDataService.getBinanceOrderbook(currencyPair, null);
//          
//          LOG.info("ob.lastUpdateId= " + ob.lastUpdateId);
//          Thread.sleep(10000);
//          
//          subscriber.onNext(ob);
//          subscriber.onComplete();
//                      
//          LOG.info("Finished InitializeOrderBook ");
//                      
//
//      } catch(Exception e) {
//        //TODO: maybe try catch:                       
//        subscriber.onError(e);
//      }
//    });
  
//    return orderbookObservable;
  }
  
  


}

