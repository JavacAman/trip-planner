import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TripService } from '../../core/services/trip.service';
import { Activity, DayItinerary, Trip } from '../../core/models/trip.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { BudgetCardComponent } from '../../shared/components/budget-card/budget-card.component';

@Component({
  selector: 'app-itinerary',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, LoadingSpinnerComponent, BudgetCardComponent],
  template: `
    <div class="max-w-5xl mx-auto px-4 py-8">
      <!-- Back -->
      <a routerLink="/dashboard" class="text-primary-600 hover:underline text-sm flex items-center gap-1 mb-6">
        ← Back to My Trips
      </a>

      @if (loading) {
        <app-loading-spinner message="Loading your itinerary..." />
      }

      @if (!loading && trip) {
        <!-- Trip Header -->
        <div class="card mb-6">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h1 class="text-3xl font-bold text-gray-900">{{ trip.destination }}</h1>
              <div class="flex items-center gap-3 mt-2 flex-wrap">
                <span class="text-gray-500 text-sm">📅 {{ trip.numberOfDays }} days</span>
                <span class="text-gray-500 text-sm">💰 {{ trip.budgetType | titlecase }}</span>
                <span [ngClass]="getStatusClass(trip.status)" class="badge">{{ trip.status | titlecase }}</span>
              </div>
              <div class="flex flex-wrap gap-1.5 mt-3">
                @for (interest of trip.interests; track interest) {
                  <span class="badge-blue">{{ interest }}</span>
                }
              </div>
            </div>
            <button (click)="showAddActivity = true"
                    class="btn-primary text-sm flex-shrink-0">
              + Add Activity
            </button>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <!-- Itinerary Days -->
          <div class="lg:col-span-2 space-y-4">
            @for (day of trip.itinerary; track day.id) {
              <div class="card">
                <div class="flex items-center justify-between mb-4">
                  <div>
                    <h2 class="text-lg font-bold text-gray-900">Day {{ day.dayNumber }}</h2>
                    @if (day.theme) {
                      <p class="text-primary-600 text-sm font-medium">{{ day.theme }}</p>
                    }
                  </div>
                  <div class="flex items-center gap-2">
                    <div class="relative">
                      <button (click)="toggleIssueMenu(day.dayNumber)"
                              class="text-xs btn-secondary flex items-center gap-1"
                              [disabled]="regeneratingDay === day.dayNumber">
                        ⚠️ Report Issue
                      </button>
                      @if (issueMenuOpenFor === day.dayNumber) {
                        <div class="absolute right-0 mt-1 w-48 bg-white border border-gray-200 rounded-lg shadow-lg z-10 overflow-hidden">
                          @for (issue of issueOptions; track issue.type) {
                            <button (click)="reportIssue(day, issue.type)"
                                    class="w-full text-left text-sm px-3 py-2 hover:bg-gray-50 flex items-center gap-2">
                              <span>{{ issue.icon }}</span>
                              <span>{{ issue.label }}</span>
                            </button>
                          }
                        </div>
                      }
                    </div>
                    <button (click)="openRegenerateDay(day)"
                            class="text-xs btn-secondary flex items-center gap-1"
                            [disabled]="regeneratingDay === day.dayNumber">
                      @if (regeneratingDay === day.dayNumber) {
                        <svg class="animate-spin h-3 w-3" viewBox="0 0 24 24" fill="none">
                          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.4 0 0 5.4 0 12h4z"/>
                        </svg>
                      } @else {
                        🔄
                      }
                      Regenerate
                    </button>
                  </div>
                </div>
                @if (regeneratingDay === day.dayNumber && activeIssueLabel) {
                  <p class="text-xs text-primary-600 mb-2">AI is replanning this day for: {{ activeIssueLabel }}…</p>
                }

                <div class="space-y-3">
                  @for (activity of day.activities; track activity.id) {
                    <div class="flex gap-3 p-3 bg-gray-50 rounded-lg group">
                      <div class="flex-shrink-0 mt-0.5">
                        <span class="text-lg">{{ getTimeIcon(activity.timeOfDay) }}</span>
                      </div>
                      <div class="flex-1 min-w-0">
                        <div class="flex items-start justify-between gap-2">
                          <h4 class="font-medium text-gray-900 text-sm">{{ activity.title }}</h4>
                          <button (click)="removeActivity(activity)"
                                  class="text-gray-300 hover:text-red-500 transition-colors flex-shrink-0 opacity-0 group-hover:opacity-100 text-lg">
                            ×
                          </button>
                        </div>
                        @if (activity.description) {
                          <p class="text-gray-500 text-xs mt-0.5">{{ activity.description }}</p>
                        }
                        <div class="flex flex-wrap gap-2 mt-1.5">
                          @if (activity.location) {
                            <span class="text-xs text-gray-400">📍 {{ activity.location }}</span>
                          }
                          @if (activity.estimatedDuration) {
                            <span class="text-xs text-gray-400">⏱️ {{ activity.estimatedDuration }}</span>
                          }
                          @if (activity.estimatedCost) {
                            <span class="text-xs text-green-600 font-medium">{{ activity.estimatedCost }}</span>
                          }
                        </div>
                      </div>
                    </div>
                  }
                </div>
              </div>
            }
          </div>

          <!-- Sidebar -->
          <div class="space-y-4">
            @if (trip.totalEstimatedBudget) {
              <app-budget-card [trip]="trip" />
            }

            <!-- Hotel Suggestions -->
            @if (parsedHotels.length > 0) {
              <div class="card">
                <h3 class="text-lg font-semibold text-gray-800 mb-4">🏨 Hotel Suggestions</h3>
                <div class="space-y-3">
                  @for (hotel of parsedHotels; track hotel.name) {
                    <div class="border border-gray-100 rounded-lg p-3">
                      <div class="flex items-start justify-between gap-2">
                        <h4 class="font-medium text-gray-900 text-sm">{{ hotel.name }}</h4>
                        <span class="text-yellow-500 text-xs flex-shrink-0">⭐ {{ hotel.rating }}</span>
                      </div>
                      <div class="flex items-center gap-2 mt-1">
                        <span class="badge-purple text-xs">{{ hotel.category }}</span>
                        <span class="text-green-600 text-xs font-medium">{{ hotel.pricePerNight }}/night</span>
                      </div>
                      @if (hotel.bookingTip) {
                        <p class="text-xs text-gray-400 mt-1.5">💡 {{ hotel.bookingTip }}</p>
                      }
                    </div>
                  }
                </div>
              </div>
            }
          </div>
        </div>
      }

      <!-- Regenerate Day Modal -->
      @if (showRegenerateModal && selectedDay) {
        <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div class="bg-white rounded-xl p-6 max-w-md w-full mx-4">
            <h3 class="font-semibold text-gray-900 mb-1">Regenerate Day {{ selectedDay.dayNumber }}</h3>
            <p class="text-gray-500 text-sm mb-4">Tell AI how to improve this day</p>
            <form [formGroup]="regenerateForm" (ngSubmit)="regenerateDay()">
              <textarea formControlName="instruction"
                        class="form-input h-24 resize-none"
                        placeholder="e.g. Add more outdoor activities, focus on local cuisine, avoid tourist traps...">
              </textarea>
              <div class="flex gap-3 mt-4">
                <button type="button" (click)="showRegenerateModal = false" class="btn-secondary flex-1">Cancel</button>
                <button type="submit" class="btn-primary flex-1" [disabled]="regeneratingDay !== null">
                  Regenerate
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Add Activity Modal -->
      @if (showAddActivity) {
        <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div class="bg-white rounded-xl p-6 max-w-md w-full mx-4">
            <h3 class="font-semibold text-gray-900 mb-4">Add Activity</h3>
            <form [formGroup]="addActivityForm" (ngSubmit)="addActivity()" class="space-y-3">
              <div>
                <label class="form-label">Activity title *</label>
                <input type="text" formControlName="title" class="form-input" placeholder="Visit the museum" />
              </div>
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="form-label">Day *</label>
                  <select formControlName="dayNumber" class="form-input">
                    @for (day of trip?.itinerary; track day.dayNumber) {
                      <option [value]="day.dayNumber">Day {{ day.dayNumber }}</option>
                    }
                  </select>
                </div>
                <div>
                  <label class="form-label">Time of day</label>
                  <select formControlName="timeOfDay" class="form-input">
                    <option value="">Any time</option>
                    <option>Morning</option>
                    <option>Afternoon</option>
                    <option>Evening</option>
                  </select>
                </div>
              </div>
              <div>
                <label class="form-label">Location</label>
                <input type="text" formControlName="location" class="form-input" placeholder="City center" />
              </div>
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="form-label">Est. duration</label>
                  <input type="text" formControlName="estimatedDuration" class="form-input" placeholder="2 hours" />
                </div>
                <div>
                  <label class="form-label">Est. cost</label>
                  <input type="text" formControlName="estimatedCost" class="form-input" placeholder="$15" />
                </div>
              </div>
              <div class="flex gap-3 pt-2">
                <button type="button" (click)="showAddActivity = false" class="btn-secondary flex-1">Cancel</button>
                <button type="submit" class="btn-primary flex-1" [disabled]="addingActivity">
                  {{ addingActivity ? 'Adding...' : 'Add Activity' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `
})
export class ItineraryComponent implements OnInit {
  trip: Trip | null = null;
  loading = true;
  showRegenerateModal = false;
  showAddActivity = false;
  selectedDay: DayItinerary | null = null;
  regeneratingDay: number | null = null;
  addingActivity = false;
  parsedHotels: any[] = [];

  // Smart Re-planner: quick-pick issue options, each mapped to a tailored regeneration instruction
  issueMenuOpenFor: number | null = null;
  activeIssueLabel: string | null = null;
  issueOptions: { type: string; icon: string; label: string; instruction: string }[] = [
    {
      type: 'BAD_WEATHER',
      icon: '🌧️',
      label: 'Bad Weather',
      instruction: 'Bad weather is forecast for this day. Replace all outdoor activities with creative indoor alternatives (museums, indoor markets, workshops, cafes, indoor attractions) that match the same theme, location area, and budget level.'
    },
    {
      type: 'NOT_FEELING_WELL',
      icon: '🤒',
      label: 'Not Feeling Well',
      instruction: 'The traveler is not feeling well today. Replace the plan with a lighter, low-energy day: shorter activities, more rest time, and options close to the accommodation with minimal physical exertion.'
    },
    {
      type: 'OVER_BUDGET',
      icon: '💸',
      label: 'Over Budget',
      instruction: 'The traveler has gone over budget. Replace today\'s activities with more affordable or free alternatives that still match the same interests and theme.'
    },
    {
      type: 'RUNNING_LATE',
      icon: '🕐',
      label: 'Running Late',
      instruction: 'The traveler is running late today. Compress and reorder today\'s activities into a shorter, more efficient plan, dropping or shortening the lowest-priority items.'
    }
  ];

  regenerateForm: FormGroup;
  addActivityForm: FormGroup;

  constructor(
    private route: ActivatedRoute,
    private tripService: TripService,
    private fb: FormBuilder
  ) {
    this.regenerateForm = this.fb.group({
      instruction: ['', Validators.required]
    });

    this.addActivityForm = this.fb.group({
      title: ['', Validators.required],
      dayNumber: [1, Validators.required],
      timeOfDay: [''],
      location: [''],
      estimatedDuration: [''],
      estimatedCost: ['']
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.tripService.getTripById(id).subscribe({
      next: (res) => {
        this.trip = res.data;
        this.parseHotels();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  parseHotels(): void {
    if (!this.trip?.hotelSuggestions) return;
    try {
      const parsed = JSON.parse(this.trip.hotelSuggestions);
      this.parsedHotels = parsed.hotels || [];
    } catch {
      this.parsedHotels = [];
    }
  }

  openRegenerateDay(day: DayItinerary): void {
    this.selectedDay = day;
    this.regenerateForm.reset();
    this.showRegenerateModal = true;
  }

  regenerateDay(): void {
    if (!this.trip || !this.selectedDay || this.regenerateForm.invalid) return;
    this.regeneratingDay = this.selectedDay.dayNumber;
    this.showRegenerateModal = false;

    this.tripService.regenerateDay(this.trip.id, this.selectedDay.dayNumber, this.regenerateForm.value).subscribe({
      next: (res) => {
        this.trip = res.data;
        this.regeneratingDay = null;
      },
      error: () => { this.regeneratingDay = null; }
    });
  }

  toggleIssueMenu(dayNumber: number): void {
    this.issueMenuOpenFor = this.issueMenuOpenFor === dayNumber ? null : dayNumber;
  }

  reportIssue(day: DayItinerary, issueType: string): void {
    if (!this.trip) return;
    const issue = this.issueOptions.find(o => o.type === issueType);
    if (!issue) return;

    this.issueMenuOpenFor = null;
    this.regeneratingDay = day.dayNumber;
    this.activeIssueLabel = `${issue.icon} ${issue.label}`;

    this.tripService.regenerateDay(this.trip.id, day.dayNumber, { instruction: issue.instruction }).subscribe({
      next: (res) => {
        this.trip = res.data;
        this.regeneratingDay = null;
        this.activeIssueLabel = null;
      },
      error: () => {
        this.regeneratingDay = null;
        this.activeIssueLabel = null;
      }
    });
  }

  removeActivity(activity: Activity): void {
    if (!this.trip) return;
    this.tripService.removeActivity(this.trip.id, activity.id).subscribe({
      next: (res) => { this.trip = res.data; }
    });
  }

  addActivity(): void {
    if (!this.trip || this.addActivityForm.invalid) return;
    this.addingActivity = true;

    this.tripService.addActivity(this.trip.id, this.addActivityForm.value).subscribe({
      next: (res) => {
        this.trip = res.data;
        this.showAddActivity = false;
        this.addActivityForm.reset({ dayNumber: 1 });
        this.addingActivity = false;
      },
      error: () => { this.addingActivity = false; }
    });
  }

  getTimeIcon(timeOfDay: string): string {
    return { Morning: '🌅', Afternoon: '☀️', Evening: '🌙' }[timeOfDay] ?? '🎯';
  }

  getStatusClass(status: string): string {
    return { COMPLETED: 'badge-green', GENERATING: 'badge-yellow', FAILED: 'badge-red' }[status] ?? 'badge-blue';
  }
}
