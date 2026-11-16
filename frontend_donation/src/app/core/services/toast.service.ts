import { isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info';

export interface ToastMessage {
  id: number;
  type: ToastType;
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private readonly platformId = inject(PLATFORM_ID);
  private nextId = 1;
  readonly toasts = signal<ToastMessage[]>([]);

  show(type: ToastType, message: string, durationMs = 4500): void {
    const id = this.nextId++;
    this.toasts.update((list) => [...list, { id, type, message }]);
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        this.dismiss(id);
      }, durationMs);
    }
  }

  success(message: string): void {
    this.show('success', message);
  }

  error(message: string): void {
    this.show('error', message);
  }

  info(message: string): void {
    this.show('info', message);
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }
}
