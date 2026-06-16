import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard, guestGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('auth guards', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated']);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });
    router = TestBed.inject(Router);
  });

  it('authGuard allows access when authenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    expect(result).toBeTrue();
  });

  it('authGuard redirects to login when not authenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);
    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    expect(result).not.toBeTrue();
    expect(router.serializeUrl(result as any)).toBe('/auth/login');
  });

  it('guestGuard allows access when not authenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);
    const result = TestBed.runInInjectionContext(() => guestGuard({} as any, {} as any));
    expect(result).toBeTrue();
  });

  it('guestGuard redirects to dashboard when already authenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    const result = TestBed.runInInjectionContext(() => guestGuard({} as any, {} as any));
    expect(result).not.toBeTrue();
    expect(router.serializeUrl(result as any)).toBe('/dashboard');
  });
});
