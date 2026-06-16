import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { jwtInterceptor } from './jwt.interceptor';
import { AuthService } from '../services/auth.service';

describe('jwtInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getToken', 'logout']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } }
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('attaches the Authorization header when a token is present', () => {
    authServiceSpy.getToken.and.returnValue('jwt-token');

    httpClient.get('/api/v1/trips').subscribe();

    const req = httpMock.expectOne('/api/v1/trips');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush({});
  });

  it('does not attach an Authorization header when no token is present', () => {
    authServiceSpy.getToken.and.returnValue(null);

    httpClient.get('/api/v1/trips').subscribe();

    const req = httpMock.expectOne('/api/v1/trips');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('logs out and redirects on a 401 response', () => {
    authServiceSpy.getToken.and.returnValue('expired-token');
    const router = TestBed.inject(Router);

    httpClient.get('/api/v1/trips').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/v1/trips');
    req.flush({ message: 'unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});
