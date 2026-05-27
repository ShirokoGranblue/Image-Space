# Project Memory

- Backend uses Spring Boot 3.2, Java 21, Sa-Token auth, MyBatis-Plus, MinIO storage, and `Result<T>` API responses.
- Frontend uses Vue 3, Element Plus, Pinia, Vue Router, and `/api` axios calls that unwrap `Result` into `res.data`.
- All user-facing assistant messages in this repo should end each sentence with `喵~` or `喵!`.
- Current deployment uses Docker Compose with Nginx proxying frontend, backend, and MinIO routes.
