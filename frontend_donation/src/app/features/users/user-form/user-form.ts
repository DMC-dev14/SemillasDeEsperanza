import { Component, EventEmitter, inject, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { RolNombre, UserResponse } from '../../../core/models/auth.model';
import { ToastService } from '../../../core/services/toast.service';
import { UserService } from '../../../core/services/user.service';
import {
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  PASSWORD_REGEX,
  extractApiErrorMessage,
} from '../../../core/utils/password-validators';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './user-form.html',
  styleUrl: './user-form.css',
})
export class UserFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  @Output() userCreated = new EventEmitter<UserResponse>();

  readonly loading = signal(false);
  readonly errorMsg = signal('');
  readonly showPw = signal(false);

  readonly userForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    rol: ['USUARIO' as RolNombre, [Validators.required]],
    password: [
      '',
      [
        Validators.required,
        Validators.minLength(PASSWORD_MIN_LENGTH),
        Validators.maxLength(PASSWORD_MAX_LENGTH),
        Validators.pattern(PASSWORD_REGEX),
      ],
    ],
  });

  submitForm(): void {
    if (this.loading()) return;
    this.errorMsg.set('');
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    const { name, email, rol, password } = this.userForm.getRawValue();
    this.loading.set(true);

    this.userService
      .create({ name: name.trim(), email: email.trim(), rol, password })
      .subscribe({
        next: (user) => {
          this.loading.set(false);
          this.toast.success('Usuario registrado exitosamente.');
          this.userForm.reset({ name: '', email: '', rol: 'USUARIO', password: '' });
          this.userCreated.emit(user);
          this.router.navigate(['/usuarios']);
        },
        error: (err) => {
          this.loading.set(false);
          const msg = extractApiErrorMessage(err, 'Error al crear el usuario.');
          this.errorMsg.set(msg);
        },
      });
  }
}
