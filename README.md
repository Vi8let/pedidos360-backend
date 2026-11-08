# Pedidos 360 - Backend Productor

> **Asignatura:** Desarrollo Cloud Native I (DSY1107) — Duoc UC  
> **Evaluaciones:** Evaluación Parcial 3 (EP3) y Evaluación Parcial 4 (EP4)  
> **Estado del Repositorio:** Backend Spring Boot con CRUD PostgreSQL, OAuth2 Resource Server, RBAC con AWS Cognito, Transactional Outbox y publicación asíncrona hacia RabbitMQ.

---

## 📖 Documentación Oficial del Proyecto

La documentación técnica exhaustiva de toda la solución (flujo End-to-End, seguridad Cognito, topología RabbitMQ, patrón Transactional Outbox, persistencia separada y guía de despliegue) se encuentra en:

👉 **[DOCUMENTACION.md](DOCUMENTACION.md)**

---

## 🚀 Resumen del Microservicio Productor (`pedidos360-backend`)

Microservicio Spring Boot principal encargado de la lógica transaccional de pedidos e inventario, control de acceso RBAC y publicación confiable de eventos:

- **Puerto:** `8888`
- **Base de Datos Operacional:** `pedidos360_db` en PostgreSQL 16 (tablas `pedidos` y `outbox_eventos`).
- **Seguridad:** OAuth 2.0 Resource Server validando JWT de **AWS Cognito** (`us-east-1_C1DQHPBOn`).
  - `GET /api/pedidos/**`: Usuarios autenticados.
  - `POST`, `PUT`, `DELETE /api/pedidos/**`: Exclusivo rol `ROLE_Admin` (Grupo Cognito `Admin`).
  - Manejo de respuestas `401 Unauthorized` y `403 Forbidden` con emisión automática de alertas de seguridad.
- **Transactional Outbox:** Garantiza entrega *At-Least-Once* persistiendo el pedido y su evento de auditoría en la misma transacción ACID. En bajas (`DELETE`), almacena el snapshot completo del producto.
- **Outbox Worker:** Publicación asíncrona periódica a RabbitMQ con Publisher Confirms (`correlated`).
- **Admin REST API:** Endpoints `POST /api/admin/queues` y `DELETE /api/admin/queues/{nombre}` para gestión dinámica del broker.

---

## 📂 Ecosistema de la Solución

| Proyecto / Carpeta | Rol | Puerto | Base de Datos |
|---|---|:---:|---|
| **`pedidos360-backend`** (este repo) | Microservicio Productor (Pedidos + Outbox) | `8888` | `pedidos360_db` |
| **`pedidos360-consumer`** | Microservicio Consumidor (Auditoría + DLQ) | `8889` | `auditoria_db` |
| **`pedidos360-front`** | Frontend SPA (Angular 17+ / OIDC) | `4200` | N/A |

---

## 🛠️ Comandos Rápidos

```bash
# Levantar PostgreSQL 16 y RabbitMQ 4
docker compose up -d

# Ejecutar las pruebas unitarias
./mvnw clean test

# Iniciar el backend productor
./mvnw spring-boot:run
```
