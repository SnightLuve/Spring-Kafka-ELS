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
| `product-service` | `18082 -> 8082` | CRUD Product, luu SQL Server, publish event sang Kafka |
| `search-service` | `18083 -> 8083` | Search Product, consume Kafka event, index vao Elasticsearch, cache Redis |
| `apm-server` | `8200` | Nhan trace/metric tu 2 Java service |
| `kibana` | `5601` | Xem APM, logs, Elasticsearch data |
| `kafka-ui` | `8090` | Xem Kafka topic/message |
| `redis-commander` | `8081` | Xem Redis cache |

Khi chay file `microservices/docker-compose.yml`, cac port expose ra may host duoc doi sang dai rieng de co the chay song song voi stack goc:

| Service | Port trong container | Port tren may host |
| --- | --- | --- |
| SQL Server | `1433` | `11433` |
| Elasticsearch | `9200` | `19200` |
| Kibana | `5601` | `15601` |
| APM Server | `8200` | `18200` |
| Kafka | `9092` | `19092` |
| Kafka UI | `8080` | `18090` |
| Redis | `6379` | `16379` |
| Redis Commander | `8081` | `18081` |

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
SQL Server:     localhost:11433
Kafka:          localhost:19092
Elasticsearch:  http://localhost:19200
Redis:          localhost:16379
APM Server:     http://localhost:18200
```

Neu chay local bang Maven va dung infrastructure cua `microservices/docker-compose.yml`, set cac bien moi truong:

```powershell
$env:SQLSERVER_URL="jdbc:sqlserver://localhost:11433;databaseName=db;encrypt=true;trustServerCertificate=true"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:19092"
$env:ELASTICSEARCH_URIS="http://localhost:19200"
$env:ELASTICSEARCH_USERNAME="elastic"
$env:ELASTICSEARCH_PASSWORD="YourPassword@123"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="16379"
$env:ELASTIC_APM_SERVER_URLS="http://localhost:18200"
```

## Test API

Tao product:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:18082/api/products `
  -ContentType "application/json" `
  -Body '{"name":"Samsung Galaxy S24","description":"Android phone","price":25990000,"category":"Phone","brand":"Samsung"}'
```

Tim kiem product:

```powershell
Invoke-RestMethod "http://localhost:18083/api/search?keyword=samsung"
```

Goi y tim kiem:

```powershell
Invoke-RestMethod "http://localhost:18083/api/search/suggest?q=sam"
```

Neu Elasticsearch chua co data, goi reindex:

```powershell
Invoke-RestMethod -Method Post http://localhost:18082/api/products/reindex
```

## Xem APM

APM da duoc cau hinh bang Elastic APM auto-attach trong 2 main class:

```text
product-service/src/main/java/com/example/productservice/ProductServiceApplication.java
search-service/src/main/java/com/example/searchservice/SearchServiceApplication.java
```

Mo Kibana:

```text
http://localhost:15601
```

Dang nhap Kibana:

```text
Username: elastic
Password: YourPassword@123
```

Vao man hinh danh sach service APM:

```text
http://localhost:15601/app/apm/services
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

Neu ban mo trang APM setup/onboarding va thay nhieu loi `403 Forbidden` voi duong dan `/api/fleet/...`, do la phan Fleet-managed setup cua Kibana. Project nay dang dung standalone `apm-server`, khong dung Fleet-managed APM, nen hay xem trang Services sau khi da goi API tao trace:

```text
http://localhost:15601/app/apm/services
```

Trong `docker-compose.yml`, Kibana da duoc cau hinh de Fleet UI co the nap dung hon trong moi truong dev:

```yaml
XPACK_FLEET_AGENTS_ENABLED: "true"
XPACK_FLEET_ISAIRGAPPED: "true"
```

Neu van thay loi cu trong trinh duyet, hay recreate container va refresh lai tab Kibana:

```powershell
cd D:\ELS\ELS\microservices
docker compose down
docker compose up -d --build
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
ELASTIC_APM_SERVER_URLS=http://localhost:18200
ELASTIC_APM_ENVIRONMENT=local
ELASTIC_APM_TRANSACTION_SAMPLE_RATE=1.0
```

Neu chay `search-service` local voi Elasticsearch secure trong Docker Compose, set them:

```powershell
$env:ELASTICSEARCH_USERNAME="elastic"
$env:ELASTICSEARCH_PASSWORD="YourPassword@123"
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

### Kafka bao `Bootstrap broker localhost:19092 disconnected`

Kiem tra service dang chay o dau:

- Chay local bang Maven voi stack microservices: dung `localhost:19092`
- Chay trong Docker Compose: dung `kafka:29092`

Trong `docker-compose.yml` da cau hinh san:

```yaml
KAFKA_BOOTSTRAP_SERVERS: kafka:29092
```

### Elasticsearch bao `Connection closed by peer`

Dam bao Elasticsearch URI la:

```text
http://localhost:19200
```

Khong duoc goi HTTP vao Kafka port:

```text
http://localhost:19092
```

Kafka port `19092` tren may host chi danh cho Kafka client, khong phai HTTP.

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
Invoke-RestMethod http://localhost:18082/api/products
Invoke-RestMethod "http://localhost:18083/api/search?keyword=samsung"
```
