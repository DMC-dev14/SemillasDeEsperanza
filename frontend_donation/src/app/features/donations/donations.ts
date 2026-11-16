import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CampaignResponse } from '../../core/models/campaign.model';
import { AuthService } from '../../core/services/auth';
import { CampaignService } from '../../core/services/campaign.service';
import { DonationService } from '../../core/services/donation.service';
import { ToastService } from '../../core/services/toast.service';
import { ConfirmModal } from '../../shared/components/confirm-modal/confirm-modal';
import {
  FALLBACK_CAMPAIGN_IMAGE,
  extractApiErrorMessage,
} from '../../core/utils/password-validators';

export type PaymentMethodType = 'card' | 'transfer' | 'wallet' | 'paypal';

@Component({
  selector: 'app-donations',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ConfirmModal],
  templateUrl: './donations.html',
  styleUrl: './donations.css',
})
export class DonationsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly campaignService = inject(CampaignService);
  private readonly donationService = inject(DonationService);
  private readonly toast = inject(ToastService);

  readonly presetAmounts = [20, 50, 100, 200];
  readonly campaigns = signal<CampaignResponse[]>([]);
  readonly loadingCampaigns = signal<boolean>(true);
  readonly selectedAmount = signal<number>(50);
  readonly isCustomAmount = signal<boolean>(false);
  readonly selectedType = signal<'unica' | 'recurrente'>('unica');
  readonly selectedPay = signal<PaymentMethodType>('card');
  readonly selectedProjectId = signal<number>(1);

  readonly modalOpen = signal<boolean>(false);
  readonly processing = signal<boolean>(false);
  readonly alertMessage = signal<string>('');
  readonly alertType = signal<'error' | 'success'>('error');

  readonly donateForm = this.fb.nonNullable.group({
    customAmount: [''],
    donorName: [
      this.auth.currentUser()?.name || '',
      [Validators.required, Validators.minLength(2), Validators.maxLength(100)],
    ],
    donorEmail: [
      this.auth.currentUser()?.email || '',
      [Validators.required, Validators.email],
    ],
    donorPhone: ['', [Validators.maxLength(30)]],
    cardNumber: [''],
    cardExpiry: [''],
    cardCvv: [''],
  });

  readonly selectedProject = computed<CampaignResponse | undefined>(() => {
    const id = Number(this.selectedProjectId());
    return this.campaigns().find((c) => c.id === id) || this.campaigns()[0];
  });

  readonly amountInvalid = computed(() => {
    return !this.selectedAmount() || this.selectedAmount() < 5;
  });

  ngOnInit(): void {
    const user = this.auth.currentUser();
    if (user) {
      this.donateForm.patchValue({
        donorName: user.name || '',
        donorEmail: user.email || '',
      });
    }

    this.applyCardValidators(this.selectedPay());

    this.campaignService.getActiveCampaigns().subscribe({
      next: (list) => {
        this.loadingCampaigns.set(false);
        if (list && list.length > 0) {
          this.campaigns.set(list);
          const currentId = this.selectedProjectId();
          if (!list.some((c) => c.id === currentId)) {
            this.selectedProjectId.set(list[0].id);
          }
        }
      },
      error: () => {
        this.loadingCampaigns.set(false);
      },
    });

    this.route.queryParamMap.subscribe((params) => {
      const proy = params.get('proyecto');
      if (proy && !Number.isNaN(Number(proy))) {
        this.selectedProjectId.set(Number(proy));
      }
    });
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img && img.src !== FALLBACK_CAMPAIGN_IMAGE) {
      img.src = FALLBACK_CAMPAIGN_IMAGE;
    }
  }

  setType(type: 'unica' | 'recurrente'): void {
    this.selectedType.set(type);
  }

  selectAmount(amount: number): void {
    this.selectedAmount.set(amount);
    this.isCustomAmount.set(false);
    this.donateForm.patchValue({ customAmount: '' });
  }

  onCustomAmount(event: Event): void {
    const raw = (event.target as HTMLInputElement).value;
    const val = parseFloat(raw) || 0;
    this.isCustomAmount.set(true);
    this.selectedAmount.set(val);
  }

  selectProject(id: number): void {
    this.selectedProjectId.set(id);
  }

  selectPay(method: PaymentMethodType): void {
    this.selectedPay.set(method);
    this.applyCardValidators(method);
  }

  private applyCardValidators(method: PaymentMethodType): void {
    const { cardNumber, cardExpiry, cardCvv } = this.donateForm.controls;
    if (method === 'card') {
      cardNumber.setValidators([Validators.required, Validators.pattern(/^\d{13,19}$/)]);
      cardExpiry.setValidators([
        Validators.required,
        Validators.pattern(/^(0[1-9]|1[0-2])\/\d{2}$/),
      ]);
      cardCvv.setValidators([Validators.required, Validators.pattern(/^\d{3,4}$/)]);
    } else {
      cardNumber.clearValidators();
      cardExpiry.clearValidators();
      cardCvv.clearValidators();
    }
    cardNumber.updateValueAndValidity({ emitEvent: false });
    cardExpiry.updateValueAndValidity({ emitEvent: false });
    cardCvv.updateValueAndValidity({ emitEvent: false });
  }

  openDonateModal(): void {
    this.alertMessage.set('');
    this.donateForm.markAllAsTouched();

    if (this.donateForm.invalid || this.amountInvalid()) {
      this.toast.error('Por favor revisa los campos obligatorios de tu donación.');
      return;
    }

    this.modalOpen.set(true);
  }

  closeDonateModal(): void {
    this.modalOpen.set(false);
  }

  confirmDonation(): void {
    const { donorName, donorEmail, donorPhone } = this.donateForm.getRawValue();
    const name = donorName.trim();
    const email = donorEmail.trim();
    const phone = donorPhone.trim();
    const campaignId = Number(this.selectedProjectId() || 1);
    const amount = Number(this.selectedAmount());
    const method = this.selectedPay();

    this.closeDonateModal();
    this.processing.set(true);

    this.donationService
      .createDonation({
        campaignId,
        amount,
        donorName: name,
        donorEmail: email,
        donorPhone: phone,
        paymentMethod: method,
        donationType: this.selectedType(),
        message: `Donación ${this.selectedType()} de ${name}`,
      })
      .subscribe({
        next: (res) => {
          this.processing.set(false);
          const don = res.donation;
          const campTitle =
            don?.campaignTitle || this.selectedProject()?.title || 'Proyecto Social';

          if ((method === 'wallet' || method === 'paypal') && don?.id && res.checkoutToken) {
            this.toast.info('Redirigiendo a la pasarela de autorización de pago...');
            this.router.navigate(['/pagos'], {
              queryParams: {
                donationId: don.id,
                checkoutToken: res.checkoutToken,
                method,
                amount,
                campaign: campTitle,
              },
              state: {
                donationId: don.id,
                checkoutToken: res.checkoutToken,
                method,
                amount,
                campaign: campTitle,
              },
            });
            return;
          }

          const voucher =
            don?.voucherNumber || `REC-2026-${String(don?.id || 1).padStart(5, '0')}`;
          const txCode = don?.codeReferential || 'TX-CONFIRMADO';

          this.toast.success(
            `¡Gracias ${name.split(' ')[0]}! Donación registrada con recibo ${voucher} 🌱`
          );
          this.alertType.set('success');
          this.alertMessage.set(
            `¡Donación confirmada! Recibo N° ${voucher} · Transacción ${txCode} por S/${amount.toFixed(
              2
            )} para "${campTitle}".`
          );

          if (this.auth.currentRole() === 'USUARIO') {
            setTimeout(() => {
              this.router.navigate(['/dashboard'], { fragment: 'mis-donaciones' });
            }, 1500);
          }
        },
        error: (err) => {
          this.processing.set(false);
          const msg = extractApiErrorMessage(err, 'No se pudo procesar la donación.');
          this.alertType.set('error');
          this.alertMessage.set(msg);
          this.toast.error(msg);
        },
      });
  }
}
