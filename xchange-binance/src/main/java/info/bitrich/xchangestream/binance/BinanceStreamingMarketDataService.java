package info.bitrich.xchangestream.binance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.binance.dto.BinanceWebsocketTransaction;
import info.bitrich.xchangestream.binance.dto.DepthBinanceWebSocketTransaction;
import info.bitrich.xchangestream.binance.dto.TickerBinanceWebsocketTransaction;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import sun.rmi.runtime.Log;

import org.knowm.xchange.binance.dto.marketdata.BinanceOrderbook;
import org.knowm.xchange.binance.dto.marketdata.BinanceTicker24h;
import org.knowm.xchange.binance.service.BinanceMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;
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

import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static info.bitrich.xchangestream.binance.dto.BaseBinanceWebSocketTransaction.BinanceWebSocketTypes.*;

public class BinanceStreamingMarketDataService implements StreamingMarketDataService {
    private static final Logger LOG = LoggerFactory.getLogger(BinanceStreamingMarketDataService.class);

    private final BinanceStreamingService service;
    private final Map<CurrencyPair, OrderBook> orderbooks = new HashMap<>();
    //private final Map<CurrencyPair, OrderBook> orderbooks = new ConcurrentHashMap<>();

    private final Map<CurrencyPair, Observable<BinanceTicker24h>> tickerSubscriptions = new HashMap<>();
    private final Map<CurrencyPair, Observable<OrderBook>> orderbookSubscriptions = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    
    
    private final Map<CurrencyPair, LinkedList<DepthBinanceWebSocketTransaction>> orderbookDiffBuffers = new ConcurrentHashMap<>();
    
    private final Map<CurrencyPair, BinanceOrderbook> initialOrderbooks = new ConcurrentHashMap<>();
    

    private BinanceMarketDataService marketDataService;
    
    private BiFunction<BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>, CurrencyPair, OrderBook> transactionMapper;

    public BinanceStreamingMarketDataService(BinanceStreamingService service) {
      this(service, null);
    }

    public BinanceStreamingMarketDataService(BinanceStreamingService service, org.knowm.xchange.binance.service.BinanceMarketDataService marketDataService) {
        this.service = service;
        this.marketDataService = marketDataService;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (marketDataService == null) {
          // Set the mapping to simple mapping without initialization:
          transactionMapper =
              new BiFunction<BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>, CurrencyPair, OrderBook>() {
                    @Override
                    public OrderBook apply(BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction> transaction, CurrencyPair currencypair) throws IOException {
                        return uninitializedOrderbookMapFunction(transaction, currencypair);
                     }
              };
        } else {
          transactionMapper =
              new BiFunction<BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>, CurrencyPair, OrderBook>() {
                    @Override
                    public OrderBook apply(BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction> transaction, CurrencyPair currencypair) throws IOException {
                        return initializedOrderbookMapFunction(transaction, currencypair);
                     }
              };
        }
    }
    
    @Override
    public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
        if (!service.getProductSubscription().getOrderBook().contains(currencyPair)) {
            throw new UnsupportedOperationException("Binance exchange only supports up front subscriptions - subscribe at connect time");
        }
        return orderbookSubscriptions.get(currencyPair);
    }

    public Observable<BinanceTicker24h> getRawTicker(CurrencyPair currencyPair, Object... args) {
        if (!service.getProductSubscription().getTicker().contains(currencyPair)) {
            throw new UnsupportedOperationException("Binance exchange only supports up front subscriptions - subscribe at connect time");
        }
        return tickerSubscriptions.get(currencyPair);
    }

    @Override
    public Observable<Ticker> getTicker(CurrencyPair currencyPair, Object... args) {
        return getRawTicker(currencyPair)
                .map(BinanceTicker24h::toTicker);
    }

    @Override
    public Observable<Trade> getTrades(CurrencyPair currencyPair, Object... args) {
        throw new NotYetImplementedForExchangeException();
    }

    private static String channelFromCurrency(CurrencyPair currencyPair, String subscriptionType) {
        String currency = String.join("", currencyPair.toString().split("/")).toLowerCase();
        return currency + "@" + subscriptionType;
    }

    /**
     * Registers subsriptions with the streaming service for the given products.
     *
     * As we receive messages as soon as the connection is open, we need to register subscribers to handle these before the
     * first messages arrive.
     */
    public void openSubscriptions(ProductSubscription productSubscription) {
        productSubscription.getTicker()
                .forEach(currencyPair ->
                        tickerSubscriptions.put(currencyPair, triggerObservableBody(rawTickerStream(currencyPair).share())));
        productSubscription.getOrderBook()
                .forEach(currencyPair ->
                        orderbookSubscriptions.put(currencyPair, triggerObservableBody(orderBookStream(currencyPair).share())));
    }

    private Observable<BinanceTicker24h> rawTickerStream(CurrencyPair currencyPair) {
        return service.subscribeChannel(channelFromCurrency(currencyPair, "ticker"))
                .map((JsonNode s) -> tickerTransaction(s.toString()))
                .filter(transaction ->
                        transaction.getData().getCurrencyPair().equals(currencyPair) &&
                            transaction.getData().getEventType() == TICKER_24_HR)
                .map(transaction -> transaction.getData().getTicker());
    }

    private OrderBook uninitializedOrderbookMapFunction(BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction> transaction, CurrencyPair currencyPair) {
      DepthBinanceWebSocketTransaction depth = transaction.getData();

      OrderBook currentOrderBook = orderbooks.computeIfAbsent(currencyPair, orderBook ->
              new OrderBook(null, new ArrayList<>(), new ArrayList<>()));

      UpdateOrderBook(currentOrderBook, depth);
      
      return currentOrderBook;
    }
    
    private OrderBook initializedOrderbookMapFunction(BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction> transaction, CurrencyPair currencyPair) throws IOException {
      DepthBinanceWebSocketTransaction currentTransaction = transaction.getData();
      LOG.info("Mapping incoming transaction with: Currency:{} lastUpdateId:{}: ", currentTransaction.getCurrencyPair(), currentTransaction.getOrderBook().lastUpdateId);
      
      
      
      LinkedList<DepthBinanceWebSocketTransaction> buffer = orderbookDiffBuffers.computeIfAbsent(currencyPair, buff -> new LinkedList<DepthBinanceWebSocketTransaction>());
      //TODO: 1 - (ALWAYS) Check that the sequence number matches the last one:
      //TODO: 2- Check init:
      //boolean isOrderbookInialized = false;
      BinanceOrderbook initialOrderbook = initialOrderbooks.get(currencyPair);
      
      if (initialOrderbook != null) {
        DepthBinanceWebSocketTransaction prevTransaction = buffer.pollFirst();
        
        if (prevTransaction == null) {
          LOG.info("Buffer is empty: Currency:{} lastUpdateId:{}: ", currentTransaction.getCurrencyPair(), currentTransaction.getOrderBook().lastUpdateId);
          
          //The buffer is empty (everything is inialized, just need to process the current transaction):
          OrderBook currentOrderBook = orderbooks.computeIfAbsent(currencyPair, orderBook ->
          new OrderBook(null, new ArrayList<>(), new ArrayList<>()));

          UpdateOrderBook(currentOrderBook, currentTransaction);
          return currentOrderBook;
        }
        
        //the buffer is not empty so this is the first time we hit this code after we've fetched the full order book:
        
        // The initial order book should be set to the one we've fetched earlier in the initialization step.
        // we just don't have a timestamp for it:
        OrderBook currentOrderBook = BinanceOrderBookToOrderBook(initialOrderbook, currencyPair);

        
        //make sure the orderbook is empty:
        if (orderbooks.put(currencyPair, currentOrderBook) != null) {
        //TODO: Is this the right exception to throw ?
          String error = "A possible race confition detected while initializing the order book";
          LOG.error(error);
          throw new IOException(error);
        }
        
        
        LOG.info("Buffer has {} transactions.", buffer.size());
        
        // Now process the buffered websocket data in the buffer (and empty the buffer):
        while(prevTransaction != null) {
          if (prevTransaction.getOrderBook().lastUpdateId > initialOrderbook.lastUpdateId){
            LOG.info("Updating orderbook with transaction with: Currency:{} lastUpdateId:{}: ", prevTransaction.getCurrencyPair(), prevTransaction.getOrderBook().lastUpdateId);
            UpdateOrderBook(currentOrderBook, prevTransaction);
            
          } else {
            LOG.info("Dropping transaction with: Currency:{} lastUpdateId:{}: ", prevTransaction.getCurrencyPair(), prevTransaction.getOrderBook().lastUpdateId);
          }

          prevTransaction = buffer.pollFirst();
        }
        
        
        // and process the current transaction
        UpdateOrderBook(currentOrderBook, currentTransaction);
        
        return currentOrderBook;
        
      } else {
        LOG.info("Buffering transaction with: Currency:{} lastUpdateId:{}", currentTransaction.getCurrencyPair(), currentTransaction.getOrderBook().lastUpdateId);
        // Just buffer
        buffer.addLast(currentTransaction);
        
        return new OrderBook(null, null, null); // No orderbook while buffering
      }
    }
    
    private BiFunction<
                        AbstractMap.SimpleEntry<BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>, BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>>, 
                        BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>, 
                        AbstractMap.SimpleEntry<BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>, BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>>
                      > getPairAccumulator()
    {
      return new BiFunction<
          AbstractMap.SimpleEntry<BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>, BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>>, 
          BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>, 
          AbstractMap.SimpleEntry<BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>, BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>>
        >() {
        @Override
        public SimpleEntry<BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>, BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>> apply(
            SimpleEntry<BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>, BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>> t1,
            BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction> t2) throws Exception
        {
          // TODO Auto-generated method stub
          return new SimpleEntry<>(t1.getValue(), t2);
        }
      };
    }
    
    private Observable<OrderBook> orderBookStream(CurrencyPair currencyPair) {
        return service.subscribeChannel(channelFromCurrency(currencyPair, "depth"))
                .map((JsonNode s) -> depthTransaction(s.toString()))
                .filter(transaction ->
                        transaction.getData().getCurrencyPair().equals(currencyPair) &&
                                transaction.getData().getEventType() == DEPTH_UPDATE)
                
                .scan( 
                    //() -> new AbstractMap.SimpleEntry<BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>, BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>>(null, null), 
                    new AbstractMap.SimpleEntry<BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>, BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>>(null, null), //1. initial value
                    getPairAccumulator()
                    )
                .filter(pair -> pair != null && pair.getValue() != null) // Make sure we don't emit the initial value (for some reason scan does)
                .doOnNext(pair -> {
                  if (pair.getKey() == null) {
                    //LOG.info("NULL");
                    LOG.info("Curr:{} Prev:NULL", pair.getValue().getData().getOrderBook().lastUpdateId);                  
                  } else {
                    DepthBinanceWebSocketTransaction currOrderBook = pair.getValue().getData();
                    DepthBinanceWebSocketTransaction prevOrderBook = pair.getKey().getData();
                    
                    LOG.info("Curr:{} Prev:{}", currOrderBook.getOrderBook().lastUpdateId, prevOrderBook.getOrderBook().lastUpdateId);
                    
                    // make sure: While listening to the stream, each new event's U should be equal to the previous event's u+1
                    if (currOrderBook.firstUpdateId != prevOrderBook.getOrderBook().lastUpdateId + 1) {
                      throw new ExchangeException("CurrencyPair: " + currencyPair + " Received 2 sequencial events with mismatching Final update ID and First update ID.");
                    }
                  }
                  
                })
                .map(pair -> transactionMapper.apply(pair.getValue(), currencyPair))
                //TODO: doesn't work. the orderbook function is called 1st without waiting for one event to come through 1st
                //.skip(1) // Skipping at least one event so I know for sure we're not making the full order book request prematurely  
                .skipUntil(InitializeOrderBook(currencyPair));
    }



    private void UpdateOrderBook(OrderBook currentOrderBook, DepthBinanceWebSocketTransaction depth) {
      CurrencyPair currencyPair = depth.getCurrencyPair();
      
      BinanceOrderbook ob = depth.getOrderBook();
      ob.bids.forEach((key, value) -> currentOrderBook.update(new OrderBookUpdate(
          OrderType.BID,
          null,
          currencyPair,
          key,
          depth.getEventTime(),
          value)));
      ob.asks.forEach((key, value) -> currentOrderBook.update(new OrderBookUpdate(
          OrderType.ASK,
          null,
          currencyPair,
          key,
          depth.getEventTime(),
          value)));
    }
    
    OrderBook BinanceOrderBookToOrderBook(BinanceOrderbook ob, CurrencyPair currencyPair) {
      
      List<LimitOrder> asks = ob.asks.entrySet().stream().map(kv -> new LimitOrder(OrderType.ASK, kv.getValue(), currencyPair, "", null, kv.getKey()))
      .collect(Collectors.toList());
      
      List<LimitOrder> bids = ob.bids.entrySet().stream().map(kv -> new LimitOrder(OrderType.BID, kv.getValue(), currencyPair, "", null, kv.getKey()))
          .collect(Collectors.toList());
      
      return new OrderBook(null, asks, bids);
    }
    
    /** Force observable to execute its body, this way we get `BinanceStreamingService` to register the observables emitter
     * ready for our message arrivals. */
    private <T> Observable<T> triggerObservableBody(Observable<T> observable) {
        Consumer<T> NOOP = whatever -> {};
        observable.subscribe(NOOP);
        return observable;
    }

    private BinanceWebsocketTransaction<TickerBinanceWebsocketTransaction> tickerTransaction(String s) {
        try {
            return mapper.readValue(s, new TypeReference<BinanceWebsocketTransaction<TickerBinanceWebsocketTransaction>>() {});
        } catch (IOException e) {
            throw new ExchangeException("Unable to parse ticker transaction", e);
        }
    }

    private BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction> depthTransaction(String s) {
        try {
            return mapper.readValue(s, new TypeReference<BinanceWebsocketTransaction<DepthBinanceWebSocketTransaction>>() {});
        } catch (IOException e) {
          throw new ExchangeException("Unable to parse order book transaction", e);
        }
    }
    
    private Observable<Long> InitializeOrderBook(CurrencyPair currencyPair) {
      if (marketDataService == null) {
        // Don't skip anything:
        return Observable.just(1L);
      }
      
      // Skip (don't emit, but buffer) events till we fetch the entire order book 
      Observable<Long> oSkip = Observable.fromCallable(new Callable<Long>() {
        @Override
        public Long call() throws Exception {
          //TODO: I wanted to skip at least one event BEFORE making the call to fetch the orderbook (.skip(1)) but I can't get it to work.
          // So, for now, we'll sleep for 3 seconds
          LOG.warn("Sleeping for 3 seconds before making full order book request:");
          Thread.sleep(3000);
          
          LOG.info("Making reuqest for full orderbook:");
          long startTime = System.nanoTime();
          BinanceOrderbook ob = marketDataService.getBinanceOrderbook(currencyPair, null);
          
          if (initialOrderbooks.put(currencyPair, ob) != null) {
            String error = "Concurrency Error. Initial orderbook for currencyPair:" + currencyPair + " has alredy been set.";
            LOG.error(error);
            throw new IOException(error);
          }
          //TODO when setting the time stamp, use: BinanceBaseService.getTimestamp() which takes into account the delta between our system and their server
          
          long endTime = System.nanoTime();
          
          //LOG.info(String.format("Received orderbook with last update id: %d Duration %dms", ob.lastUpdateId, (endTime - startTime)/1000000 ));
          LOG.info("Received orderbook with CurrencyPaid:{} last update id: {} Duration {}ms", currencyPair, ob.lastUpdateId, (endTime - startTime)/1000000 );
          return 0L;
        }
      }).subscribeOn(Schedulers.io());
      
      return oSkip;
    }

}
