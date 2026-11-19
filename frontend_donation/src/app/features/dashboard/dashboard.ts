import { Component, computed, inject, OnInit, PLATFORM_ID, signal } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  ClientDashboardSummary,
  DonationResponse,
  PagedResponse,
  TopDonor,
} from '../../core/models/donation.model';
import { AuthService } from '../../core/services/auth';
import { CampaignService } from '../../core/services/campaign.service';
import { DonationService } from '../../core/services/donation.service';
import { ToastService } from '../../core/services/toast.service';
import {
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  PASSWORD_REGEX,
  createPasswordMatchValidator,
  extractApiErrorMessage,
} from '../../core/utils/password-validators';
import { CustomDatePipe } from '../../shared/pipes/custom-date-pipe';
import { UserAvatarComponent } from '../../shared/components/user-avatar';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, CustomDatePipe, UserAvatarComponent],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly campaignService = inject(CampaignService);
  private readonly donationService = inject(DonationService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly summary = signal<ClientDashboardSummary>({
    totalDonado: 0,
    cantidadDonaciones: 0,
    proyectosApoyados: 0,
  });
  readonly myDonations = signal<DonationResponse[]>([]);
  readonly myDonationsPage = signal<PagedResponse<DonationResponse> | null>(null);
  readonly myDonationsPageIndex = signal(0);
  readonly myDonationsPageSize = signal(10);
  readonly myDonationsTotalPages = computed(() => this.myDonationsPage()?.totalPages ?? 0);
  readonly myDonationsCurrentPage = computed(() => (this.myDonationsPage()?.page ?? 0) + 1);
  readonly canGoToPreviousMyDonationsPage = computed(() => this.myDonationsPageIndex() > 0);
  readonly canGoToNextMyDonationsPage = computed(
    () => this.myDonationsPageIndex() + 1 < this.myDonationsTotalPages(),
  );
  readonly topDonors = signal<TopDonor[]>([]);
  readonly recentFeed = signal<DonationResponse[]>([]);

  readonly editingProfile = signal(false);
  readonly savingProfile = signal(false);
  readonly profileError = signal('');

  readonly changingPw = signal(false);
  readonly savingPw = signal(false);
  readonly pwError = signal('');
  readonly showCurrPw = signal(false);
  readonly showNewPw = signal(false);
  readonly showConfirmPw = signal(false);

  readonly selectedReceipt = signal<DonationResponse | null>(null);

  readonly profileForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
  });

  readonly passwordForm = this.fb.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: [
        '',
        [
          Validators.required,
          Validators.minLength(PASSWORD_MIN_LENGTH),
          Validators.maxLength(PASSWORD_MAX_LENGTH),
          Validators.pattern(PASSWORD_REGEX),
        ],
      ],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: createPasswordMatchValidator('newPassword', 'confirmPassword') },
  );

  ngOnInit(): void {
    const user = this.auth.currentUser();
    if (user) {
      this.profileForm.patchValue({ name: user.name, email: user.email });
    }

    if (this.auth.getToken()) {
      this.auth.me().subscribe({
        next: (u) => {
          this.profileForm.patchValue({ name: u.name, email: u.email });
        },
        error: () => {},
      });
    }

    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.donationService.getMyDonations().subscribe((res) => {
      if (res.summary) {
        this.summary.set(res.summary);
      }
      this.topDonors.set(res.topDonors || []);
      this.recentFeed.set(res.recentFeed || []);
    });

    this.loadMyDonationsPage();
  }

  loadMyDonationsPage(page: number = this.myDonationsPageIndex()): void {
    this.donationService.getMyDonationsPage(page, this.myDonationsPageSize()).subscribe({
      next: (res) => {
        this.myDonationsPage.set(res);
        this.myDonations.set(res.content || []);
        this.myDonationsPageIndex.set(res.page);
        if (res.totalPages > 0 && res.page >= res.totalPages) {
          this.loadMyDonationsPage(res.totalPages - 1);
        }
      },
      error: () => {
        this.myDonationsPage.set(null);
        this.myDonations.set([]);
      },
    });
  }

  goToMyDonationsPage(page: number): void {
    if (page < 0 || page >= this.myDonationsTotalPages()) {
      return;
    }
    this.loadMyDonationsPage(page);
  }

  get userInitial(): string {
    const name = this.auth.currentUser()?.name;
    return name && name.trim() ? name.trim().charAt(0).toUpperCase() : 'C';
  }

  donorInitial(name: string | null | undefined): string {
    return name && name.trim() ? name.trim().charAt(0).toUpperCase() : 'D';
  }

  toggleEditProfile(): void {
    const next = !this.editingProfile();
    this.editingProfile.set(next);
    this.profileError.set('');
    if (next) {
      const user = this.auth.currentUser();
      this.profileForm.patchValue({
        name: user?.name || '',
        email: user?.email || '',
      });
    }
  }

  saveProfile(): void {
    this.profileError.set('');
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      this.profileError.set('Revisa el nombre (2 a 50 caracteres) y el formato de correo.');
      return;
    }

    this.savingProfile.set(true);
    const { name, email } = this.profileForm.getRawValue();

    this.auth.updateProfile({ name: name.trim(), email: email.trim() }).subscribe({
      next: () => {
        this.savingProfile.set(false);
        this.editingProfile.set(false);
        this.toast.success('Perfil actualizado en la base de datos.');
      },
      error: (err) => {
        this.savingProfile.set(false);
        const msg = extractApiErrorMessage(err, 'No se pudo actualizar el perfil.');
        this.profileError.set(msg);
        this.toast.error(msg);
      },
    });
  }

  onAvatarChange(imageUrl: string | null): void {
    if (imageUrl) {
      this.toast.success('Foto de perfil actualizada');
    } else {
      this.toast.success('Foto de perfil eliminada');
    }
    // The auth service already updates the currentUser signal internally
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      this.toast.error('Solo JPG, PNG o WEBP');
      input.value = '';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.toast.error('Máximo 5 MB');
      input.value = '';
      return;
    }

    this.auth.uploadAvatar(file).subscribe({
      next: (user) => {
        this.toast.success('Foto de perfil actualizada');
        input.value = '';
      },
      error: (err) => {
        this.toast.error('Error subiendo la foto');
        input.value = '';
      },
    });
  }

  togglePwChange(): void {
    const next = !this.changingPw();
    this.changingPw.set(next);
    this.pwError.set('');
    if (!next) {
      this.passwordForm.reset();
    }
  }

  savePwChange(): void {
    this.pwError.set('');
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      if (this.passwordForm.errors?.['passwordMismatch']) {
        this.pwError.set('La confirmación de contraseña no coincide.');
      } else {
        this.pwError.set(
          'La nueva contraseña debe tener entre 8 y 72 caracteres, una mayúscula, una minúscula y un número.',
        );
      }
      return;
    }

    this.savingPw.set(true);
    const currentPassword = this.passwordForm.get('currentPassword')?.value || '';
    const newPassword = this.passwordForm.get('newPassword')?.value || '';

    this.auth.changePassword({ currentPassword, newPassword }).subscribe({
      next: () => {
        this.savingPw.set(false);
        this.togglePwChange();
        this.toast.success('Contraseña actualizada correctamente en la base de datos.');
      },
      error: (err) => {
        this.savingPw.set(false);
        const msg = extractApiErrorMessage(err, 'Error al cambiar la contraseña.');
        this.pwError.set(msg);
        this.toast.error(msg);
      },
    });
  }

  openReceiptModal(donation: DonationResponse): void {
    this.selectedReceipt.set(donation);
  }

  closeReceiptModal(): void {
    this.selectedReceipt.set(null);
  }

  printReceipt(): void {
    if (isPlatformBrowser(this.platformId)) {
      window.print();
    }
  }
}
