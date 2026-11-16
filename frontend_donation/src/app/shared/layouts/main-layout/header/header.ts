import { Component, inject, signal } from '@angular/core';
import { CommonModule, ViewportScroller } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../../core/services/auth';
import { ToastService } from '../../../../core/services/toast.service';
import { HasRole } from '../../../directives/has-role';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, HasRole],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly viewportScroller = inject(ViewportScroller);

  readonly mobileMenuOpen = signal(false);
  readonly activeAdminTab = signal<'resumen' | 'campanas' | 'donaciones' | 'usuarios'>('resumen');

  toggleMenu(): void {
    this.mobileMenuOpen.update((v) => !v);
  }

  closeMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  get userInitial(): string {
    const name = this.auth.currentUser()?.name;
    if (!name || !name.trim()) {
      return this.auth.currentRole() === 'ADMINISTRADOR' ? 'A' : 'C';
    }
    return name.trim().charAt(0).toUpperCase();
  }

  switchAdminTab(tab: 'resumen' | 'campanas' | 'donaciones' | 'usuarios', event?: Event): void {
    if (event) {
      event.preventDefault();
    }
    this.activeAdminTab.set(tab);
    this.closeMenu();
    this.router.navigate(['/admin'], { fragment: tab });
  }

  goToSection(path: string, fragment: string, event?: Event): void {
    if (event) {
      event.preventDefault();
    }
    this.closeMenu();
    this.router.navigate([path], { fragment }).then(() => {
      this.viewportScroller.scrollToAnchor(fragment);
    });
  }

  handleLogout(): void {
    this.closeMenu();
    this.auth.logout().subscribe({
      next: () => {
        this.toast.info('Has cerrado sesión correctamente.');
        this.router.navigate(['/login']);
      },
    });
  }
}
