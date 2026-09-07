# ELS Microservices

This folder contains two new services created from the existing monolith without changing the original project.

## Services

- `product-service` runs on port `8082`.
  - Owns Product CRUD APIs.
  - Stores products in SQL Server.
  - Publishes product change events to Kafka.

- `search-service` runs on port `8083`.
  - Owns Search APIs.
  - Consumes product change events from Kafka.
  - Indexes products into Elasticsearch.
  - Uses Redis for search result caching.

## API

Product service:

```text
POST   http://localhost:8082/api/products
PUT    http://localhost:8082/api/products/{id}
DELETE http://localhost:8082/api/products/{id}
GET    http://localhost:8082/api/products/{id}
GET    http://localhost:8082/api/products
POST   http://localhost:8082/api/products/reindex
```

Search service:

```text
GET http://localhost:8083/api/search?keyword=samsung
GET http://localhost:8083/api/search/suggest?q=sam
```

## Build

Run from this `microservices` folder:

```bash
cd product-service
mvn clean package -DskipTests

cd ../search-service
mvn clean package -DskipTests
```

## Run With Docker Compose

Build the two services first, then run:

```bash
cd microservices
docker compose up -d --build
```

## Kafka Contract

Both services use the same JSON message shape on topic `product-sync-topic`:

```json
{
  "actionType": "CREATE",
  "productId": 1,
  "name": "Samsung Galaxy S24",
  "description": "Example description",
  "price": 25990000,
  "category": "Phone",
  "brand": "Samsung",
  "active": true
}
```

For deletes, only `actionType` and `productId` are required.
