import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { UserResponse } from '../../../core/models/auth.model';
import { AuthService } from '../../../core/services/auth';
import { ToastService } from '../../../core/services/toast.service';
import { UserService } from '../../../core/services/user.service';
import { extractApiErrorMessage } from '../../../core/utils/password-validators';
import { CustomDatePipe } from '../../../shared/pipes/custom-date-pipe';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RolNombre } from '../../../core/models/auth.model';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, CustomDatePipe],
  templateUrl: './user-list.html',
  styleUrl: './user-list.css',
})
export class UserListComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly toast = inject(ToastService);

  readonly usuarios = signal<UserResponse[]>([]);

  private readonly fb = inject(FormBuilder);

  readonly editUserModalOpen = signal(false);
  readonly editingUserId = signal<number | null>(null);
  readonly savingEditUser = signal(false);
  readonly editUserError = signal('');

  readonly editUserForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    rol: ['USUARIO' as RolNombre, [Validators.required]],
  });

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.userService.getAll().subscribe({
      next: (list) => this.usuarios.set(list || []),
      error: () => this.usuarios.set([]),
    });
  }

  toggleUserActive(u: UserResponse): void {
    this.userService.update(u.id, { active: !u.active }).subscribe({
      next: () => {
        this.toast.success(`Estado del usuario #${u.id} actualizado.`);
        this.loadUsers();
      },
      error: (err) => {
        const msg = extractApiErrorMessage(err, 'Error al actualizar usuario.');
        this.toast.error(msg);
      },
    });
  }
  deleteUser(id: number): void {
    if (!confirm('¿Eliminar usuario?')) return;
    this.userService.delete(id).subscribe({
      next: () => {
        this.toast.success('Usuario eliminado');
        this.loadUsers();
      },
      error: (err) => this.toast.error(extractApiErrorMessage(err, 'No se pudo eliminar')),
    });
  }

  openEditUser(u: UserResponse): void {
    if (u.id === this.auth.currentUser()?.id) return;

    this.editingUserId.set(u.id);
    this.editUserError.set('');

    this.editUserForm.reset({
      name: u.name,
      email: u.email,
      rol: u.rolName === 'ADMINISTRADOR' ? 'ADMINISTRADOR' : 'USUARIO',
    });

    this.editUserModalOpen.set(true);
  }

  closeEditUser(): void {
    if (this.savingEditUser()) return;

    this.editUserModalOpen.set(false);
    this.editingUserId.set(null);
    this.editUserError.set('');
  }

  saveEditUser(): void {
    const id = this.editingUserId();

    if (id === null || this.savingEditUser()) return;

    this.editUserError.set('');

    if (this.editUserForm.invalid) {
      this.editUserForm.markAllAsTouched();
      this.editUserError.set('Verifica los datos del usuario.');
      return;
    }

    const { name, email, rol } = this.editUserForm.getRawValue();

    this.savingEditUser.set(true);

    this.userService
      .update(id, {
        name: name.trim(),
        email: email.trim(),
        rol,
      })
      .subscribe({
        next: () => {
          this.savingEditUser.set(false);
          this.closeEditUser();
          this.toast.success('Usuario actualizado correctamente');
          this.loadUsers();
        },
        error: (err) => {
          this.savingEditUser.set(false);

          const msg = extractApiErrorMessage(err, 'No se pudo actualizar el usuario');

          this.editUserError.set(msg);
          this.toast.error(msg);
        },
      });
  }
}
