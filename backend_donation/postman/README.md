# Pruebas del backend con Postman

Base URL: `http://localhost:8080`

## Importar

1. Postman → **Import** → selecciona `SemillasDeEsperanza.postman_collection.json`.
2. La colección ya trae la variable `baseUrl = http://localhost:8080`.
3. Ajusta `adminEmail` / `adminPassword` si tu administrador usa otras credenciales
   (por defecto `admin@semillas.org` / `Admin123`, configurables con las variables
   de entorno `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`).

## Orden recomendado

1. **01 - Autenticación → Login administrador**: guarda automáticamente el `token`.
2. **01 - Autenticación → Registrar usuario (público)**: guarda `registerEmail`.
3. **01 - Autenticación → Login usuario registrado**: guarda `usuarioToken`.
4. **02 - Usuarios → Crear usuario USUARIO** y **Crear administrador**.
5. El resto de requests de la carpeta 02 y 03.

También puedes ejecutar toda la colección con **Run collection** en ese mismo orden.
Los emails se generan con `Date.now()` en los pre-request scripts, así que la
colección se puede ejecutar varias veces sin chocar con el `409` de duplicados.

## Qué valida

- Registro público: `201`, rol forzado a `USUARIO`, cuenta activa, sin contraseña en la respuesta.
- Login: `200`, JWT y datos del usuario.
- `/api/auth/me`: `200` con token y `401` JSON sin token.
- Creación de cuentas por administrador: `201` con rol `USUARIO` o `ADMINISTRADOR`.
- Seguridad: `401` sin token, `403` con token de usuario no administrador.
- Errores: `409` email duplicado, `400` validaciones, `401` credenciales inválidas, `404` usuario inexistente.
