import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ContactService } from '../../core/services/contact.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './contact.html',
  styleUrl: './contact.css',
})
export class ContactComponent {
  private readonly fb = inject(FormBuilder);
  private readonly contactService = inject(ContactService);
  private readonly toast = inject(ToastService);

  readonly messageSent = signal(false);
  readonly sentFirstName = signal('');
  readonly sentEmail = signal('');

  readonly contactForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    subject: ['', [Validators.required, Validators.minLength(3)]],
    message: ['', [Validators.required, Validators.minLength(20)]],
  });

  handleContact(): void {
    if (this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      return;
    }

    const payload = this.contactForm.getRawValue();
    this.contactService.submitContactMessage(payload).subscribe((record) => {
      this.sentFirstName.set(record.name.split(' ')[0]);
      this.sentEmail.set(record.email);
      this.messageSent.set(true);
      this.toast.success('¡Mensaje enviado! Te responderemos en menos de 24 horas.');
    });
  }

  resetContact(): void {
    this.contactForm.reset();
    this.messageSent.set(false);
  }
}
