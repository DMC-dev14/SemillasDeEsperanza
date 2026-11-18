import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CampaignResponse } from '../../core/models/campaign.model';
import { CampaignService } from '../../core/services/campaign.service';
import { FALLBACK_CAMPAIGN_IMAGE } from '../../core/utils/password-validators';
import { UserAvatarComponent } from '../../shared/components/user-avatar';

@Component({
  selector: 'app-campaigns',
  standalone: true,
  imports: [CommonModule, RouterLink, UserAvatarComponent],
  templateUrl: './campaigns.html',
  styleUrl: './campaigns.css',
})
export class CampaignsComponent implements OnInit {
  readonly campaignService = inject(CampaignService);
  private readonly router = inject(Router);

  readonly categories = ['Todos', 'Educación', 'Salud', 'Vivienda', 'Alimentación'];
  readonly selectedCategory = signal('Todos');
  readonly campaigns = signal<CampaignResponse[]>([]);
  readonly loading = signal(true);

  readonly filteredCampaigns = computed(() => {
    const cat = this.selectedCategory();
    const all = this.campaigns();
    if (cat === 'Todos') return all;
    return all.filter((c) => c.category === cat);
  });

  ngOnInit(): void {
    this.campaignService.getActiveCampaigns().subscribe({
      next: (data) => {
        this.campaigns.set(data || []);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img && img.src !== FALLBACK_CAMPAIGN_IMAGE) {
      img.src = FALLBACK_CAMPAIGN_IMAGE;
    }
  }

  filterProjects(cat: string): void {
    this.selectedCategory.set(cat);
  }

  openDetail(id: number): void {
    this.router.navigate(['/proyectos', id]);
  }

  donateToProject(id: number, event: MouseEvent): void {
    event.stopPropagation();
    this.router.navigate(['/donar'], { queryParams: { proyecto: id } });
  }
}
