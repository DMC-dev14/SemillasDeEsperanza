import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import { CampaignRequest, CampaignResponse, CampaignStatus } from '../models/campaign.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class CampaignService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/api/campaigns`;

  getPublicCampaigns(): Observable<CampaignResponse[]> {
    return this.http
      .get<CampaignResponse[]>(this.apiUrl)
      .pipe(catchError((err) => throwError(() => err)));
  }

  getActiveCampaigns(): Observable<CampaignResponse[]> {
    return this.http
      .get<CampaignResponse[]>(`${this.apiUrl}/active`)
      .pipe(catchError((err) => throwError(() => err)));
  }

  getCampaignById(id: number): Observable<CampaignResponse> {
    return this.http
      .get<CampaignResponse>(`${this.apiUrl}/${id}`)
      .pipe(catchError((err) => throwError(() => err)));
  }

  getAllForAdmin(): Observable<CampaignResponse[]> {
    return this.http
      .get<CampaignResponse[]>(`${this.apiUrl}/admin`)
      .pipe(catchError((err) => throwError(() => err)));
  }

  create(payload: CampaignRequest): Observable<CampaignResponse> {
    return this.http
      .post<CampaignResponse>(this.apiUrl, payload)
      .pipe(catchError((err) => throwError(() => err)));
  }

  update(id: number, payload: CampaignRequest): Observable<CampaignResponse> {
    return this.http
      .put<CampaignResponse>(`${this.apiUrl}/${id}`, payload)
      .pipe(catchError((err) => throwError(() => err)));
  }

  updateStatus(id: number, status: CampaignStatus): Observable<CampaignResponse> {
    return this.http
      .patch<CampaignResponse>(`${this.apiUrl}/${id}/status`, { status })
      .pipe(catchError((err) => throwError(() => err)));
  }

  delete(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.apiUrl}/${id}`)
      .pipe(catchError((err) => throwError(() => err)));
  }

  formatShortCurrency(val: number | null | undefined): string {
    const amount = Number(val || 0);
    if (amount >= 1000) {
      return `S/${(amount / 1000).toFixed(1)}k`;
    }
    return `S/${amount.toFixed(0)}`;
  }

  formatFullCurrency(val: number | null | undefined): string {
    const amount = Number(val || 0);
    return (
      'S/' +
      amount.toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      })
    );
  }

  uploadImage(file: File): Observable<{
    url: string;
    public_id?: string;
    publicId?: string;
  }> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<{
      url: string;
      public_id?: string;
      publicId?: string;
    }>(`${environment.apiBaseUrl}/api/images`, formData);
  }
}
