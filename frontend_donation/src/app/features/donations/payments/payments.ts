import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth';
import { DonationService } from '../../../core/services/donation.service';
import { ToastService } from '../../../core/services/toast.service';
import { extractApiErrorMessage } from '../../../core/utils/password-validators';

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './payments.html',
  styleUrl: './payments.css',
})
export class PaymentsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly donationService = inject(DonationService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly donationId = input<number>(0);
  readonly checkoutToken = input<string>('');
  readonly paymentCompleted = output<void>();

  readonly routeDonationId = signal<number>(0);
  readonly routeCheckoutToken = signal<string>('');
  readonly paymentMethod = signal<'wallet' | 'paypal'>('wallet');
  readonly donationAmount = signal<number>(0);
  readonly campaignTitle = signal<string>('');
  readonly paypalOrderId = signal<string>('');
  readonly completed = signal<boolean>(false);

  readonly loading = signal(false);
  readonly errorMsg = signal('');

  readonly effectiveDonationId = computed(() => this.donationId() || this.routeDonationId());
  readonly effectiveCheckoutToken = computed(
    () => this.checkoutToken() || this.routeCheckoutToken()
  );
  readonly hasValidContext = computed(
    () => this.effectiveDonationId() > 0 && !!this.effectiveCheckoutToken()
  );

  readonly yapeForm = this.fb.nonNullable.group({
    phoneNumber: ['', [Validators.required, Validators.pattern(/^\d{9}$/)]],
    otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  ngOnInit(): void {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras?.state as
      | {
          donationId?: number;
          checkoutToken?: string;
          method?: 'wallet' | 'paypal';
          amount?: number;
          campaign?: string;
        }
      | undefined;

    if (state?.donationId) {
      this.routeDonationId.set(Number(state.donationId));
    }
    if (state?.checkoutToken) {
      this.routeCheckoutToken.set(state.checkoutToken);
    }
    if (state?.method === 'paypal' || state?.method === 'wallet') {
      this.paymentMethod.set(state.method);
    }
    if (state?.amount) {
      this.donationAmount.set(Number(state.amount));
    }
    if (state?.campaign) {
      this.campaignTitle.set(state.campaign);
    }

    this.route.queryParamMap.subscribe((params) => {
      const donationIdParam = params.get('donationId') || params.get('donacionId');
      const tokenParam =
        params.get('checkoutToken') || params.get('token') || params.get('checkout_token');
      const methodParam = params.get('method');
      const amountParam = params.get('amount');
      const campaignParam = params.get('campaign');

      if (donationIdParam && !Number.isNaN(Number(donationIdParam))) {
        this.routeDonationId.set(Number(donationIdParam));
      }
      if (tokenParam) {
        this.routeCheckoutToken.set(tokenParam);
      }
      if (methodParam === 'paypal' || methodParam === 'wallet') {
        this.paymentMethod.set(methodParam);
      }
      if (amountParam && !Number.isNaN(Number(amountParam))) {
        this.donationAmount.set(Number(amountParam));
      }
      if (campaignParam) {
        this.campaignTitle.set(campaignParam);
      }
    });
  }
  processYape(): void {
    this.errorMsg.set('');
    if (this.yapeForm.invalid) {
      this.yapeForm.markAllAsTouched();
      return;
    }

    const { phoneNumber, otp } = this.yapeForm.getRawValue();
    const donationId = this.effectiveDonationId();
    const checkoutToken = this.effectiveCheckoutToken();

    this.loading.set(true);

    this.donationService
      .generateYapeToken({
        donationId,
        checkoutToken,
        phoneNumber,
        otp,
      })
      .subscribe({
        next: (tokenRes) => {
          this.donationService
            .chargeYapePayment({
              donationId,
              checkoutToken,
              token: tokenRes.token,
            })
            .subscribe({
              next: () => {
                this.loading.set(false);
                this.completed.set(true);
                this.toast.success('¡Pago con Yape procesado exitosamente!');
                this.paymentCompleted.emit();
                this.redirectAfterCompletion();
              },
              error: (err) => {
                this.loading.set(false);
                this.errorMsg.set(
                  extractApiErrorMessage(err, 'Error al procesar el cobro en Yape.')
                );
              },
            });
        },
        error: (err) => {
          this.loading.set(false);
          this.errorMsg.set(
            extractApiErrorMessage(err, 'Código de aprobación Yape inválido.')
          );
        },
      });
  }

  processPayPal(): void {
    this.errorMsg.set('');
    const donationId = this.effectiveDonationId();
    const checkoutToken = this.effectiveCheckoutToken();

    this.loading.set(true);

    this.donationService
      .createPayPalOrder({
        donationId,
        checkoutToken,
      })
      .subscribe({
        next: (orderRes) => {
          this.paypalOrderId.set(orderRes.orderId);
          this.donationService
            .capturePayPalOrder(orderRes.orderId, {
              donationId,
              checkoutToken,
            })
            .subscribe({
              next: () => {
                this.loading.set(false);
                this.completed.set(true);
                this.toast.success('¡Pago con PayPal capturado exitosamente!');
                this.paymentCompleted.emit();
                this.redirectAfterCompletion();
              },
              error: (err) => {
                this.loading.set(false);
                this.errorMsg.set(
                  extractApiErrorMessage(err, 'Error al capturar la orden de PayPal.')
                );
              },
            });
        },
        error: (err) => {
          this.loading.set(false);
          this.errorMsg.set(
            extractApiErrorMessage(err, 'No se pudo iniciar la orden en PayPal.')
          );
        },
      });
  }

  private redirectAfterCompletion(): void {
    if (this.auth.currentRole() === 'USUARIO') {
      setTimeout(() => {
        this.router.navigate(['/dashboard'], { fragment: 'mis-donaciones' });
      }, 1600);
    }
  }
}
