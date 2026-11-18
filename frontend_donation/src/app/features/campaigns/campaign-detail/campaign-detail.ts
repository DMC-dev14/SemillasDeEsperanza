import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CampaignResponse } from '../../../core/models/campaign.model';
import { CampaignService } from '../../../core/services/campaign.service';
import { ToastService } from '../../../core/services/toast.service';
import { FALLBACK_CAMPAIGN_IMAGE } from '../../../core/utils/password-validators';
import { UserAvatarComponent } from '../../../shared/components/user-avatar';

@Component({
  selector: 'app-campaign-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, UserAvatarComponent],
  templateUrl: './campaign-detail.html',
  styleUrl: './campaign-detail.css',
})
export class CampaignDetailComponent implements OnInit {
  readonly campaignService = inject(CampaignService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly project = signal<CampaignResponse | null>(null);
  readonly loading = signal<boolean>(true);

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const rawId = params.get('id');
      const numId = Number(rawId);
      if (Number.isNaN(numId) || numId <= 0) {
        this.router.navigate(['/404']);
        return;
      }

      this.loading.set(true);
      this.campaignService.getCampaignById(numId).subscribe({
        next: (camp) => {
          this.project.set(camp);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.toast.error('El proyecto solicitado no existe o no se encuentra disponible.');
          this.router.navigate(['/404']);
        },
      });
    });
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img && img.src !== FALLBACK_CAMPAIGN_IMAGE) {
      img.src = FALLBACK_CAMPAIGN_IMAGE;
    }
  }
}
