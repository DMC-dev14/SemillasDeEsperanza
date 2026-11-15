import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth';
import { ToastService } from '../../../core/services/toast.service';
import { LoginRequest } from '../../../core/models/auth.model';
import { extractApiErrorMessage } from '../../../core/utils/password-validators';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly serverError = signal('');
  readonly showPassword = signal(false);

  readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    password: ['', [Validators.required]],
  });

  togglePasswordVisibility(): void {
    this.showPassword.update((v) => !v);
  }

  clearServerError(): void {
    if (this.serverError()) {
      this.serverError.set('');
    }
  }

  handleLogin(): void {
    this.serverError.set('');

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.serverError.set('Por favor ingresa tu correo y contraseña.');
      return;
    }

    this.loading.set(true);

    const { email, password } = this.loginForm.getRawValue();
    const credentials: LoginRequest = {
      email: email.trim(),
      password,
    };

    this.auth.login(credentials).subscribe({
      next: (authData) => {
        this.loading.set(false);
        const role = authData.user?.rolName || 'USUARIO';
        const userName = authData.user?.name || 'Usuario';
        this.toast.success(`¡Bienvenido/a ${userName}! Redirigiendo...`);

        setTimeout(() => {
          if (role === 'ADMINISTRADOR') {
            this.router.navigate(['/admin']);
          } else {
            this.router.navigate(['/dashboard']);
          }
        }, 400);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = extractApiErrorMessage(
          err,
          'Credenciales inválidas. Verifica tu correo y contraseña.'
        );
        this.serverError.set(msg);
        this.toast.error(msg);
      },
    });
  }
}
