import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './confirm-modal.html',
  styleUrl: './confirm-modal.css',
})
export class ConfirmModal {
  readonly open = input<boolean>(false);
  readonly title = input<string>('¿Confirmar acción?');
  readonly description = input<string>('');
  readonly subDescription = input<string>('');
  readonly confirmText = input<string>('¡Confirmar!');
  readonly cancelText = input<string>('Cancelar');

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  onOverlayClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cancelled.emit();
    }
  }
}
