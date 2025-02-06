package org.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class VerveServiceApplication {
    private static final Logger logger = LoggerFactory.getLogger(VerveServiceApplication.class);
    private static final ConcurrentHashMap<Integer, Boolean> localUniqueRequests = new ConcurrentHashMap<>();
    private static final AtomicInteger uniqueCount = new AtomicInteger(0);
    private static final String KAFKA_TOPIC = "unique-requests";
    private static final Producer<String, String> kafkaProducer = createKafkaProducer();
    private static final Consumer<String, String> kafkaConsumer = createKafkaConsumer();
    private static final String LOG_FILE = "unique_requests.log";

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new VerveServlet()), "/api/verve/accept");
        startLoggingTask();
        startKafkaConsumer();

        server.start();
        logger.info("Server started on port 8080");
        server.join();
    }

    private static Producer<String, String> createKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        return new KafkaProducer<>(props);
    }

    private static Consumer<String, String> createKafkaConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "verve-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        return new KafkaConsumer<>(props);
    }

    private static void startKafkaConsumer() {
        new Thread(() -> {
            kafkaConsumer.subscribe(Collections.singletonList(KAFKA_TOPIC));
            while (true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(100);
                records.forEach(record -> localUniqueRequests.putIfAbsent(Integer.parseInt(record.value()), true));
            }
        }).start();
    }

    private static void logToFile(String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(message);
        } catch (IOException e) {
            logger.error("Error writing to log file", e);
        }
    }

    private static void startLoggingTask() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int count = uniqueCount.getAndSet(0);
                String logMessage = "Unique requests in last minute: " + count;
                logger.info(logMessage);
                logToFile(logMessage);
                kafkaProducer.send(new ProducerRecord<>(KAFKA_TOPIC, logMessage));
            }
        }, 60000, 60000);
    }

    public static class VerveServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            processRequest(req, resp, "GET");
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            processRequest(req, resp, "POST");
        }

        private void processRequest(HttpServletRequest req, HttpServletResponse resp, String method) throws IOException {
            String idParam = req.getParameter("id");
            String endpoint = req.getParameter("endpoint");

            if (idParam == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("failed");
                return;
            }

            try {
                int id = Integer.parseInt(idParam);
                kafkaProducer.send(new ProducerRecord<>(KAFKA_TOPIC, Integer.toString(id)));
                if (localUniqueRequests.putIfAbsent(id, true) == null) {
                    uniqueCount.incrementAndGet();
                }
                if (endpoint != null) {
                    sendHttpRequest(endpoint, uniqueCount.get(), method);
                }
                resp.getWriter().write("ok");
            } catch (Exception e) {
                logger.error("Error processing request", e);
                resp.getWriter().write("failed");
            }
        }

        private void sendHttpRequest(String endpoint, int count, String method) {
            try {
                URL url = new URL(endpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                if ("POST".equals(method)) {
                    connection.setDoOutput(true);
                    connection.getOutputStream().write(("{\"count\": " + count + "}").getBytes());
                }
                int responseCode = connection.getResponseCode();
                logger.info("Sent {} request to {} with count {} - Response Code: {}", method, endpoint, count, responseCode);
            } catch (Exception e) {
                logger.error("Error sending HTTP request to endpoint: " + endpoint, e);
            }
        }
    }
}
