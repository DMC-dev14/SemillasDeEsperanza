import { Routes } from '@angular/router';
import { adminGuard, authGuard, donorGuard, guestGuard } from './core/guards/auth-guard';

export const routes: Routes = [
  {
    path: '',
    title: 'Semillas de Esperanza — Inicio',
    loadComponent: () => import('./features/home/home').then((m) => m.HomeComponent),
  },
  {
    path: 'index',
    redirectTo: '',
    pathMatch: 'full',
  },
  {
    path: 'inicio',
    redirectTo: '',
    pathMatch: 'full',
  },
  {
    path: 'proyectos',
    title: 'Semillas de Esperanza — Proyectos Activos',
    loadComponent: () =>
      import('./features/campaigns/campaigns').then((m) => m.CampaignsComponent),
  },
  {
    path: 'proyectos/:id',
    title: 'Semillas de Esperanza — Detalle de Proyecto',
    loadComponent: () =>
      import('./features/campaigns/campaign-detail/campaign-detail').then(
        (m) => m.CampaignDetailComponent
      ),
  },
  {
    path: 'nosotros',
    title: 'Semillas de Esperanza — Quiénes Somos',
    loadComponent: () => import('./features/about/about').then((m) => m.AboutComponent),
  },
  {
    path: 'contacto',
    title: 'Semillas de Esperanza — Contáctanos',
    loadComponent: () => import('./features/contact/contact').then((m) => m.ContactComponent),
  },
  {
    path: 'donar',
    title: 'Semillas de Esperanza — Haz tu Donación',
    loadComponent: () =>
      import('./features/donations/donations').then((m) => m.DonationsComponent),
  },
  {
    path: 'pagos',
    title: 'Semillas de Esperanza — Pasarela de Pagos',
    loadComponent: () =>
      import('./features/donations/payments/payments').then((m) => m.PaymentsComponent),
  },
  {
    path: '',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/auth').then((m) => m.AuthComponent),
    children: [
      {
        path: 'login',
        title: 'Semillas de Esperanza — Iniciar Sesión',
        loadComponent: () =>
          import('./features/auth/login/login').then((m) => m.LoginComponent),
      },
      {
        path: 'registro',
        title: 'Semillas de Esperanza — Crear Cuenta',
        loadComponent: () =>
          import('./features/auth/register/register').then((m) => m.RegisterComponent),
      },
      {
        path: 'recuperar-password',
        title: 'Semillas de Esperanza — Recuperar Contraseña',
        loadComponent: () =>
          import('./features/auth/forgot-password/forgot-password').then(
            (m) => m.ForgotPasswordComponent
          ),
      },
      {
        path: 'forgot-password',
        redirectTo: 'recuperar-password',
        pathMatch: 'full',
      },
      {
        path: 'reset-password',
        title: 'Semillas de Esperanza — Restablecer Contraseña',
        loadComponent: () =>
          import('./features/auth/reset-password/reset-password').then(
            (m) => m.ResetPasswordComponent
          ),
      },
    ],
  },
  {
    path: 'dashboard',
    title: 'Semillas de Esperanza — Mi Cuenta de Donante',
    canActivate: [donorGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard').then((m) => m.DashboardComponent),
  },
  {
    path: 'perfil',
    title: 'Semillas de Esperanza — Mi Perfil',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profile/profile').then((m) => m.ProfileComponent),
  },
  {
    path: 'admin',
    title: 'Semillas de Esperanza — Panel Administrador',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/admin').then((m) => m.AdminComponent),
  },
  {
    path: 'usuarios',
    title: 'Semillas de Esperanza — Gestión de Usuarios',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/users/user-list/user-list').then((m) => m.UserListComponent),
  },
  {
    path: 'usuarios/nuevo',
    title: 'Semillas de Esperanza — Nuevo Usuario',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/users/user-form/user-form').then((m) => m.UserFormComponent),
  },
  {
    path: '404',
    title: 'Semillas de Esperanza — Página no encontrada (404)',
    loadComponent: () =>
      import('./features/not-found/not-found').then((m) => m.NotFoundComponent),
  },
  {
    path: '**',
    title: 'Semillas de Esperanza — Página no encontrada (404)',
    loadComponent: () =>
      import('./features/not-found/not-found').then((m) => m.NotFoundComponent),
  },
];
