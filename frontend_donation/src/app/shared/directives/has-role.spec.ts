import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HasRole } from './has-role';

@Component({
  standalone: true,
  imports: [HasRole],
  template: `<div *appHasRole="'VISITANTE'" id="visitor-block">Visitante</div>`,
})
class TestHostComponent {}

describe('HasRole', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('should render content when role matches', async () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('#visitor-block')).toBeTruthy();
  });
});

