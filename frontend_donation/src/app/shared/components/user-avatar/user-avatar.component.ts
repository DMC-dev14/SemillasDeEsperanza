import { Component, input, output, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-user-avatar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-avatar.component.html',
  styleUrl: './user-avatar.component.scss'
})
export class UserAvatarComponent {
  imageUrl = input<string | null | undefined>(null);
  name = input<string>('Usuario');
  size = input<'xs' | 'sm' | 'md' | 'lg' | 'xl'>('md');
  editable = input<boolean>(false);
  alt = input<string>('Avatar de usuario');

  avatarChange = output<string | null>();

  private authService = inject(AuthService);
  protected isUploading = signal(false);
  protected uploadError = signal<string | null>(null);

  protected get displayName(): string {
    const n = this.name();
    if (n && n !== 'Usuario') return n;
    const user = this.authService.currentUser();
    return user?.name || 'Usuario';
  }

  protected get initials(): string {
    const name = this.displayName;
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  }

  protected get sizeClass(): string {
    const sizes = {
      xs: 'avatar-xs',
      sm: 'avatar-sm',
      md: 'avatar-md',
      lg: 'avatar-lg',
      xl: 'avatar-xl'
    };
    return sizes[this.size()];
  }

  protected get hasImage(): boolean {
    return !!this.imageUrl() && this.imageUrl()!.trim() !== '';
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    const file = input.files[0];
    if (!this.validateFile(file)) return;

    this.uploadAvatar(file);
    input.value = '';
  }

  protected onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img) {
      img.style.display = 'none';
    }
  }

  private validateFile(file: File): boolean {
    const maxSize = 5 * 1024 * 1024;
    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

    if (!allowedTypes.includes(file.type)) {
      this.uploadError.set('Solo se permiten imágenes JPG, PNG, WebP o GIF');
      return false;
    }
    if (file.size > maxSize) {
      this.uploadError.set('La imagen no debe superar los 5MB');
      return false;
    }
    return true;
  }

  private uploadAvatar(file: File): void {
    this.isUploading.set(true);
    this.uploadError.set(null);

    this.authService.uploadAvatar(file).subscribe({
      next: (response) => {
        this.isUploading.set(false);
        this.avatarChange.emit(response.imageUrl);
      },
      error: (err) => {
        this.isUploading.set(false);
        this.uploadError.set(err.error?.message || 'Error al subir la imagen');
      }
    });
  }

  protected removeAvatar(): void {
    this.authService.deleteAvatar().subscribe({
      next: () => {
        this.avatarChange.emit(null);
      },
      error: (err) => {
        this.uploadError.set(err.error?.message || 'Error al eliminar la imagen');
      }
    });
  }

  protected triggerFileInput(fileInput: HTMLInputElement): void {
    fileInput.click();
  }
}