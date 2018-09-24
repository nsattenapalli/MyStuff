package com.techsophy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.ConnectionLostException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.StopWatch;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.JettyXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;


public class ChatTest {

	private static Log logger = LogFactory.getLog(ChatTest.class);

	private static final int NUMBER_OF_USERS = 10000;

	private static final int BROADCAST_MESSAGE_COUNT = 10000;

	//Load for 10000 users and each user can send 100 messages.

	public static void main(String[] args) throws Exception {
		
		File file = new File("/home/nagraj/Documents/log.txt");
		FileWriter writer = new FileWriter(file);

		String host = "172.16.0.244";
		if (args.length > 0) {
			host = args[0];
		}

		int port = 8080;
		if (args.length > 1) {
			port = Integer.valueOf(args[1]);
		}


		final CountDownLatch connectLatch = new CountDownLatch(NUMBER_OF_USERS);
		final CountDownLatch subscribeLatch = new CountDownLatch(NUMBER_OF_USERS);
		final CountDownLatch messageLatch = new CountDownLatch(NUMBER_OF_USERS);
		final CountDownLatch disconnectLatch = new CountDownLatch(NUMBER_OF_USERS);

		final AtomicReference<Throwable> failure = new AtomicReference<>();

		StandardWebSocketClient webSocketClient = new StandardWebSocketClient();

		HttpClient jettyHttpClient = new HttpClient();
		jettyHttpClient.setMaxConnectionsPerDestination(1000);
		jettyHttpClient.setExecutor(new QueuedThreadPool(1000));
		jettyHttpClient.start();

		List<Transport> transports = new ArrayList<>();
		transports.add(new WebSocketTransport(webSocketClient));
		transports.add(new JettyXhrTransport(jettyHttpClient));

		SockJsClient sockJsClient = new SockJsClient(transports);

		try {
			ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
			taskScheduler.afterPropertiesSet();

			String stompUrl = "ws://172.16.0.244:8080/ws";
			WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
			stompClient.setMessageConverter(new StringMessageConverter());
			stompClient.setTaskScheduler(taskScheduler);
			stompClient.setDefaultHeartbeat(new long[] {0, 0});

			writer.write("Connecting and subscribing " + NUMBER_OF_USERS + " users ");
			writer.write("\n");
			StopWatch stopWatch = new StopWatch("STOMP Broker Relay WebSocket Load Tests");
			stopWatch.start();

			List<ConsumerStompSessionHandler> consumers = new ArrayList<>();
			for (int i=0; i < NUMBER_OF_USERS; i++) {
				consumers.add(new ConsumerStompSessionHandler(BROADCAST_MESSAGE_COUNT, connectLatch,
						subscribeLatch, messageLatch, disconnectLatch, failure));
				stompClient.connect(stompUrl, consumers.get(i), host, port);
			}

			stopWatch.stop();
			
			writer.write("Connecting Finished: " + stopWatch.getLastTaskTimeMillis() + " millis");
			writer.write("\n");
			writer.write("Broadcasting " + BROADCAST_MESSAGE_COUNT + " messages to " + NUMBER_OF_USERS + " users ");
			writer.write("\n");
			stopWatch.start();

			ProducerStompSessionHandler producer = new ProducerStompSessionHandler(BROADCAST_MESSAGE_COUNT, failure);
			stompClient.connect(stompUrl, producer, host, port);
			stompClient.setTaskScheduler(taskScheduler);

			writer.write("Sending Finished: " + stopWatch.getLastTaskTimeMillis() + " millis");
			writer.write("\n");
			producer.session.disconnect();
			
			stopWatch.stop();

			System.out.println("\nPress any key to exit...");
			System.in.read();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally {
			jettyHttpClient.stop();
		}
		writer.close();
		logger.debug("Exiting");
		System.exit(0);
	}


	private static class ConsumerStompSessionHandler extends StompSessionHandlerAdapter {

		private final int expectedMessageCount;

		private final CountDownLatch connectLatch;

		private final CountDownLatch subscribeLatch;

		private final CountDownLatch messageLatch;

		private final CountDownLatch disconnectLatch;

		private final AtomicReference<Throwable> failure;

		private AtomicInteger messageCount = new AtomicInteger(0);


		public ConsumerStompSessionHandler(int expectedMessageCount, CountDownLatch connectLatch,
				CountDownLatch subscribeLatch, CountDownLatch messageLatch, CountDownLatch disconnectLatch,
				AtomicReference<Throwable> failure) {

			this.expectedMessageCount = expectedMessageCount;
			this.connectLatch = connectLatch;
			this.subscribeLatch = subscribeLatch;
			this.messageLatch = messageLatch;
			this.disconnectLatch = disconnectLatch;
			this.failure = failure;
		}

		@Override
		public void afterConnected(final StompSession session, StompHeaders connectedHeaders) {
			this.connectLatch.countDown();
			session.setAutoReceipt(true);
			session.subscribe("/topic/public", new StompFrameHandler() {
				@Override
				public Type getPayloadType(StompHeaders headers) {
					return String.class;
				}

				@Override
				public void handleFrame(StompHeaders headers, Object payload) {
					if (messageCount.incrementAndGet() == expectedMessageCount) {
						messageLatch.countDown();
						disconnectLatch.countDown();
						session.disconnect();
					}
				}
			}).addReceiptTask(new Runnable() {
				@Override
				public void run() {
					subscribeLatch.countDown();
				}
			});
		}

		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			try {
				File file = new File("/home/nagraj/Documents/log.txt");
				FileWriter writer = new FileWriter(file);
				writer.write("Transport error"+ exception);
				writer.write("\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.failure.set(exception);
			if (exception instanceof ConnectionLostException) {
				this.disconnectLatch.countDown();
			}
		}

		@Override
		public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
			try {
				File file = new File("/home/nagraj/Documents/log.txt");
				FileWriter writer = new FileWriter(file);
				writer.write("Handling exception"+ ex);
				writer.write("\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.failure.set(ex);
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			Exception ex = new Exception(headers.toString());
			try {
				File file = new File("/home/nagraj/Documents/log.txt");
				FileWriter writer = new FileWriter(file);
				writer.write("STOMP ERROR frame"+ ex);
				writer.write("\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.failure.set(ex);
		}

		@Override
		public String toString() {
			return "ConsumerStompSessionHandler[messageCount=" + this.messageCount + "]";
		}
	}

	private static class ProducerStompSessionHandler extends StompSessionHandlerAdapter {

		private final int numberOfMessagesToBroadcast;

		private final AtomicReference<Throwable> failure;

		private StompSession session;


		public ProducerStompSessionHandler(int numberOfMessagesToBroadcast, AtomicReference<Throwable> failure) {
			this.numberOfMessagesToBroadcast = numberOfMessagesToBroadcast;
			this.failure = failure;
		}

		@Override
		public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
			this.session = session;
			int i =0;
			String message = "hello";
			try {
				File file = new File("/home/nagraj/Documents/log.txt");
				FileWriter writer = new FileWriter(file);
				for ( ; i < this.numberOfMessagesToBroadcast; i++) {
					session.send("/app/chat.sendMessage", message+i);
					writer.write("Message "+message+i);
					writer.write("\n");
					writer.close();
				}
			}
			catch (Throwable t) {
				try {
					File file = new File("/home/nagraj/Documents/log.txt");
					FileWriter writer = new FileWriter(file);
					writer.write("Message sending failed at " + i+ t);
					writer.write("\n");
					writer.close();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				failure.set(t);
			}
		}

		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			try {
				File file = new File("/home/nagraj/Documents/log.txt");
				FileWriter writer = new FileWriter(file);
				writer.write("Transport error"+ exception);
				writer.write("\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.failure.set(exception);
		}

		@Override
		public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
			try {
				File file = new File("/home/nagraj/Documents/log.txt");
				FileWriter writer = new FileWriter(file);
				writer.write("Handling exception"+ ex);
				writer.write("\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.failure.set(ex);
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			Exception ex = new Exception(headers.toString());
			try {
				File file = new File("/home/nagraj/Documents/log.txt");
				FileWriter writer = new FileWriter(file);
				writer.write("STOMP ERROR frame"+ ex);
				writer.write("\n");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.failure.set(ex);
		}
	}

}