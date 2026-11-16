# 🌱 Semillas de Esperanza — Frontend Angular

Frontend SPA desarrollado en **Angular 21** (Standalone Components, Signals, Reactive Forms e inyección mediante `inject()`), migrado con fidelidad visual 1:1 desde el diseño original en HTML5/CSS3 y conectado íntegramente a la API REST de **Spring Boot + MySQL**.

## 🏛️ Arquitectura del Proyecto (Feature-Based + Atomic Design)

```text
src/app/
├── core/                           # Capa transversal singleton (Autenticación, API, Seguridad)
│   ├── guards/
│   │   └── auth-guard.ts           # Guards funcionales (authGuard, adminGuard, guestGuard) con inject()
│   ├── interceptors/
│   │   └── auth-interceptor.ts     # Interceptor HTTP funcional para adjuntar Bearer JWT con inject()
│   ├── models/
│   │   ├── auth.model.ts           # DTOs de autenticación, perfil y usuarios
│   │   ├── campaign.model.ts       # DTOs de campañas y proyectos activos
│   │   └── donation.model.ts       # DTOs de donaciones, comprobantes, Yape y PayPal
│   └── services/
│       ├── auth.ts                 # Servicio de sesión, login, registro y recuperación (inject)
│       ├── campaign.service.ts     # Servicio CRUD de campañas y formateo monetario (inject)
│       ├── donation.service.ts     # Servicio de donaciones, dashboard y pasarelas (inject)
│       ├── storage.ts              # Servicio SSR-safe para LocalStorage (inject)
│       ├── toast.service.ts        # Servicio reactivo de notificaciones Toast globales
│       └── user.service.ts         # Servicio administrativo de gestión de usuarios (inject)
├── shared/                         # Componentes atómicos, layouts reutilizables, pipes y directivas
│   ├── components/
│   │   └── confirm-modal/          # Modal reutilizable de confirmación
│   ├── directives/
│   │   └── has-role.ts             # Directiva estructural por rol (*appHasRole) con inject()
│   ├── layouts/main-layout/
│   │   ├── header/                 # Navbar adaptativo para VISITANTE, USUARIO y ADMINISTRADOR
│   │   └── footer/                 # Footer adaptativo con suscripción reactiva a Newsletter
│   └── pipes/
│       └── custom-date-pipe.ts     # Pipe de formato dd/MM/yyyy HH:mm
├── features/                       # Módulos funcionales por dominio (Lazy-Loaded)
│   ├── home/                       # Página de inicio (Hero, misión, campaña destacada, testimonios)
│   ├── about/                      # Quiénes somos (Misión, visión, línea de tiempo, equipo, sellos)
│   ├── campaigns/                  # Catálogo filtrable de proyectos activos
│   │   └── campaign-detail/        # Detalle de proyecto y desglose de fondos
│   ├── donations/                  # Flujo de donación (única/recurrente, montos, proyecto, pago)
│   │   └── payments/               # Integración de pasarela Yape / MercadoPago
│   ├── contact/                    # Formulario reactivo de contacto e información institucional
│   ├── auth/
│   │   ├── login/                  # Inicio de sesión reactivo con JWT
│   │   ├── register/               # Registro multipaso (3 pasos) con medidor de seguridad
│   │   ├── forgot-password/        # Solicitud de enlace de recuperación por correo
│   │   └── reset-password/         # Restablecimiento de contraseña con token
│   ├── dashboard/                  # Panel de donante (KPIs, top donadores, feed en vivo, recibos, perfil)
│   ├── profile/                    # Vista dedicada de edición de perfil personal
│   ├── admin/                      # Consola administrativa (Resumen, Campañas, Donaciones, Usuarios)
│   ├── users/                      # Componentes modulares de listado y creación de usuarios
│   └── not-found/                  # Página de Error 404 personalizada
├── app.routes.ts                   # Definición centralizada de rutas + ruta comodín 404
├── app.config.ts                   # Configuración de HttpClient (withFetch + authInterceptor) y Router
└── app.ts                          # Componente raíz
```

## 🚀 Ejecución en Entorno Local

1. Instalar dependencias:
   ```bash
   cd frontend_donation
   npm install
   ```
2. Iniciar servidor de desarrollo (puerto `4200`, conectado a `http://localhost:8080`):
   ```bash
   npm start
   ```
