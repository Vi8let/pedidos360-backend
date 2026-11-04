# Pedidos 360 - Backend API 📦🚀

Servicio Backend RESTful para el backoffice de **Pedidos 360**, desarrollado como parte de la asignatura **Desarrollo Cloud Native I (DSY1107)** en Duoc UC.

Este microservicio gestiona las operaciones CRUD del catálogo y estado de pedidos, persistiendo datos mediante Spring Data JPA y una base de datos en memoria H2, e integrándose con **Amazon Cognito** y **AWS API Gateway** para la autenticación y autorización basada en tokens JWT y Scopes OAuth 2.0.

---

## 🏗️ Arquitectura de la Solución

```
+------------------------------------+
|         Frontend Angular           |  (Puerto 4200 - Flujo PKCE)
+-----------------+------------------+
                  |
         1. Login | Redirección OAuth2
                  v
+-----------------+------------------+
|          Amazon Cognito            |  (IdP / CIAM - Emite Access e ID Tokens)
|  - User Pool: us-east-1_C1DQHPBOn  |
|  - Resource Server: rs-api-pedidos |
+-----------------+------------------+
                  |
                  | 2. Petición con Bearer <Access_Token>
                  v
+-----------------+------------------+
|         AWS API Gateway            |  (Authorizer Cognito + Scopes)
|  - GET    -> rs-api-pedidos/pedidos-read
|  - POST   -> rs-api-pedidos/pedidos-write
|  - PUT    -> rs-api-pedidos/pedidos-write
|  - DELETE -> rs-api-pedidos/pedidos-delete
+-----------------+------------------+
                  |
                  | 3. Tráfico autorizado / proxy HTTP
                  v
+-----------------+------------------+
|     Backend Spring Boot (8888)     |  (OAuth2 Resource Server + JPA + H2)
+------------------------------------+
```

---

## 🛠️ Stack Tecnológico

- **Java**: 17+
- **Framework**: Spring Boot 3.5.x
- **Seguridad**: Spring Security 6 con `spring-boot-starter-oauth2-resource-server` (validación de JWT emitida por AWS Cognito)
- **Persistencia**: Spring Data JPA / Hibernate
- **Base de Datos**: H2 Database (in-memory)
- **Gestor de Dependencias**: Apache Maven con Maven Wrapper (`mvnw`)

---

## 📂 Estructura del Proyecto

```
pedidos360-backend/
├── src/
│   ├── main/
│   │   ├── java/cl/duoc/pedidos360_backend/
│   │   │   ├── Pedidos360BackendApplication.java # Clase principal Spring Boot
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java           # Configuración de OAuth2, CORS y filtros
│   │   │   │   └── DataInitializer.java          # Carga de datos de prueba al iniciar
│   │   │   ├── controller/
│   │   │   │   └── PedidoController.java         # Controlador REST (/api/pedidos)
│   │   │   ├── model/
│   │   │   │   └── Pedido.java                   # Entidad JPA
│   │   │   ├── repository/
│   │   │   │   └── PedidoRepository.java         # Interfaz Spring Data JPA
│   │   │   ├── service/
│   │   │   │   └── PedidoService.java            # Lógica de negocio y validaciones
│   │   │   └── exception/
│   │   │       └── ResourceNotFoundException.java# Excepción para recursos no encontrados (404)
│   │   └── resources/
│   │       └── application.properties            # Configuración de puerto, H2 y Cognito
│   └── test/                                     # Pruebas unitarias y de integración
├── pom.xml                                       # Dependencias Maven
└── README.md                                     # Documentación del proyecto
```

---

## 📡 Endpoints de la API REST (`/api/pedidos`)

Todos los endpoints requieren la cabecera HTTP:
```http
Authorization: Bearer <ACCESS_TOKEN>
```

| Método | Endpoint | Scope Requerido | Descripción | Código Éxito |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/pedidos` | `rs-api-pedidos/pedidos-read` | Obtiene el listado completo de pedidos | `200 OK` |
| `GET` | `/api/pedidos/{id}` | `rs-api-pedidos/pedidos-read` | Obtiene el detalle de un pedido por su ID | `200 OK` (o `404`) |
| `POST` | `/api/pedidos` | `rs-api-pedidos/pedidos-write` | Crea un nuevo pedido en el sistema | `201 Created` (o `400`) |
| `PUT` | `/api/pedidos/{id}` | `rs-api-pedidos/pedidos-write` | Actualiza los datos de un pedido existente | `200 OK` (o `404`, `400`) |
| `DELETE` | `/api/pedidos/{id}` | `rs-api-pedidos/pedidos-delete` (o `write`) | Elimina un pedido por su ID | `204 No Content` (o `404`) |

### Ejemplos de Carga Útil (Payload)

#### `POST /api/pedidos` (Crear pedido)
```json
{
  "producto": "Monitor Gamer 27\"",
  "descripcion": "Monitor QHD 165Hz para puesto de diseño",
  "cantidad": 2,
  "stock": 10,
  "estado": "EN_PREPARACION",
  "imageUrl": "https://via.placeholder.com/150"
}
```

#### `PUT /api/pedidos/1` (Actualizar pedido)
```json
{
  "producto": "Notebook Pro Actualizado",
  "descripcion": "Notebook corporativo 32GB RAM",
  "cantidad": 1,
  "stock": 5,
  "estado": "DESPACHADO",
  "imageUrl": "https://via.placeholder.com/150"
}
```

---

## 🔐 Integración con AWS (Cognito + API Gateway)

### 1. ¿Cómo funcionan los Scopes en AWS Cognito?
En Amazon Cognito, un scope personalizado no es solo una palabra suelta; pertenece a un **Resource Server**. Por ello, el nombre calificado completo siempre tiene el formato:
```
<identificador-del-resource-server>/<nombre-del-scope>
```
En este proyecto:
- **Resource Server Identifier**: `rs-api-pedidos`
- **Custom Scopes**:
  - `rs-api-pedidos/pedidos-read`
  - `rs-api-pedidos/pedidos-write` (o subdivididos en `pedidos-create`, `pedidos-update`)
  - `rs-api-pedidos/pedidos-delete`

### 2. Configuración en la Consola de AWS paso a paso

#### Paso A: Crear o editar los Custom Scopes en Cognito
1. Ir a **Amazon Cognito** ➔ **User Pools** ➔ Seleccionar tu User Pool (`us-east-1_C1DQHPBOn`).
2. Menú lateral ➔ **Applications** ➔ **Resource Servers**.
3. Seleccionar el Resource Server `rs-api-pedidos` (o crearlo si no existe con ese identificador).
4. Agregar los scopes que faltaban:
   - Scope name: `pedidos-write` | Descripción: *Permite crear y modificar pedidos*
   - Scope name: `pedidos-delete` | Descripción: *Permite eliminar pedidos*
5. Guardar cambios.

#### Paso B: Habilitar los nuevos scopes en el App Client
1. Menú lateral ➔ **Applications** ➔ **App clients** ➔ Seleccionar tu App Client (ej. `5f24qerscjusiu3apprn79gbdc`).
2. Pestaña **Login pages** ➔ Botón **Edit**.
3. En la sección **Custom scopes**, marcar las casillas de:
   - `rs-api-pedidos/pedidos-read`
   - `rs-api-pedidos/pedidos-write`
   - `rs-api-pedidos/pedidos-delete`
4. Guardar cambios.

#### Paso C: Configurar la protección por método en AWS API Gateway
1. Ir a **API Gateway** ➔ Abrir tu API REST (`api-pedidos` o equivalente).
2. Asegurarte de tener creado el **Authorizer** de tipo **Cognito** apuntando al User Pool.
3. En el árbol de **Resources**:
   - En `GET /api/pedidos` y `GET /api/pedidos/{id}`:
     - **Method Request** ➔ Authorization: `Cognito Authorizer`.
     - **Authorization Scopes**: Agregar `rs-api-pedidos/pedidos-read`.
   - En `POST /api/pedidos`:
     - **Method Request** ➔ Authorization: `Cognito Authorizer`.
     - **Authorization Scopes**: Agregar `rs-api-pedidos/pedidos-write`.
   - En `PUT /api/pedidos/{id}`:
     - **Method Request** ➔ Authorization: `Cognito Authorizer`.
     - **Authorization Scopes**: Agregar `rs-api-pedidos/pedidos-write`.
   - En `DELETE /api/pedidos/{id}`:
     - **Method Request** ➔ Authorization: `Cognito Authorizer`.
     - **Authorization Scopes**: Agregar `rs-api-pedidos/pedidos-delete` (o `write`).
4. ⚠️ **MUY IMPORTANTE**: Hacer clic en **Deploy API** seleccionando el Stage correspondiente (ej. `test`). *Si no se hace deploy, los cambios de scopes no tendrán efecto y API Gateway seguirá con la configuración anterior*.

---

### 3. ¿Debe cambiarse algo en el código al agregar los Scopes?

Depende de la arquitectura de validación adoptada:

#### Caso 1: Validación delegada en AWS API Gateway (Por defecto actual)
API Gateway actúa como guardián de entrada. Si un token no tiene el scope configurado en el método (ej. `rs-api-pedidos/pedidos-write`), **API Gateway corta la petición con error 401/403** y el tráfico ni siquiera llega al backend Spring Boot.
En este caso, la configuración actual en [`SecurityConfig.java`](src/main/java/cl/duoc/pedidos360_backend/config/SecurityConfig.java):
```java
.requestMatchers("/api/pedidos", "/api/pedidos/**").authenticated()
```
Es suficiente, ya que valida que todo tráfico entrante posea un JWT legítimo firmado por el emisor de Cognito (`issuer-uri`).

#### Caso 2: Seguridad en Profundidad (Zero-Trust en Spring Boot)
Si deseas que el microservicio Spring Boot también compruebe los scopes de forma autónoma (por ejemplo, si se invoca el backend directamente sin pasar por API Gateway), solo debes descomentar en [`SecurityConfig.java`](src/main/java/cl/duoc/pedidos360_backend/config/SecurityConfig.java) las reglas granulares:
```java
.requestMatchers(HttpMethod.GET, "/api/pedidos", "/api/pedidos/**")
    .hasAnyAuthority("SCOPE_rs-api-pedidos/pedidos-read")
.requestMatchers(HttpMethod.POST, "/api/pedidos")
    .hasAnyAuthority("SCOPE_rs-api-pedidos/pedidos-write")
.requestMatchers(HttpMethod.PUT, "/api/pedidos/**")
    .hasAnyAuthority("SCOPE_rs-api-pedidos/pedidos-write")
.requestMatchers(HttpMethod.DELETE, "/api/pedidos/**")
    .hasAnyAuthority("SCOPE_rs-api-pedidos/pedidos-delete", "SCOPE_rs-api-pedidos/pedidos-write")
```
> **Nota técnica:** Spring Security antepone automáticamente el prefijo `SCOPE_` a cada valor presente en el claim `scope` del JWT decodificado.

---

### 4. ⚠️ Importante: ID Token vs Access Token

Este es el error más frecuente en el laboratorio:

- **ID Token (`token_use: id`)**: Transporta datos de identidad del usuario (`email`, `sub`, `cognito:groups`, `profile`). **NO contiene el claim `scope`**. Si API Gateway o Spring Boot exigen un scope y se envía un ID Token, la petición será rechazada con `401 Unauthorized` o `403 Forbidden`.
- **Access Token (`token_use: access`)**: Transporta permisos y contiene explícitamente el claim `scope` con los permisos otorgados (ej. `scope: openid email profile rs-api-pedidos/pedidos-read rs-api-pedidos/pedidos-write`). **Este es el token que SIEMPRE se debe enviar como Bearer cuando hay Custom Scopes configurados**.

#### ¿Qué cambiar en el Frontend (`pedidos360-front`)?
1. En `src/environments/environment.ts`, actualizar los scopes solicitados en el inicio de sesión:
   ```typescript
   scope: 'openid email profile rs-api-pedidos/pedidos-read rs-api-pedidos/pedidos-write rs-api-pedidos/pedidos-delete'
   ```
2. En `src/app/services/pedidos.service.ts`, cerciorarse de enviar el **Access Token** en la cabecera `Authorization`:
   ```typescript
   const token = this.authService.getAccessToken();
   ```

---

## 💻 Ejecución y Pruebas Locales

### Requisitos
- JDK 17 o superior instalado.
- Puerto `8888` libre en tu equipo.

### Compilar y Ejecutar
En la raíz del proyecto (`pedidos360-backend/pedidos360-backend`):

```bash
# Con Maven Wrapper (Windows PowerShell)
./mvnw clean spring-boot:run

# O con Maven del sistema
mvn clean spring-boot:run
```

La aplicación iniciará en `http://localhost:8888`.

### Base de Datos H2 y Consola Web
- **URL Consola H2**: [http://localhost:8888/h2-console](http://localhost:8888/h2-console)
- **JDBC URL**: `jdbc:h2:mem:pedidosdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- **User Name**: `sa`
- **Password**: *(dejar en blanco)*

Al arrancar, `DataInitializer.java` inserta automáticamente 3 pedidos de demostración iniciales (Notebook, Smartphone y Teclado Mecánico).

---

## 🧪 Pruebas con Postman o cURL

### Configurar OAuth 2.0 en Postman
1. **Type**: `OAuth 2.0`
2. **Grant Type**: `Authorization Code (With PKCE)`
3. **Callback URL**: `https://oauth.pstmn.io/v1/callback`
4. **Auth URL**: `https://<tu-dominio-cognito>.auth.us-east-1.amazoncognito.com/oauth2/authorize`
5. **Access Token URL**: `https://<tu-dominio-cognito>.auth.us-east-1.amazoncognito.com/oauth2/token`
6. **Client ID**: Tu App Client ID de Cognito.
7. **Client Secret**: *(vacío si es SPA / cliente público)*
8. **Scope**: `openid email profile rs-api-pedidos/pedidos-read rs-api-pedidos/pedidos-write`
9. **Get New Access Token** ➔ Iniciar sesión con un usuario del User Pool ➔ **Use Token**.
