# SemillasDeEsperanza
# 🌱 Semillas de Esperanza

Sistema web desarrollado con **Angular**, **Spring Boot** y **MySQL**.

## 📋 Descripción

Semillas de Esperanza es una aplicación web que [agrega aquí una breve descripción del propósito del proyecto: por ejemplo, gestión de donaciones, apoyo a comunidades, programas sociales, etc.]

## 🛠️ Tecnologías utilizadas

- **Frontend:** Angular
- **Backend:** Spring Boot (Java)
- **Base de datos:** MySQL
- **Otras herramientas:** Maven / npm

## 📁 Estructura del proyecto

```
semillas-de-esperanza/
├── backend/          # API REST con Spring Boot
│   ├── src/
│   └── pom.xml
├── frontend/         # Aplicación Angular
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

Configura las credenciales en `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/semillas_esperanza
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
spring.jpa.hibernate.ddl-auto=update
```

### 3. Levantar el backend (Spring Boot)

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

El backend quedará disponible en `http://localhost:8080`

### 4. Levantar el frontend (Angular)

```bash
cd frontend
npm install
ng serve
```

El frontend quedará disponible en `http://localhost:4200`

## 📌 Funcionalidades principales

- [ ] Funcionalidad 1
- [ ] Funcionalidad 2
- [ ] Funcionalidad 3

## 🤝 Contribuir

Las contribuciones son bienvenidas. Para contribuir:

1. Haz un fork del proyecto
2. Crea una rama para tu funcionalidad (`git checkout -b feature/nueva-funcionalidad`)
3. Haz commit de tus cambios (`git commit -m 'Agrega nueva funcionalidad'`)
4. Sube tus cambios (`git push origin feature/nueva-funcionalidad`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la licencia [MIT](LICENSE) (o la que corresponda).

## 👥 Autores

- Tu nombre - [tu-usuario](https://github.com/tu-usuario)
