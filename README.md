# 🌱 Semillas de Esperanza

Sistema web desarrollado con **Angular**, **Spring Boot** y **MySQL**.

## 📋 Descripción

Semillas de Esperanza es una plataforma web de donaciones orientada al apoyo social y comunitario, facilitando la recaudación de fondos y el seguimiento transparente de campañas y proyectos.

## 🛠️ Tecnologías utilizadas

- **Frontend:** Angular 18+ (Standalone Components, Signals, Reactive Forms, Tailwind CSS)
- **Backend:** Spring Boot (Java 17+, Spring Security, Spring Data JPA, JWT)
- **Base de datos:** MySQL 8.0+
- **Gestión de dependencias:** Maven / npm

## 📁 Estructura del proyecto

```
semillas-de-esperanza/
├── backend_donation/     # API REST con Spring Boot
│   ├── src/
│   └── pom.xml
├── frontend_donation/    # Aplicación cliente Angular
│   ├── src/
│   └── package.json
└── README.md
```

## ⚙️ Requisitos previos

- Java 17 o superior
- Node.js 18 o superior y npm
- MySQL 8.0 o superior
- Maven

## 🚀 Instalación y configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/semillas-de-esperanza.git
cd semillas-de-esperanza
```

### 2. Configurar la base de datos MySQL

```sql
CREATE DATABASE semillas_esperanza;
```

Configura las credenciales en `backend_donation/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/semillas_esperanza
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
spring.jpa.hibernate.ddl-auto=update
```

### 3. Levantar el backend (Spring Boot)

```bash
cd backend_donation
mvn clean install
mvn spring-boot:run
```

El backend quedará disponible en `http://localhost:8080`.

### 4. Levantar el frontend (Angular)

```bash
cd frontend_donation
npm install
npm start
```

El frontend quedará disponible en `http://localhost:4200`.

## 📌 Funcionalidades principales

- Gestión y autenticación segura de usuarios (JWT, roles de Administrador y Donante).
- Visualización de campañas sociales con cálculo dinámico de metas y donantes.
- Pasarela de donaciones con métodos de pago (Tarjeta, Transferencia, Yape y PayPal).
- Panel de donante con historial de aportes y generación de comprobantes.
- Consola administrativa para control y gestión de campañas, donaciones y usuarios.

## 👥 Autores

- Semillas de Esperanza — Equipo de Desarrollo
