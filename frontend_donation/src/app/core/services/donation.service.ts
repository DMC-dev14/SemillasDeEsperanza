import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import {
  CaptureOrderPayPalRequest,
  ChargePaymentRequestDTO,
  ChargePaymentResponse,
  CreateOrderPayPalRequest,
  CreateOrderPayPalResponse,
  DonationCreatedResponse,
  DonationRequest,
  DonationResponse,
  MyDonationsResponse,
  PagedResponse,
  PaymentStatus,
  PublicFeedResponse,
  YapeTokenRequestDTO,
  YapeTokenResponse,
} from '../models/donation.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class DonationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/api/donations`;
  private readonly paymentGatewayUrl = `${environment.apiBaseUrl}/api/payment-gateway`;
  private readonly paypalUrl = `${environment.apiBaseUrl}/api/paypal`;

  createDonation(payload: DonationRequest): Observable<DonationCreatedResponse> {
    return this.http
      .post<DonationCreatedResponse>(this.apiUrl, payload)
      .pipe(catchError((err) => throwError(() => err)));
  }

  getMyDonations(): Observable<MyDonationsResponse> {
    return this.http
      .get<MyDonationsResponse>(`${this.apiUrl}/me`)
      .pipe(catchError((err) => throwError(() => err)));
  }

  getPublicFeed(): Observable<PublicFeedResponse> {
    return this.http
      .get<PublicFeedResponse>(`${this.apiUrl}/public-feed`)
      .pipe(catchError((err) => throwError(() => err)));
  }

  getAllDonations(): Observable<DonationResponse[]> {
    return this.http
      .get<DonationResponse[]>(this.apiUrl)
      .pipe(catchError((err) => throwError(() => err)));
  }

  getDonationsPage(page: number, size: number): Observable<PagedResponse<DonationResponse>> {
    return this.http
      .get<PagedResponse<DonationResponse>>(`${this.apiUrl}/page`, {
        params: { page, size },
      })
      .pipe(catchError((err) => throwError(() => err)));
  }

  getMyDonationsPage(page: number, size: number): Observable<PagedResponse<DonationResponse>> {
    return this.http
      .get<PagedResponse<DonationResponse>>(`${this.apiUrl}/me/page`, {
        params: { page, size },
      })
      .pipe(catchError((err) => throwError(() => err)));
  }

  updateStatus(id: number, status: PaymentStatus): Observable<DonationResponse> {
    return this.http
      .patch<DonationResponse>(`${this.apiUrl}/${id}/status`, { status })
      .pipe(catchError((err) => throwError(() => err)));
  }

  generateYapeToken(payload: YapeTokenRequestDTO): Observable<YapeTokenResponse> {
    return this.http
      .post<YapeTokenResponse>(`${this.paymentGatewayUrl}/yape/token`, payload)
      .pipe(catchError((err) => throwError(() => err)));
  }

  chargeYapePayment(payload: ChargePaymentRequestDTO): Observable<ChargePaymentResponse> {
    return this.http
      .post<ChargePaymentResponse>(`${this.paymentGatewayUrl}/charge-payment`, payload)
      .pipe(catchError((err) => throwError(() => err)));
  }

  createPayPalOrder(payload: CreateOrderPayPalRequest): Observable<CreateOrderPayPalResponse> {
    return this.http
      .post<CreateOrderPayPalResponse>(`${this.paypalUrl}/orders`, payload)
      .pipe(catchError((err) => throwError(() => err)));
  }

  capturePayPalOrder(
    orderId: string,
    payload: CaptureOrderPayPalRequest
  ): Observable<DonationResponse> {
    return this.http
      .post<DonationResponse>(`${this.paypalUrl}/orders/${orderId}/capture`, payload)
      .pipe(catchError((err) => throwError(() => err)));
  }
}
