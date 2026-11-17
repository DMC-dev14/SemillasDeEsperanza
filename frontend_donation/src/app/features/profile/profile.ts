import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth';
import { ToastService } from '../../core/services/toast.service';
import { extractApiErrorMessage } from '../../core/utils/password-validators';
import { CustomDatePipe } from '../../shared/pipes/custom-date-pipe';
import { ImageService } from '../../core/services/image.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, CustomDatePipe],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class ProfileComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);

  readonly saving = signal(false);
  readonly errorMsg = signal('');

  readonly profileForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
  });

  selectedFile: File | null = null;
  previewUrl: string | null = null;
  private imageService = inject(ImageService);
  imageUrl: string | null = null;
  ngOnInit(): void {
    const u = this.auth.currentUser();
    if (u) {
      this.profileForm.patchValue({ name: u.name, email: u.email });
    }
  }

  get userInitial(): string {
    const name = this.auth.currentUser()?.name;
    return name && name.trim() ? name.trim().charAt(0).toUpperCase() : 'C';
  }

  saveProfile(): void {
    this.errorMsg.set('');
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const { name, email } = this.profileForm.getRawValue();

    this.auth.updateProfile({ name: name.trim(), email: email.trim() }).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success('Perfil actualizado correctamente.');
      },
      error: (err) => {
        this.saving.set(false);
        const msg = extractApiErrorMessage(err, 'Error al actualizar el perfil.');
        this.errorMsg.set(msg);
        this.toast.error(msg);
      },
    });
  }

  readonly uploadingAvatar = signal(false);

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.selectedFile = file;
    this.previewUrl = URL.createObjectURL(file);
  }

  upload() {
    if (!this.selectedFile) return;
    this.uploadingAvatar.set(true);
    this.imageService.upload(this.selectedFile).subscribe({
      next: (res) => {
        this.imageUrl = res.url;
        this.previewUrl = null;
        this.selectedFile = null;
        this.uploadingAvatar.set(false);
        this.toast.success('Avatar actualizado');
      },
      error: (err) => {
        this.uploadingAvatar.set(false);
        this.toast.error(err.error?.message || 'Error subiendo');
      },
    });
  }
}
