# Elegan Data Layer

Esta es la carpeta física oficial de datos para la aplicación Elegan:
`app/src/data/`

## Estructura
- `model/Models.kt`: alias JSON crudos (`ProductJson`, `AnnouncementJson`), sesiones de PayPal (`PayPalOrderSession`) y modelos de flujo de compra/perfil/chat.
- `remote/ApiAutenticacionMovil.kt`: cliente Retrofit conectado a las 24 rutas bajo `/api/movil/` (`auth`, `products`, `announcements`, `cart`, `saved`, `usuario`, `orders`, `payments/paypal`, `messages`, `media`).
- `remote/MobileSession.kt`: gestión de sesión autenticada contra el backend (`EncryptedSharedPreferences` únicamente para el token JWT y la identidad de sesión).
- `repository/RepositorioCatalogo.kt` y `RepositorioAnuncios.kt`: consumen `GET /api/movil/products`, `GET /api/movil/products/{product_id}` y `GET /api/movil/announcements` directamente en JSON crudo sin almacenamiento local.
- `repository/RepositorioCarrito.kt`, `RepositorioGuardadosYLikes.kt`, `RepositorioPedidos.kt`, `RepositorioMensajesSoporte.kt` y `RepositorioMedia.kt`: sincronizan carrito, guardados, pagos PayPal (`create`/`capture`), historial de órdenes, mensajes de soporte y carga multimedia (`admin`/`superadmin`) con el backend.
- `repository/EleganRepository.kt`: fachada principal de repositorios sin almacenamiento local de catálogo.
