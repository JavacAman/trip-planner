import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { ItineraryComponent } from './itinerary.component';
import { TripService } from '../../core/services/trip.service';
import { Trip, DayItinerary } from '../../core/models/trip.model';

describe('ItineraryComponent — Smart Re-planner', () => {
  let component: ItineraryComponent;
  let tripServiceSpy: jasmine.SpyObj<TripService>;

  const sampleDay: DayItinerary = { id: 1, dayNumber: 1, theme: 'Arrival', activities: [] };
  const sampleTrip: Trip = {
    id: 99,
    destination: 'Tokyo',
    numberOfDays: 3,
    budgetType: 'MEDIUM',
    interests: ['food'],
    itinerary: [sampleDay],
    estimatedFlightCost: 0,
    estimatedAccommodationCost: 0,
    estimatedFoodCost: 0,
    estimatedActivitiesCost: 0,
    totalEstimatedBudget: 0,
    status: 'COMPLETED',
    hotelSuggestions: '',
    createdAt: '',
    updatedAt: ''
  };

  beforeEach(() => {
    tripServiceSpy = jasmine.createSpyObj('TripService', ['getTripById', 'regenerateDay']);
    tripServiceSpy.getTripById.and.returnValue(of({ success: true, message: 'ok', data: sampleTrip, timestamp: '' }));

    TestBed.configureTestingModule({
      imports: [ItineraryComponent],
      providers: [
        { provide: TripService, useValue: tripServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '99' } } }
        }
      ]
    });

    const fixture = TestBed.createComponent(ItineraryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('exposes exactly the four required issue options', () => {
    const types = component.issueOptions.map(o => o.type);
    expect(types).toEqual(['BAD_WEATHER', 'NOT_FEELING_WELL', 'OVER_BUDGET', 'RUNNING_LATE']);
  });

  it('toggles the issue menu for a given day', () => {
    component.toggleIssueMenu(1);
    expect(component.issueMenuOpenFor).toBe(1);

    component.toggleIssueMenu(1);
    expect(component.issueMenuOpenFor).toBeNull();
  });

  it('reports a bad-weather issue by regenerating only that day with a weather-specific instruction', () => {
    tripServiceSpy.regenerateDay.and.returnValue(of({ success: true, message: 'ok', data: sampleTrip, timestamp: '' }));

    component.reportIssue(sampleDay, 'BAD_WEATHER');

    expect(tripServiceSpy.regenerateDay).toHaveBeenCalledTimes(1);
    const [tripId, dayNumber, request] = tripServiceSpy.regenerateDay.calls.mostRecent().args;
    expect(tripId).toBe(99);
    expect(dayNumber).toBe(1);
    expect(request.instruction.toLowerCase()).toContain('weather');
    expect(component.regeneratingDay).toBeNull();
  });

  it('clears regeneratingDay state if the regeneration request fails', () => {
    tripServiceSpy.regenerateDay.and.returnValue(throwError(() => new Error('boom')));

    component.reportIssue(sampleDay, 'OVER_BUDGET');

    expect(component.regeneratingDay).toBeNull();
    expect(component.activeIssueLabel).toBeNull();
  });
});
