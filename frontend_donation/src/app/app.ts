import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { Header } from './shared/layouts/main-layout/header/header';
import { Footer } from './shared/layouts/main-layout/footer/footer';
import { ToastService } from './core/services/toast.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, Header, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly router = inject(Router);
  readonly toastService = inject(ToastService);

  protected readonly title = signal('Semillas de Esperanza');
  private readonly currentUrl = signal('/');

  readonly isStandaloneAuthPage = computed(() => {
    const url = this.currentUrl().split('?')[0].split('#')[0];
    return (
      url === '/login' ||
      url === '/registro' ||
      url === '/recuperar-password' ||
      url === '/forgot-password' ||
      url === '/reset-password'
    );
  });

  constructor() {
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed()
      )
      .subscribe((e) => {
        this.currentUrl.set(e.urlAfterRedirects || e.url);
      });
  }
}