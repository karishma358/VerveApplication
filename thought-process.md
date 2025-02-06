# Thought Process for Verve Service Application
## Overview
The goal was to build a high-performance Java REST service capable of processing at least 10,000 requests per second. The service needed to handle both GET and POST requests, ensure deduplication of IDs even behind a load balancer, and log unique requests either to a file or a Kafka topic. Below is my approach to implementing each of the requirements and extensions.
## Core Requirements
### 1. High-Performance REST Service

I chose Eclipse Jetty as the embedded server due to its lightweight nature and ability to handle high concurrency.
The server listens on port 8080 and handles requests through a custom servlet, ensuring efficient request processing.

### 2. GET Endpoint - /api/verve/accept

The servlet accepts an integer id as a mandatory query parameter and an optional endpoint string.
If the request processes without errors, it returns "ok"; otherwise, it returns "failed".

### 3. Logging Unique Requests

I used a ConcurrentHashMap to store unique IDs for thread-safe operations and quick lookups.
A TimerTask runs every minute to log the count of unique requests to both a log file and Kafka.

### 4. HTTP Requests to Provided Endpoints

If an endpoint is provided, the service fires an HTTP GET request to it with the current minute’s unique request count.
The HTTP status code of the response is logged for tracking.
## Extensions

### Extension 1: Support for POST Requests

I modified the servlet to handle both GET and POST requests using the doGet and doPost methods.
For POST requests, the unique request count is sent as a JSON payload instead of a query parameter.

### Extension 2: Deduplication Behind a Load Balancer

To ensure deduplication across multiple instances, I integrated Kafka.
Every incoming ID is published to a Kafka topic (unique-requests), and each instance consumes from this topic to maintain a consistent view of unique IDs.
This approach ensures that even if the same ID hits different instances, it is only counted once.

### Extension 3: Logging to a Distributed Streaming Service

In addition to logging unique request counts to a file, I also send this data to a Kafka topic.
This demonstrates the ability to log both locally and to a distributed system, catering to different logging needs.

## Design Considerations

### Concurrency

Using ConcurrentHashMap ensures thread-safe storage of unique IDs.
AtomicInteger is used for maintaining the count of unique requests, avoiding synchronization issues.

### Scalability

Kafka helps in scaling the deduplication logic across multiple instances behind a load balancer.
Jetty’s non-blocking IO capabilities ensure that the application can handle a high volume of requests efficiently.

### Error Handling

Comprehensive error handling is implemented to ensure that failures in processing requests or sending HTTP requests are logged appropriately.

## Conclusion

By combining Jetty for high-performance request handling, Kafka for distributed deduplication and logging, and robust error handling mechanisms, the Verve Service Application meets all the core requirements and extensions. The system is designed to be scalable, reliable, and efficient, capable of handling large volumes of requests while maintaining accurate logging and deduplication.