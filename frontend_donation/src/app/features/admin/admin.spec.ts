import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { AdminComponent } from './admin';

describe('AdminComponent', () => {
  let component: AdminComponent;
  let fixture: ComponentFixture<AdminComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call handleAdminCreateUser when clicking create user button', async () => {
    const spy = vi.spyOn(component, 'handleAdminCreateUser');
    component.openCreateUserModal();
    fixture.detectChanges();
    await fixture.whenStable();

    const btn = fixture.nativeElement.querySelector('#btn-admin-create-user') as HTMLButtonElement;
    expect(btn).toBeTruthy();
    btn.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(spy).toHaveBeenCalled();
  });

  it('should call handleSaveCampaign when clicking save campaign button', async () => {
    const spy = vi.spyOn(component, 'handleSaveCampaign');
    component.openCampaignModal();
    fixture.detectChanges();
    await fixture.whenStable();

    const btn = fixture.nativeElement.querySelector('#btn-save-campaign') as HTMLButtonElement;
    expect(btn).toBeTruthy();
    btn.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(spy).toHaveBeenCalled();
  });
});
