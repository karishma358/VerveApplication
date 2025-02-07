Verve Service Application
This Java-based REST service can handle at least 10,000 requests per second. It processes incoming requests, logs unique request counts, interacts with external endpoints, and integrates with Kafka for distributed streaming and deduplication.

📦 Features
GET Endpoint

/api/verve/accept
Accepts:
id (integer, mandatory)
endpoint (string, optional)
Returns:

"ok" if processed successfully.
"failed" if errors occur.
Logging

Logs unique request counts every minute to a file.
External HTTP Requests

If the endpoint parameter is provided, the service fires an HTTP GET request with the current minute's unique count.
Extension 1: Option to fire a POST request instead, with customizable data.
Deduplication Behind Load Balancers

Extension 2: Uses Kafka-based deduplication to handle duplicate ids across multiple instances.
Kafka Integration

Extension 3: Sends the count of unique IDs to a Kafka topic (verve-unique-ids) every minute.
🚀 Prerequisites
Ensure the following are installed on your system:

Java 11+
Maven 3.6+
Docker and Docker Compose
🔧 Setting Up and Running the Application
1️⃣ Clone the Repository
bash
Copy
Edit
git clone https://github.com/your-username/verve-service-app.git
cd verve-service-app
2️⃣ Build the Application
Use Maven to build the application JAR file:

bash
Copy
Edit
mvn clean package
The compiled JAR will be located at target/verve-service-app-1.0-SNAPSHOT.jar.

3️⃣ Run the Application with Docker Compose
We use Docker Compose to manage the Java application, Kafka, and Zookeeper services.

Docker Setup
Dockerfile
Builds the Java application into a container.

dockerfile
Copy
Edit
FROM openjdk:11-jre-slim
COPY target/verve-service-app-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
docker-compose.yml
Sets up Zookeeper, Kafka, and the Verve Service Application.

yaml
Copy
Edit
version: '3'
services:
  zookeeper:
    image: wurstmeister/zookeeper:3.4.6
    ports:
      - "2181:2181"

  kafka:
    image: wurstmeister/kafka:2.12-2.3.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    depends_on:
      - zookeeper

  verve-service:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - kafka
4️⃣ Start the Services
Run the following command to start the services:

bash
Copy
Edit
docker-compose up --build
The Verve Service will be accessible at http://localhost:8080/api/verve/accept.
Kafka will be running on localhost:9092.
🧪 Testing the Application
1️⃣ Sending Requests
Basic GET request:

bash
Copy
Edit
curl "http://localhost:8080/api/verve/accept?id=123"
GET request with external endpoint:

bash
Copy
Edit
curl "http://localhost:8080/api/verve/accept?id=456&endpoint=http://example.com/receive"
Switch to POST requests (Extension 1):

To demonstrate POST instead of GET for external endpoints, modify the request behavior in the code or pass a query parameter like method=POST if implemented.

2️⃣ Verifying Logs
Check the application's logs for the count of unique requests:

bash
Copy
Edit
docker logs verve-service
You should see entries like:

sql
Copy
Edit
[INFO] Unique request count in the last minute: 5
3️⃣ Verifying Kafka Messages
Ensure Kafka is receiving the unique ID counts:

List Kafka topics:

bash
Copy
Edit
docker exec -it kafka kafka-topics --list --bootstrap-server kafka:9092
Consume messages from the verve-unique-ids topic:

bash
Copy
Edit
docker exec -it kafka kafka-console-consumer --bootstrap-server kafka:9092 --topic verve-unique-ids --from-beginning
You should see messages like:

Copy
Edit
5
7
10
🐳 Stopping and Cleaning Up
To stop all services:

bash
Copy
Edit
docker-compose down
