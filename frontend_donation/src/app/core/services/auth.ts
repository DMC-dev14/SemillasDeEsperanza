import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, Observable, of, tap, throwError } from 'rxjs';
import {
  AuthResponse,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
  UpdateProfileRequest,
  UserResponse,
} from '../models/auth.model';
import { StorageService } from './storage';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(StorageService);

  private readonly apiUrl = `${environment.apiBaseUrl}/api/auth`;
  private readonly tokenKey = 'auth_token';
  private readonly legacyTokenKey = 'jwtToken';
  private readonly userKey = 'auth_user';

  readonly currentUser = signal<UserResponse | null>(this.loadStoredUser());

  readonly isAuthenticated = computed(() => !!this.currentUser() && !!this.getToken());

  readonly currentRole = computed<'ADMINISTRADOR' | 'USUARIO' | 'VISITANTE'>(() => {
    const user = this.currentUser();
    if (!user || !this.getToken()) {
      return 'VISITANTE';
    }
    return user.rolName === 'ADMINISTRADOR' ? 'ADMINISTRADOR' : 'USUARIO';
  });

  private loadStoredUser(): UserResponse | null {
    const token = this.readValidToken();
    if (!token) {
      return null;
    }
    const raw = this.storage.getItem(this.userKey);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as UserResponse;
    } catch {
      return null;
    }
  }

  private isJwtExpired(token: string): boolean {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return false;
    }
    try {
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(base64)) as { exp?: number };
      if (typeof payload.exp === 'number') {
        return payload.exp * 1000 <= Date.now();
      }
      return false;
    } catch {
      return false;
    }
  }

  private readValidToken(): string | null {
    const token =
      this.storage.getItem(this.tokenKey) || this.storage.getItem(this.legacyTokenKey);
    if (!token) {
      return null;
    }
    if (this.isJwtExpired(token)) {
      this.storage.removeItem(this.tokenKey);
      this.storage.removeItem(this.legacyTokenKey);
      this.storage.removeItem(this.userKey);
      return null;
    }
    return token;
  }

  saveSession(response: AuthResponse): void {
    if (!response) return;
    if (response.token) {
      this.storage.setItem(this.tokenKey, response.token);
      this.storage.setItem(this.legacyTokenKey, response.token);
    }
    if (response.user) {
      this.storage.setItem(this.userKey, JSON.stringify(response.user));
      this.currentUser.set(response.user);
    }
  }

  updateStoredUser(user: UserResponse): void {
    if (!user) return;
    this.storage.setItem(this.userKey, JSON.stringify(user));
    this.currentUser.set(user);
  }

  clearSession(): void {
    this.storage.removeItem(this.tokenKey);
    this.storage.removeItem(this.legacyTokenKey);
    this.storage.removeItem(this.userKey);
    this.currentUser.set(null);
  }

  getToken(): string | null {
    const token = this.readValidToken();
    if (!token && this.currentUser() !== null) {
      this.currentUser.set(null);
    }
    return token;
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap((response) => this.saveSession(response)),
      catchError((err) => throwError(() => err))
    );
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, payload).pipe(
      tap((response) => this.saveSession(response)),
      catchError((err) => throwError(() => err))
    );
  }

  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/me`).pipe(
      tap((user) => this.updateStoredUser(user)),
      catchError((err) => throwError(() => err))
    );
  }

  updateProfile(payload: UpdateProfileRequest): Observable<AuthResponse> {
    return this.http.patch<AuthResponse>(`${this.apiUrl}/profile`, payload).pipe(
      tap((response) => this.saveSession(response)),
      catchError((err) => throwError(() => err))
    );
  }

  changePassword(payload: ChangePasswordRequest): Observable<void> {
    return this.http
      .post<void>(`${this.apiUrl}/change-password`, payload)
      .pipe(catchError((err) => throwError(() => err)));
  }

  forgotPassword(payload: ForgotPasswordRequest): Observable<void> {
    return this.http
      .post<void>(`${this.apiUrl}/forgot-password`, payload)
      .pipe(catchError((err) => throwError(() => err)));
  }

  resetPassword(payload: ResetPasswordRequest): Observable<void> {
    return this.http
      .post<void>(`${this.apiUrl}/reset-password`, payload)
      .pipe(catchError((err) => throwError(() => err)));
  }

  uploadAvatar(file: File): Observable<UserResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<UserResponse>(`${this.apiUrl}/me/avatar`, formData).pipe(
      tap((user) => this.updateStoredUser(user)),
      catchError((err) => throwError(() => err))
    );
  }

  deleteAvatar(): Observable<UserResponse> {
    return this.http.delete<UserResponse>(`${this.apiUrl}/me/avatar`).pipe(
      tap((user) => this.updateStoredUser(user)),
      catchError((err) => throwError(() => err))
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/logout`, {}).pipe(
      catchError(() => of(void 0)),
      tap(() => this.clearSession())
    );
  }
}
