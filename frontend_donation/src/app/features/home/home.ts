import { CommonModule, isPlatformBrowser } from '@angular/common';
import {
  Component,
  inject,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

interface Testimonial {
  quote: string;
  name: string;
  role: string;
  image: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './home.html',
})
export class Home implements OnInit, OnDestroy {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  private timer?: ReturnType<typeof setInterval>;

  readonly testimonials: Testimonial[] = [
    {
      quote:
        'Gracias a las aulas equipadas por Semillas, mi hijo tiene acceso a computadoras por primera vez. Hoy sueña con ser ingeniero.',
      name: 'María Condori',
      role: 'Madre de familia, Lima',
      image:
        'https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=80&h=80&fit=crop&auto=format',
    },
    {
      quote:
        'El programa de nutrición cambió todo. Mis hijos llegaban con hambre a clase y no podían concentrarse. Ahora sus notas mejoraron.',
      name: 'Roberto Huanca',
      role: 'Padre de familia, Trujillo',
      image:
        'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=80&h=80&fit=crop&auto=format',
    },
    {
      quote:
        'Como donante, la transparencia de Semillas me da confianza. Veo fotos, reportes y sé exactamente dónde va cada sol que aporto.',
      name: 'Ana Flores',
      role: 'Donante recurrente, Miraflores',
      image:
        'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=80&h=80&fit=crop&auto=format',
    },
  ];

  readonly seals = [
    'SUNAT Donativos',
    'GuideStar Platinum',
    'ISO 9001:2015',
    'Charity Navigator 4★',
  ];

  readonly currentTestimonial = signal(0);
  readonly progress = signal(0);
  readonly subscribed = signal(false);

  newsletterEmail = '';

  ngOnInit(): void {
    // En SSR pintamos la barra ya llena para que el HTML salga correcto.
    if (!this.isBrowser) {
      this.progress.set(91);
      return;
    }

    setTimeout(() => this.progress.set(91), 100);
    this.timer = setInterval(() => this.nextTestimonial(), 5000);
  }

  ngOnDestroy(): void {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }

  nextTestimonial(): void {
    this.currentTestimonial.update((i) => (i + 1) % this.testimonials.length);
  }

  selectTestimonial(index: number): void {
    this.currentTestimonial.set(index);

    if (!this.isBrowser) return;

    if (this.timer) clearInterval(this.timer);
    this.timer = setInterval(() => this.nextTestimonial(), 5000);
  }

  subscribe(): void {
    if (!this.newsletterEmail) return;

    this.subscribed.set(true);
    this.newsletterEmail = '';
  }
}