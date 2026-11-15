import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 72;

export const PASSWORD_REGEX = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,72}$/;

export const RESET_PASSWORD_REGEX =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,72}$/;

export function createPasswordMatchValidator(
  passwordControlName: string,
  confirmControlName: string
): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const pw = group.get(passwordControlName)?.value;
    const confirm = group.get(confirmControlName)?.value;
    return pw && confirm && pw !== confirm ? { passwordMismatch: true } : null;
  };
}

export function extractApiErrorMessage(err: unknown, fallback: string): string {
  if (!err || typeof err !== 'object') {
    return fallback;
  }
  const httpErr = err as {
    error?: {
      errors?: string[];
      detail?: string;
      message?: string;
    };
    message?: string;
  };

  if (Array.isArray(httpErr.error?.errors) && httpErr.error.errors.length > 0) {
    return httpErr.error.errors.join(', ');
  }
  if (typeof httpErr.error?.detail === 'string' && httpErr.error.detail.trim()) {
    return httpErr.error.detail.trim();
  }
  if (typeof httpErr.error?.message === 'string' && httpErr.error.message.trim()) {
    return httpErr.error.message.trim();
  }
  return fallback;
}

export const FALLBACK_CAMPAIGN_IMAGE =
  "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='800' height='450' viewBox='0 0 800 450'><rect width='800' height='450' fill='%23E9EFDD'/><circle cx='400' cy='210' r='54' fill='%233D6B35' opacity='0.15'/><path d='M400 250V190M400 190C400 160 370 142 346 154M400 190C400 160 430 142 454 154' stroke='%233D6B35' stroke-width='8' stroke-linecap='round' stroke-linejoin='round' fill='none'/><text x='400' y='305' text-anchor='middle' font-family='Poppins, sans-serif' font-size='20' font-weight='600' fill='%233D6B35'>Semillas de Esperanza</text></svg>";

