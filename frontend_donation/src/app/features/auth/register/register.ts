import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth';
import { ToastService } from '../../../core/services/toast.service';
import {
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  PASSWORD_REGEX,
  createPasswordMatchValidator,
  extractApiErrorMessage,
} from '../../../core/utils/password-validators';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly stepLabels = ['Datos personales', 'Contraseña', '¡Listo!'];
  readonly regStep = signal(0);
  readonly loading = signal(false);
  readonly serverError = signal('');
  readonly showPw = signal(false);
  readonly showConfirmPw = signal(false);
  readonly currentPwValue = signal('');

  readonly registerForm = this.fb.group(
    {
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(PASSWORD_MIN_LENGTH),
          Validators.maxLength(PASSWORD_MAX_LENGTH),
          Validators.pattern(PASSWORD_REGEX),
        ],
      ],
      confirmPassword: ['', [Validators.required]],
      acceptTerms: [false, [Validators.requiredTrue]],
    },
    { validators: createPasswordMatchValidator('password', 'confirmPassword') }
  );

  readonly reqLen = computed(() => {
    const pw = this.currentPwValue();
    return pw.length >= PASSWORD_MIN_LENGTH && pw.length <= PASSWORD_MAX_LENGTH;
  });
  readonly reqUpper = computed(() => /[A-Z]/.test(this.currentPwValue()));
  readonly reqLower = computed(() => /[a-z]/.test(this.currentPwValue()));
  readonly reqNum = computed(() => /[0-9]/.test(this.currentPwValue()));

  readonly pwScore = computed(() => {
    return [this.reqLen(), this.reqUpper(), this.reqLower(), this.reqNum()].filter(Boolean).length;
  });

  readonly pwStrengthLabel = computed(() => {
    const pw = this.currentPwValue();
    const score = this.pwScore();
    if (!pw || score === 0) return '';
    const labels = [
      'Muy débil',
      'Incompleta',
      'Casi lista',
      'Cumple todos los requisitos ✓',
    ];
    return labels[score - 1];
  });

  readonly pwStrengthColor = computed(() => {
    const score = this.pwScore();
    if (score === 0) return 'var(--sand)';
    const colors = ['#C1462F', '#8A5500', '#7C9A3E', '#2F9E6B'];
    return colors[score - 1];
  });

  onPasswordInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value || '';
    this.currentPwValue.set(val);
    this.serverError.set('');
  }

  togglePw(): void {
    this.showPw.update((v) => !v);
  }

  toggleConfirmPw(): void {
    this.showConfirmPw.update((v) => !v);
  }

  regNext(step: number): void {
    this.serverError.set('');

    if (step === 0) {
      const nameCtrl = this.registerForm.get('name');
      const emailCtrl = this.registerForm.get('email');
      nameCtrl?.markAsTouched();
      emailCtrl?.markAsTouched();

      if (nameCtrl?.invalid || emailCtrl?.invalid) {
        this.serverError.set(
          'Por favor completa correctamente tu nombre y correo electrónico.'
        );
        return;
      }

      this.regStep.set(1);
      return;
    }

    if (step === 1) {
      this.registerForm.markAllAsTouched();

      if (this.registerForm.invalid) {
        this.serverError.set(
          'Revisa los campos marcados en rojo antes de crear tu cuenta.'
        );
        this.toast.error('Verifica los requisitos de la contraseña y los términos.');
        return;
      }

      this.loading.set(true);
      const name = (this.registerForm.get('name')?.value || '').trim();
      const email = (this.registerForm.get('email')?.value || '').trim();
      const password = this.registerForm.get('password')?.value || '';

      this.auth.register({ name, email, password }).subscribe({
        next: (authData) => {
          this.loading.set(false);
          this.regStep.set(2);
          const firstName = (authData.user?.name || name).split(' ')[0];
          this.toast.success(`¡Cuenta creada! Bienvenido/a, ${firstName} 🌱`);

          setTimeout(() => {
            this.router.navigate(['/dashboard']);
          }, 1000);
        },
        error: (err) => {
          this.loading.set(false);
          const msg = extractApiErrorMessage(err, 'No se pudo completar el registro.');
          this.serverError.set(msg);
          this.toast.error(msg);

          if (err.status === 409 || msg.toLowerCase().includes('email')) {
            this.regStep.set(0);
            this.registerForm.get('email')?.setErrors({ conflict: msg });
          }
        },
      });
    }
  }

  regBack(): void {
    this.serverError.set('');
    this.regStep.update((s) => Math.max(0, s - 1));
  }

  handleRegisterFormSubmit(): void {
    if (this.regStep() === 0) {
      this.regNext(0);
    } else if (this.regStep() === 1) {
      this.regNext(1);
    }
  }
}
