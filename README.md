### Genel mimari
- **product-service (HTTP/REST, 8081)**: Müşteriden gelen indirim hesaplama isteğini alır. gRPC istemcisi olarak davranıp diğer servise sorar. Ürün/kategori veritabanı bu servistedir.
- **discount-service (gRPC, 9090 / Spring Boot 8082)**: İndirim kodu ve kategoriye göre indirim tutarını hesaplar. İndirim ve kategori veritabanı bu servistedir.
- **order-service (HTTP/REST, 8083)**: Sipariş oluşturur, toplam tutarı hesaplar ve RabbitMQ kuyruğuna sipariş oluşturuldu olayı yayınlar.
- **notification-service (HTTP/REST 8084)**: RabbitMQ `order.created` kuyruğunu dinler, e-posta bildirimi gönderir (dev: MailHog).
- **Veritabanları**: Her servis kendi Postgres’ine bağlıdır. `product-service` (54322), `discount-service` (54323), `order-service` (54324). `product-service` tarafındaki kategori ile `discount-service` tarafındaki kategori, `external_id` üzerinden eşleşir.

### Başlangıç (startup)
1) Her iki servis Spring Boot ile açılır, kendi Postgres’ine Hikari pool üzerinden bağlanır.
2) `discount-service` gRPC sunucusunu 9090 portunda yayımlar ve `DiscountService` RPC’sini kaydeder.
3) `product-service`, `application.properties`’ten gRPC host/port’u okur ve `ManagedChannel` + `BlockingStub` hazırlar.
4) Her iki DB’ye örnek veriler `data.sql` ile yüklenir (dev senaryosu).

### Sipariş bildirimi (RabbitMQ + E-posta)
- **order-service** bir sipariş oluşturduğunda `order.created` kuyruğuna bir mesaj yayınlar (RabbitMQ).
- **notification-service** bu kuyruğu dinler ve MailHog üzerinden kullanıcıya "siparişiniz oluşturuldu" e-postası gönderir.

RabbitMQ tasarımı (bu proje):
- Exchange tanımlamadık, varsayılan empty direct exchange kullanılır.
- Kuyruk: `order.created` (durable). Producer `convertAndSend(queueName, payload)` ile yayın yapar.
- Mesaj formatı: JSON (`Jackson2JsonMessageConverter`).

### Çalıştırma (root compose yok)
Bu projede her servis kendi compose/çalıştırma adımlarına sahiptir.

1) Notification stack (RabbitMQ + MailHog + notification-service)
```
cd notification-service
docker compose up --build
```
- RabbitMQ UI: `http://localhost:15672` (guest/guest)
- MailHog UI: `http://localhost:8025`

2) Order Postgres
```
cd ../order-service
docker compose up -d
```

3) Order-service’i çalıştır
```
./gradlew bootRun
# veya
./gradlew build && java -jar build/libs/order_service-0.0.1-SNAPSHOT.jar
```

4) (Opsiyonel) Discount ve Product servisleri
- Her servisin kendi `docker-compose.yml` dosyası Postgres’i ayağa kaldırır.
- Servisleri `./gradlew bootRun` ile çalıştırabilirsiniz.

5) Örnek sipariş oluşturma isteği (order-service)
```
POST http://localhost:8083/api/orders
Content-Type: application/json

{
  "customerId": 1,
  "customerEmail": "user@example.com",
  "items": [
    { "productId": 101, "unitPrice": 100.00, "quantity": 1 }
  ]
}
```

6) E-postayı doğrulama
- MailHog UI: `http://localhost:8025`
- "Your order #... has been created" başlıklı e-postayı göreceksiniz.

### İstek akışı (adım adım)
1) Müşteri, e-ticaret uygulamasında indirim kodu girer. UI/Backend, `product-service`’e HTTP isteği atar:
   - Endpoint: `POST /api/discounts/calculate`
   - Gövde: `code`, `price`, `externalCategoryId`

2) `product-service` içindeki `DiscountController`, gelen DTO’yu `Discount.proto`’ya uygun `DiscountRequest`’e çevirir.

3) `product-service` gRPC istemcisi, `DiscountService/getDiscount` RPC çağrısını `discount-service`’e yapar:
   - Kanal: `localhost:9090` (dev), prod’da servis DNS’i.
   - Çağrı tipi: blocking (senkron).

4) `discount-service` gRPC sunucusu isteği alır:
   - `externalCategoryId` ile kendi DB’sinde `Category` bulur.
   - İlgili kategoriye bağlı `Discount` kaydını (kuponu) `code` ile arar.
   - Bulursa: `newPrice = price - discountPrice` (0’ın altına düşmez).
   - Bulamazsa: `newPrice = price`, `response.statusCode=false`, `message="Invalid discount code"`.

5) `discount-service`, `DiscountResponse` döner; `product-service` yanıtı alır ve HTTP için DTO’ya çevirir.

6) `product-service` HTTP 200 ile sonuç döner:
   - `oldPrice`, `newPrice`, `code`, `response.statusCode`, `response.message`.

### Verinin anlamı ve nerede “düşüyor”?
- Bu akışta indirim “hesaplanır” ve sonuç döndürülür; ürün tablosundaki temel fiyat değiştirilmez.
- E-ticaret uygulamalarında indirim, genellikle sepette/checkout’ta geçici olarak uygulanır; sipariş fiyatı buna göre hesaplanır. Ürünün “liste fiyatı” sabit kalır.
- İhtiyaç varsa, sepet/checkout servisi bu yanıtı kullanarak satır kalemi tutarlarını günceller; kalıcı bir “order total” oluşturur.

### Hata durumları
- **Kategori bulunamazsa**: `discount-service` hata üretir (gRPC INTERNAL); `product-service` bu hatayı HTTP 5xx olarak yüzeye yansıtabilir.
- **Geçersiz kupon**: Başarılı HTTP/gRPC yanıtı gelir ama `statusCode=false` ve `newPrice=oldPrice` olur.
- **Bağlantı sorunu**: gRPC kanalında hata → `product-service` tarafında exception fırlar (şu an özel handling yok).

### Protokoller ve portlar
- HTTP/REST: `product-service` 8081, `order-service` 8083, `notification-service` 8084.
- gRPC: `discount-service` 9090. (Spring Boot 8082’de koşar.)
- RabbitMQ: 5672 (AMQP), 15672 (UI).
- MailHog: 1025 (SMTP), 8025 (UI).
- Veritabanları: `product-service` → 54322, `discount-service` → 54323, `order-service` → 54324.

### Mesaj şeması (özet)
- gRPC `DiscountRequest`: `code` (string), `price` (float), `externalCategoryId` (int64).
- gRPC `DiscountResponse`: `code`, `oldPrice`, `newPrice`, `response{statusCode(bool), message(string)}`.

### Örnek akış çıktısı
- İstek (HTTP):
```json
{"code":"SAVE10","price":100.0,"externalCategoryId":1}
```
- gRPC yanıtı (içeriden):
```json
{"code":"SAVE10","newPrice":90.0,"oldPrice":100.0,"response":{"statusCode":true,"message":"Discount applied successfully"}}
```
- HTTP yanıtı (müşteriye):
```json
{"code":"SAVE10","newPrice":90.0,"oldPrice":100.0,"response":{"statusCode":true,"message":"Discount applied successfully"}}
```

- Üst seviye akış:
  - **Client → product-service (HTTP)** → DTO → gRPC `DiscountRequest` →
  - **product-service → discount-service (gRPC)** → DB sorguları → indirim hesaplama →
  - **discount-service → product-service (gRPC yanıt)** → HTTP DTO →
  - **product-service → Client (HTTP yanıt)**.

- Bu mimaride indirim “uygulanmış fiyat” yanıt olarak döner; kalıcı fiyat değişikliği yapılmaz. Kalıcılaştırma gerekiyorsa, o iş mantığı sepet/checkout/sipariş servisinde yapılır.
