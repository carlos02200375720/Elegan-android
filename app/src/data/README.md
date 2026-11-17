# Elegan Data Layer

Esta es la carpeta física oficial de datos para la aplicación Elegan:
`app/src/data/`

## Estructura
- `model/Models.kt`: entidades y modelos de datos.
- `repository/RepositorioCatalogo.kt`: consulta exclusivamente al backend (`GET /api/movil/products`) como única fuente de productos; sin catálogo local ni archivos locales de contenido.
- `repository/EleganRepository.kt`: repositorio principal sin almacenamiento local de catálogo ni caché persistente en dispositivo.
- `remote/MobileSession.kt`: gestión de sesión autenticada contra el backend (`EncryptedSharedPreferences` únicamente para el token JWT y la identidad de sesión).
