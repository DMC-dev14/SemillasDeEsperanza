import { Component, inject, OnDestroy, OnInit, PLATFORM_ID, signal } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CampaignResponse } from '../../core/models/campaign.model';
import { CampaignService } from '../../core/services/campaign.service';
import { ContactService } from '../../core/services/contact.service';
import { ToastService } from '../../core/services/toast.service';
import { FALLBACK_CAMPAIGN_IMAGE } from '../../core/utils/password-validators';

interface Testimonial {
  quote: string;
  name: string;
  role: string;
  image: string;
}

const TESTIMONIALS: Testimonial[] = [
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

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class HomeComponent implements OnInit, OnDestroy {
  readonly campaignService = inject(CampaignService);
  private readonly contactService = inject(ContactService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly testimonials = TESTIMONIALS;
  readonly currentTestimonialIdx = signal(0);
  readonly featuredCampaign = signal<CampaignResponse | null>(null);
  readonly loadingFeatured = signal(true);

  private timerId: ReturnType<typeof setInterval> | null = null;

  readonly newsletterForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  ngOnInit(): void {
    this.campaignService.getActiveCampaigns().subscribe({
      next: (list) => {
        this.loadingFeatured.set(false);
        if (list && list.length > 0) {
          const sorted = [...list].sort(
            (a, b) => Number(b.amountCollected || 0) - Number(a.amountCollected || 0)
          );
          this.featuredCampaign.set(sorted[0]);
        }
      },
      error: () => {
        this.loadingFeatured.set(false);
      },
    });

    if (isPlatformBrowser(this.platformId)) {
      this.startTestimonialTimer();
    }
  }

  ngOnDestroy(): void {
    this.stopTestimonialTimer();
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img && img.src !== FALLBACK_CAMPAIGN_IMAGE) {
      img.src = FALLBACK_CAMPAIGN_IMAGE;
    }
  }

  get currentTestimonial(): Testimonial {
    return this.testimonials[this.currentTestimonialIdx()];
  }

  setTestimonial(index: number): void {
    this.currentTestimonialIdx.set(index);
    if (isPlatformBrowser(this.platformId)) {
      this.startTestimonialTimer();
    }
  }

  private startTestimonialTimer(): void {
    this.stopTestimonialTimer();
    this.timerId = setInterval(() => {
      this.currentTestimonialIdx.update((idx) => (idx + 1) % this.testimonials.length);
    }, 5000);
  }

  private stopTestimonialTimer(): void {
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }

  formatIntegerWithCommas(val: number | null | undefined): string {
    return Math.round(Number(val || 0)).toLocaleString('en-US');
  }

  handleNewsletter(): void {
    if (this.newsletterForm.invalid) {
      this.newsletterForm.markAllAsTouched();
      this.toast.error('Por favor ingresa un correo electrónico válido.');
      return;
    }
    const email = this.newsletterForm.controls.email.value;
    this.contactService.subscribeNewsletter(email).subscribe(() => {
      this.toast.success('¡Suscripción confirmada! 🌱');
      this.newsletterForm.reset();
    });
  }
}
