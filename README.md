# ELS Microservices

Thu muc nay chua 2 service moi duoc tach tu project ELS ban dau. Project goc van duoc giu nguyen, khong bi sua truc tiep.

## Kien Truc

```text
Client
  -> product-service -> SQL Server -> Kafka topic product-sync-topic
  -> search-service  -> Redis / Elasticsearch

Kafka topic product-sync-topic
  -> search-service consumer -> Elasticsearch index products
```

## Cac Service

| Service | Port | Vai tro |
| --- | --- | --- |
| `product-service` | `8082` | CRUD Product, luu SQL Server, publish event sang Kafka |
| `search-service` | `8083` | Search Product, consume Kafka event, index vao Elasticsearch, cache Redis |
| `apm-server` | `8200` | Nhan trace/metric tu 2 Java service |
| `kibana` | `5601` | Xem APM, logs, Elasticsearch data |
| `kafka-ui` | `8090` | Xem Kafka topic/message |
| `redis-commander` | `8081` | Xem Redis cache |

## Yeu Cau

Can cai san:

```text
Java 17
Maven
Docker Desktop
```

Kiem tra nhanh:

```powershell
java -version
mvn -version
docker version
```

## Build 2 Service

Build `product-service`:

```powershell
cd D:\ELS\ELS\microservices\product-service
mvn clean package -DskipTests
```

Build `search-service`:

```powershell
cd D:\ELS\ELS\microservices\search-service
mvn clean package -DskipTests
```

Sau khi build thanh cong se co file jar:

```text
D:\ELS\ELS\microservices\product-service\target\product-service-0.0.1-SNAPSHOT.jar
D:\ELS\ELS\microservices\search-service\target\search-service-0.0.1-SNAPSHOT.jar
```

## Chay Bang Docker Compose

Chay toan bo infrastructure va 2 service:

```powershell
cd D:\ELS\ELS\microservices
docker compose up -d --build
```

Xem container:

```powershell
docker compose ps
```

Xem log:

```powershell
docker compose logs -f product-service
docker compose logs -f search-service
docker compose logs -f apm-server
```

Dung toan bo:

```powershell
docker compose down
```

## Chay Local De Dev

Neu muon chay 2 service bang Maven tren may local, truoc tien chi chay infrastructure:

```powershell
cd D:\ELS\ELS\microservices
docker compose up -d sqlserver kafka elasticsearch kibana redis apm-server kafka-ui redis-commander
```

Terminal 1, chay `product-service`:

```powershell
cd D:\ELS\ELS\microservices\product-service
mvn spring-boot:run
```

Terminal 2, chay `search-service`:

```powershell
cd D:\ELS\ELS\microservices\search-service
mvn spring-boot:run
```

Khi chay local, config mac dinh dang dung:

```text
SQL Server:     localhost:1433
Kafka:          localhost:9092
Elasticsearch:  http://localhost:9200
Redis:          localhost:6379
APM Server:     http://localhost:8200
```

## Test API

Tao product:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8082/api/products `
  -ContentType "application/json" `
  -Body '{"name":"Samsung Galaxy S24","description":"Android phone","price":25990000,"category":"Phone","brand":"Samsung"}'
```

Tim kiem product:

```powershell
Invoke-RestMethod "http://localhost:8083/api/search?keyword=samsung"
```

Goi y tim kiem:

```powershell
Invoke-RestMethod "http://localhost:8083/api/search/suggest?q=sam"
```

Neu Elasticsearch chua co data, goi reindex:

```powershell
Invoke-RestMethod -Method Post http://localhost:8082/api/products/reindex
```

## Xem APM

APM da duoc cau hinh bang Elastic APM auto-attach trong 2 main class:

```text
product-service/src/main/java/com/example/productservice/ProductServiceApplication.java
search-service/src/main/java/com/example/searchservice/SearchServiceApplication.java
```

Mo Kibana:

```text
http://localhost:5601
```

Vao man hinh APM:

```text
http://localhost:5601/app/apm
```

Hoac trong Kibana chon:

```text
Observability -> APM -> Services
```

Sau khi goi API vai lan, ban se thay 2 service:

```text
product-service
search-service
```

Trong APM co the xem:

```text
Transaction: request REST API vao product-service/search-service
Span: SQL query, Kafka publish/consume, Elasticsearch request, Redis call
Latency: thoi gian xu ly tung endpoint
Errors: exception neu service loi
Service map: quan he giua cac service
```

## APM Environment Variables

Trong Docker Compose, 2 service da co san:

```yaml
ELASTIC_APM_SERVICE_NAME: product-service
ELASTIC_APM_SERVER_URLS: http://apm-server:8200
ELASTIC_APM_ENVIRONMENT: docker
ELASTIC_APM_TRANSACTION_SAMPLE_RATE: "1.0"
```

Voi `search-service`:

```yaml
ELASTIC_APM_SERVICE_NAME: search-service
ELASTIC_APM_SERVER_URLS: http://apm-server:8200
ELASTIC_APM_ENVIRONMENT: docker
ELASTIC_APM_TRANSACTION_SAMPLE_RATE: "1.0"
```

Khi chay local, service tu dung mac dinh:

```text
ELASTIC_APM_SERVER_URLS=http://localhost:8200
ELASTIC_APM_ENVIRONMENT=local
ELASTIC_APM_TRANSACTION_SAMPLE_RATE=1.0
```

## Kafka Contract

Ca 2 service dung chung JSON message tren topic `product-sync-topic`:

```json
{
  "actionType": "CREATE",
  "productId": 1,
  "name": "Samsung Galaxy S24",
  "description": "Android phone",
  "price": 25990000,
  "category": "Phone",
  "brand": "Samsung",
  "active": true
}
```

Voi DELETE, chi can:

```json
{
  "actionType": "DELETE",
  "productId": 1
}
```

## Loi Thuong Gap

### Kafka bao `Bootstrap broker localhost:9092 disconnected`

Kiem tra service dang chay o dau:

- Chay local bang Maven: dung `localhost:9092`
- Chay trong Docker Compose: dung `kafka:29092`

Trong `docker-compose.yml` da cau hinh san:

```yaml
KAFKA_BOOTSTRAP_SERVERS: kafka:29092
```

### Elasticsearch bao `Connection closed by peer`

Dam bao Elasticsearch URI la:

```text
http://localhost:9200
```

Khong duoc goi HTTP vao Kafka port:

```text
http://localhost:9092
```

Kafka port `9092` chi danh cho Kafka client, khong phai HTTP.

### Khong thay service trong Kibana APM

Thu cac buoc:

```powershell
docker compose ps
docker compose logs -f apm-server
docker compose logs -f product-service
docker compose logs -f search-service
```

Sau do goi API vai lan de tao trace:

```powershell
Invoke-RestMethod http://localhost:8082/api/products
Invoke-RestMethod "http://localhost:8083/api/search?keyword=samsung"
```
