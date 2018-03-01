package info.bitrich.xchangestream.btcmarkets;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.bitrich.xchangestream.service.exception.NotConnectedException;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;

public class SocketIOStreamingService
{
  //TODO: Move :
  public class EventListener implements Emitter.Listener {
    private String eventName;
    private Emitter.Listener  listener;
    
    public EventListener (Emitter.Listener  listener, String eventName) {
      this.eventName = eventName;
      this.listener = listener;
    }
    
    public String getEventName() {
      return eventName;
    }
    
    public void call(Object... args) {
      listener.call(args);
    }
    
    public Emitter.Listener getListener(){
      return listener;
    }
    
    
  }
  
  public class SocketIOException extends Exception{

    public SocketIOException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
      super(message, cause, enableSuppression, writableStackTrace);
      // TODO Auto-generated constructor stub
    }

    public SocketIOException(String message, Throwable cause)
    {
      super(message, cause);
      // TODO Auto-generated constructor stub
    }

    public SocketIOException(String message)
    {
      super(message);
      // TODO Auto-generated constructor stub
    }

    public SocketIOException(Throwable cause)
    {
      super(cause);
      // TODO Auto-generated constructor stub
    }
  }
  
  private static final Logger LOG = LoggerFactory.getLogger(SocketIOStreamingService.class);
  
  private final Socket socket;
  private String serviceUrl;

  public SocketIOStreamingService(String serviceUrl) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
    this.serviceUrl = serviceUrl;
    
    X509TrustManager trustManager = getAllowAllTrustManager();
    
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] {trustManager },  new java.security.SecureRandom());
    
    //https://stackoverflow.com/questions/25509296/trusting-all-certificates-with-okhttp
    HostnameVerifier myHostnameVerifier = getAllAllowedHostnameVerifier();
    
    
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .hostnameVerifier(myHostnameVerifier)
        //.sslSocketFactory(sslContext.getSocketFactory(), getTrustManager())
        .sslSocketFactory(sslContext.getSocketFactory(), getAllowAllTrustManager())
        .build();

    // default settings for all sockets
    IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
    IO.setDefaultOkHttpCallFactory(okHttpClient);
    
    IO.Options opts = new IO.Options();
    opts.callFactory = okHttpClient;
    opts.webSocketFactory = okHttpClient;
    opts.secure = true;
    opts.transports = new String[] { "websocket" };

    this.socket = IO.socket(serviceUrl, opts);
  }
  
  public Completable connect() {
   
    Completable response =  Completable.create(e -> {
      socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
          @Override
          public void call(Object... args) {
            LOG.info("Connected to {}", serviceUrl);
  
            e.onComplete();
          }
      }).on(Socket.EVENT_ERROR, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Object arg = args[0];
          LOG.info("Socket.io Error while listening :{} arg:{}", serviceUrl,  (arg != null ? arg.toString() : ""));

          //todo: is this a string ?
          if (arg.getClass().isInstance(Throwable.class)) {
            e.onError(new SocketIOException((Throwable)arg));
          } else if (arg.getClass().isInstance(String.class)) {
              e.onError(new SocketIOException((String)arg));
          } else {
            e.onError(new SocketIOException(arg.toString()));
          }
          
        }
      }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Object arg = args[0];
          LOG.info("Error while trying to connect to:{} arg:{}", serviceUrl,  (arg != null ? arg.toString() : ""));

          if (arg.getClass().isInstance(Throwable.class)) {
            e.onError(new SocketIOException((Throwable)arg));
          } else if (arg.getClass().isInstance(String.class)) {
              e.onError(new SocketIOException((String)arg));
          } else {
            e.onError(new SocketIOException(arg.toString()));
          }
        }
      }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          //TODO: Maybe change the isLive state to false
          LOG.info("Received a disconnect event");
        }
      }).on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          //TODO: Maybe change the isLive state to false
          LOG.info("Received a timeout event");
        }
      }).on(Socket.EVENT_RECONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          //TODO: Maybe change the isLive state to false
          LOG.info("reonnected");
        }
      });
    });
    
    socket.connect();
    return response;
    
    //return response; 
}
  
  
  public Completable disconnect() {
    LOG.debug("SocketIOStreamingService.disconnect");
    return Completable.create(completable -> {
      socket.disconnect();
        completable.onComplete();
    });
  }
  
  /**
   * 
   * @param channelName
   * @param eventName E.g: OrderBookChange
   * @return
   */
  public Observable<String> subscribeChannel(String channelName, String eventName) {
    return subscribeChannel(channelName, Collections.singletonList(eventName));
  }
  
  public Observable<String> subscribeChannel(String channelName, List<String> eventsName) {
    
    List<EventListener> listeners = new LinkedList<EventListener>();
    return Observable.<String>create(subscriber -> 
          {
            if (socket.connected()) {
              //Only join the channel once:
              socket.emit("join", channelName);
              
              
              for(String eventName: eventsName){
                final Emitter.Listener listener =
                    args -> 
                          {
                            
                            if (args.length == 0) {
                              LOG.info("Event {} recieved with NO args", eventName);
                              // no payload:
                              subscriber.onNext(null);
                              
                            } else if (args.length > 0) {
                              LOG.info("Event {} recieved with args[0] of type: {} ", eventName, args[0].getClass().getName());
                              LOG.debug("Event {} recieved with args[0]: {} ", eventName, args[0].toString());
                             }

                            subscriber.onNext(args[0].toString());
                          };
                    
                socket.on(eventName, listener);
                
                listeners.add(new EventListener(listener, eventName));
              }
              
            } else {
              subscriber.onError(new NotConnectedException());
            }
      
          }).doOnDispose(() -> listeners.forEach(l -> {socket.off(l.getEventName(), l.getListener());}));
  }
  
  public boolean isSocketOpen() {
    return socket.connected();
  }
  
  /**
   * Testing constructor
   */
  protected SocketIOStreamingService(Socket socket) {
      this.socket = socket;
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
  
  private static HostnameVerifier getAllAllowedHostnameVerifier() {
    return new HostnameVerifier() {
      @Override
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    };
  }


}
