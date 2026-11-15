import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { UserResponse } from '../../../core/models/auth.model';
import { AuthService } from '../../../core/services/auth';
import { ToastService } from '../../../core/services/toast.service';
import { UserService } from '../../../core/services/user.service';
import { extractApiErrorMessage } from '../../../core/utils/password-validators';
import { CustomDatePipe } from '../../../shared/pipes/custom-date-pipe';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, RouterLink, CustomDatePipe],
  templateUrl: './user-list.html',
  styleUrl: './user-list.css',
})
export class UserListComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly toast = inject(ToastService);

  readonly usuarios = signal<UserResponse[]>([]);

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
}
