import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import { CreateUserRequest, UpdateUserRequest, UserResponse } from '../models/auth.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/api/users`;

  getAll(): Observable<UserResponse[]> {
    return this.http
      .get<UserResponse[]>(this.apiUrl)
      .pipe(catchError((err) => throwError(() => err)));
  }

  getById(id: number): Observable<UserResponse> {
    return this.http
      .get<UserResponse>(`${this.apiUrl}/${id}`)
      .pipe(catchError((err) => throwError(() => err)));
  }

  create(payload: CreateUserRequest): Observable<UserResponse> {
    return this.http
      .post<UserResponse>(this.apiUrl, payload)
      .pipe(catchError((err) => throwError(() => err)));
  }

  update(id: number, payload: UpdateUserRequest): Observable<UserResponse> {
    return this.http
      .patch<UserResponse>(`${this.apiUrl}/${id}`, payload)
      .pipe(catchError((err) => throwError(() => err)));
  }

  delete(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.apiUrl}/${id}`)
      .pipe(catchError((err) => throwError(() => err)));
  }

  uploadAvatar(id: number, file: File): Observable<UserResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<UserResponse>(`${this.apiUrl}/${id}/avatar`, formData)
      .pipe(catchError((err) => throwError(() => err)));
  }

  deleteAvatar(id: number): Observable<UserResponse> {
    return this.http
      .delete<UserResponse>(`${this.apiUrl}/${id}/avatar`)
      .pipe(catchError((err) => throwError(() => err)));
  }
}
