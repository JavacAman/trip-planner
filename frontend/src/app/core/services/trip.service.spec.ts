import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TripService } from './trip.service';
import { environment } from '../../../environments/environment';

describe('TripService', () => {
  let service: TripService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/trips`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TripService]
    });
    service = TestBed.inject(TripService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should POST a new trip request', () => {
    service.createTrip({ destination: 'Tokyo', numberOfDays: 5, budgetType: 'MEDIUM', interests: ['food'] })
      .subscribe();

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.destination).toBe('Tokyo');
    req.flush({ success: true, message: 'ok', data: {}, timestamp: '' });
  });

  it('should GET paginated user trips with query params', () => {
    service.getUserTrips(1, 5).subscribe();

    const req = httpMock.expectOne(r => r.url === apiUrl);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('5');
    req.flush({ success: true, message: 'ok', data: {}, timestamp: '' });
  });

  it('should POST a day regeneration request to the correct day endpoint', () => {
    service.regenerateDay(7, 2, { instruction: 'Bad weather — go indoors' }).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/7/days/2/regenerate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.instruction).toContain('Bad weather');
    req.flush({ success: true, message: 'ok', data: {}, timestamp: '' });
  });

  it('should DELETE an activity by id', () => {
    service.removeActivity(7, 42).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/7/activities/42`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, message: 'ok', data: {}, timestamp: '' });
  });
});
