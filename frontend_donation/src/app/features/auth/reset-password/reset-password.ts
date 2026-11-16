import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth';
import { ToastService } from '../../../core/services/toast.service';
import {
  PASSWORD_MIN_LENGTH,
  RESET_PASSWORD_REGEX,
  createPasswordMatchValidator,
  extractApiErrorMessage,
} from '../../../core/utils/password-validators';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css',
})
export class ResetPasswordComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly success = signal(false);
  readonly serverError = signal('');
  readonly showPw = signal(false);
  readonly currentPw = signal('');

  readonly resetForm = this.fb.group(
    {
      token: ['', [Validators.required]],
      newPassword: [
        '',
        [
          Validators.required,
          Validators.minLength(PASSWORD_MIN_LENGTH),
          Validators.pattern(RESET_PASSWORD_REGEX),
        ],
      ],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: createPasswordMatchValidator('newPassword', 'confirmPassword') }
  );

  readonly reqLen = computed(() => this.currentPw().length >= PASSWORD_MIN_LENGTH);
  readonly reqUpper = computed(() => /[A-Z]/.test(this.currentPw()));
  readonly reqLower = computed(() => /[a-z]/.test(this.currentPw()));
  readonly reqNum = computed(() => /\d/.test(this.currentPw()));
  readonly reqSpecial = computed(() => /[@$!%*?&]/.test(this.currentPw()));

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const token = params.get('token');
      if (token) {
        this.resetForm.patchValue({ token });
      }
    });
  }

  onPwInput(event: Event): void {
    this.currentPw.set((event.target as HTMLInputElement).value || '');
    this.serverError.set('');
  }

  togglePw(): void {
    this.showPw.update((v) => !v);
  }

  handleSubmit(): void {
    this.serverError.set('');
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    const token = (this.resetForm.get('token')?.value || '').trim();
    const newPassword = this.resetForm.get('newPassword')?.value || '';

    this.auth.resetPassword({ token, newPassword }).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        this.toast.success('¡Contraseña restablecida con éxito! Ya puedes iniciar sesión.');
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1500);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = extractApiErrorMessage(
          err,
          'El enlace de recuperación es inválido o ha expirado.'
        );
        this.serverError.set(msg);
        this.toast.error(msg);
      },
    });
  }
}
