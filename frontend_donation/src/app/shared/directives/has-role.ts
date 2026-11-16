import { Directive, effect, inject, input, TemplateRef, ViewContainerRef } from '@angular/core';
import { AuthService } from '../../core/services/auth';

@Directive({
  selector: '[appHasRole]',
  standalone: true,
})
export class HasRole {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly auth = inject(AuthService);

  readonly appHasRole = input<string>('');
  private hasView = false;

  constructor() {
    effect(() => {
      const requiredRole = this.appHasRole();
      const currentRole = this.auth.currentRole();
      const matches = currentRole === requiredRole;

      if (matches && !this.hasView) {
        this.viewContainer.createEmbeddedView(this.templateRef);
        this.hasView = true;
      } else if (!matches && this.hasView) {
        this.viewContainer.clear();
        this.hasView = false;
      }
    });
  }
}
