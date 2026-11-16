import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { StorageService } from './storage';

export interface ContactMessagePayload {
  name: string;
  email: string;
  subject: string;
  message: string;
}

export interface ContactSubmissionRecord extends ContactMessagePayload {
  submittedAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class ContactService {
  private readonly storage = inject(StorageService);
  private readonly contactStorageKey = 'semillas_contact_submissions';
  private readonly newsletterStorageKey = 'semillas_newsletter_subscribers';

  submitContactMessage(payload: ContactMessagePayload): Observable<ContactSubmissionRecord> {
    const cleanRecord: ContactSubmissionRecord = {
      name: payload.name.trim(),
      email: payload.email.trim().toLowerCase(),
      subject: payload.subject.trim(),
      message: payload.message.trim(),
      submittedAt: new Date().toISOString(),
    };

    const existing = this.readList<ContactSubmissionRecord>(this.contactStorageKey);
    this.storage.setItem(this.contactStorageKey, JSON.stringify([cleanRecord, ...existing.slice(0, 19)]));

    return of(cleanRecord);
  }

  subscribeNewsletter(email: string): Observable<{ email: string; subscribedAt: string }> {
    const normalizedEmail = email.trim().toLowerCase();
    const existing = this.readList<string>(this.newsletterStorageKey);
    if (!existing.includes(normalizedEmail)) {
      this.storage.setItem(
        this.newsletterStorageKey,
        JSON.stringify([normalizedEmail, ...existing])
      );
    }
    return of({
      email: normalizedEmail,
      subscribedAt: new Date().toISOString(),
    });
  }

  private readList<T>(key: string): T[] {
    const raw = this.storage.getItem(key);
    if (!raw) return [];
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? (parsed as T[]) : [];
    } catch {
      return [];
    }
  }
}

