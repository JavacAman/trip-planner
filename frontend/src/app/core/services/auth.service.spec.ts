import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { AuthResponse } from '../models/auth.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const sampleAuthResponse: AuthResponse = {
    token: 'jwt-token',
    tokenType: 'Bearer',
    userId: 1,
    email: 'user@example.com',
    fullName: 'Test User',
    role: 'USER'
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } }
      ]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created with no authenticated user initially', () => {
    expect(service).toBeTruthy();
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.currentUser).toBeNull();
  });

  it('should store the token and user on successful login', () => {
    service.login({ email: 'user@example.com', password: 'password123' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, message: 'ok', data: sampleAuthResponse, timestamp: '' });

    expect(service.getToken()).toBe('jwt-token');
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.currentUser?.email).toBe('user@example.com');
  });

  it('should store the token and user on successful registration', () => {
    service.register({ fullName: 'Test User', email: 'user@example.com', password: 'password123' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/register`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, message: 'ok', data: sampleAuthResponse, timestamp: '' });

    expect(service.getToken()).toBe('jwt-token');
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('should clear stored credentials and redirect to login on logout', () => {
    const router = TestBed.inject(Router);
    service.login({ email: 'user@example.com', password: 'password123' }).subscribe();
    httpMock.expectOne(`${environment.apiUrl}/auth/login`)
      .flush({ success: true, message: 'ok', data: sampleAuthResponse, timestamp: '' });

    service.logout();

    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.currentUser).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});
