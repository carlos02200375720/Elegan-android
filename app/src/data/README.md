# Elegan Data Layer

Esta es la carpeta física oficial de datos para la aplicación Elegan:
`app/src/data/`

## Estructura
- `model/Models.kt`: entidades y modelos de datos.
- `repository/EleganRepository.kt`: repositorio principal con persistencia 100% local en el dispositivo (`SharedPreferences` y almacenamiento interno de archivos multimedia).
- `remote/MobileSession.kt`: gestión de sesión y autenticación local cifrada en el dispositivo mediante AES-256.
