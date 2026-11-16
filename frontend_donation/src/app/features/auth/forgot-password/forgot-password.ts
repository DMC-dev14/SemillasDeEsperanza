import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth';
import { ToastService } from '../../../core/services/toast.service';
import { extractApiErrorMessage } from '../../../core/utils/password-validators';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css',
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly sent = signal(false);
  readonly serverError = signal('');

  readonly forgotForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  handleSubmit(): void {
    this.serverError.set('');
    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    const email = this.forgotForm.controls.email.value.trim();

    this.auth.forgotPassword({ email }).subscribe({
      next: () => {
        this.loading.set(false);
        this.sent.set(true);
        this.toast.success('Si el correo está registrado, recibirás un enlace de recuperación.');
      },
      error: (err) => {
        this.loading.set(false);
        const msg = extractApiErrorMessage(
          err,
          'No se pudo procesar la solicitud en este momento.'
        );
        this.serverError.set(msg);
        this.toast.error(msg);
      },
    });
  }
}
