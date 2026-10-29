# Mendo Plant 🌿

E-Commerce / Marketplace de venta de plantas, desarrollado con **Java 17 + Spring Boot 3**.

## Stack

- Java 17, Spring Boot 3.3.4
- Spring Data JPA + H2 (en memoria, fácil de migrar a PostgreSQL — ver comentarios en `pom.xml` y `application.yml`)
- Spring Security 6 (Form Login por sesión, BCrypt)
- Thymeleaf + Bootstrap 5 (CDN)
- Lombok, Maven
- Manejo global de excepciones con `@ControllerAdvice`

## Cómo ejecutar

Requisitos: JDK 17+ y Maven (o el wrapper `./mvnw` si lo agregás con `mvn -N io.takari:maven:wrapper`).

```bash
mvn spring-boot:run
```

La aplicación queda disponible en **http://localhost:8080**.

## Variables de entorno (credenciales fuera del repo)

Todas las credenciales se leen **por variables de entorno**, nunca desde el repo.
Copiá `.env.example` a `.env` (queda ignorado por `.gitignore`) y exportá las
variables antes de arrancar:

| Variable | Para qué sirve |
|----------|----------------|
| `MYSQL_USER` / `MYSQL_PASSWORD` | Credenciales de MySQL (XAMPP). |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USER` / `MAIL_PASSWORD` / `MAIL_FROM` | SMTP para los códigos de verificación. Sin `MAIL_HOST` el código se imprime en consola. |
| `URL_PUBLICA` | URL pública de la app (la usa Mercado Pago para el webhook). |
| `MP_ACCESS_TOKEN` / `MP_SANDBOX` | Checkout Pro de Mercado Pago. Sin token cae al flujo simulado. |
| `ENVIO_TARIFA_PLANA` / `ENVIO_GRATIS_DESDE` | Tarifas de envío. |
| `FACTURACION_*` | Datos de facturación (proveedor, punto de venta, CUIT, etc.). |

Consola H2 (para inspeccionar la base en memoria): **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:mendoplantdb`
- Usuario: `sa` — Password: (vacío)

## Usuarios de prueba (cargados por `data.sql`)

| Rol   | Email                    | Password      |
|-------|--------------------------|---------------|
| Admin | admin@mendoplant.com     | Admin123!     |
| User  | usuario@mendoplant.com   | Usuario123!   |

## Estructura del proyecto

```
src/main/java/com/mendoplant/app/
 ├── config/          SecurityConfig
 ├── security/        UserPrincipal, CustomUserDetailsService
 ├── model/            Entidades JPA (User, Plant, Order, OrderItem, BlogPost, CareGuide,
 │                      Category, SiteConfig) y Enums (Role, Tamano, TipoLuz, TipoMaceta,
 │                      TipoSustrato, EstadoOrden)
 ├── dto/              DTOs de entrada para formularios (UserRegistrationDTO, PlantDTO, etc.)
 ├── repository/       Interfaces JpaRepository (+ JpaSpecificationExecutor para filtros)
 ├── service/          Interfaces de servicio
 ├── service/impl/     Implementaciones con la lógica de negocio
 ├── controller/       Controladores MVC (Home, Auth, Plant, Wishlist, Order, Blog, Admin)
 └── exception/        Excepciones de dominio + GlobalExceptionHandler

src/main/resources/
 ├── application.yml
 ├── data.sql           Datos iniciales (usuarios, categorías, plantas, blog, guías, config)
 ├── templates/          Vistas Thymeleaf organizadas por módulo
 └── static/css/
```

## Funcionalidades incluidas

- Registro, login/logout, cambio de contraseña.
- Catálogo de plantas con filtro avanzado (categoría, luz, tamaño, sustrato, rango de precio, texto).
- Publicación, edición y baja lógica de plantas por parte de usuarios autenticados (dueño o admin).
- Wishlist (agregar/quitar/listar).
- Compra con **Mercado Pago (Checkout Pro)**: reserva de stock, preferencia de pago,
  webhook de notificación y verificación manual; sin token configurado cae a compra simulada.
- Blog con slugs auto-generados y guías de cuidado (por planta o por categoría genérica).
- Panel de administración: categorías, usuarios (rol/estado), blog, guías de cuidado y
  contenido dinámico del Home / Términos y Condiciones.

## Notas de diseño

- `TipoMaceta` y `TipoSustrato` se modelaron como **Enum** (alternativa permitida en la
  consigna) por simplicidad; `Category`, en cambio, es una entidad con CRUD propio en el
  panel de administración, tal como se pidió explícitamente.
- La baja de una planta es lógica (`activo = false`), no se borra el registro, para preservar
  la integridad de órdenes históricas que la referencian.
- `SiteConfig` implementa el contenido dinámico del Home y los Términos como pares clave/valor,
  editables desde `/admin/contenido`.

## Mercado Pago (Checkout Pro)

La compra reserva stock creando la orden en `PENDIENTE` y redirige al checkout de
Mercado Pago. El cobro se confirma por **webhook** (`/api/pagos/notificaciones`) o por
**verificación a demanda** (botón "Verificar estado del pago" en la orden).

1. Creá una aplicación en https://www.mercadopago.com.ar/developers (con cuenta de
   prueba/sandbox para desarrollo).
2. Configurá las variables de entorno al ejecutar:

   ```bash
   set MP_ACCESS_TOKEN=TEST-XXXXXXXXXXXXXXXXXXXX
   set MP_SANDBOX=true
   set URL_PUBLICA=http://localhost:8080
   mvn spring-boot:run
   ```

   - `MP_SANDBOX=true` usa `sandbox_init_point` y las tarjetas de prueba de Mercado Pago.
   - `URL_PUBLICA` debe ser una URL **alcanzable desde internet**: Mercado Pago la usa
     para el webhook y para devolverte al sitio después de pagar. En desarrollo usá un
     túnel (ngrok/cloudflared) y apuntá ahí `URL_PUBLICA`.
3. Si **no** hay access token, el checkout cae al *flujo simulado* (compra sin pago real),
   útil para demos.
4. Estados: `PENDIENTE` (esperando cobro) → `COMPLETADO` (pago aprobado) o `CANCELADO`
   (pago rechazado/cancelado; el stock reservado se repone automáticamente).



## Migrar a PostgreSQL

1. Agregar la dependencia de `org.postgresql:postgresql` en `pom.xml` (ya está comentada ahí).
2. En `application.yml`, cambiar `datasource.url`, `driver-class-name` y el dialect de Hibernate
   (instrucciones detalladas en los comentarios de ese archivo).
3. Ajustar `ddl-auto` a `validate`/`none` y migrar el esquema con Flyway o Liquibase en un
   entorno productivo.
