import { Component, inject } from '@angular/core';
import { CommonModule, ViewportScroller } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth';
import { ContactService } from '../../../../core/services/contact.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './footer.html',
  styleUrl: './footer.css',
})
export class Footer {
  readonly auth = inject(AuthService);
  readonly toast = inject(ToastService);
  private readonly contactService = inject(ContactService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly viewportScroller = inject(ViewportScroller);

  readonly newsletterForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  switchAdminTab(tab: 'resumen' | 'campanas' | 'donaciones' | 'usuarios', event?: Event): void {
    if (event) {
      event.preventDefault();
    }
    this.router.navigate(['/admin'], { fragment: tab });
  }

  goToSection(path: string, fragment: string, event?: Event): void {
    if (event) {
      event.preventDefault();
    }
    this.router.navigate([path], { fragment }).then(() => {
      this.viewportScroller.scrollToAnchor(fragment);
    });
  }

  handleNewsletter(): void {
    if (this.newsletterForm.invalid) {
      this.newsletterForm.markAllAsTouched();
      this.toast.error('Ingresa un correo electrónico válido para suscribirte.');
      return;
    }
    const email = this.newsletterForm.controls.email.value;
    this.contactService.subscribeNewsletter(email).subscribe({
      next: () => {
        this.toast.success('¡Suscripción confirmada! 🌱');
        this.newsletterForm.reset();
      },
    });
  }

  handleLogout(): void {
    this.auth.logout().subscribe({
      next: () => {
        this.toast.info('Has cerrado sesión correctamente.');
        this.router.navigate(['/login']);
      },
    });
  }
}
