import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TripService } from '../../core/services/trip.service';

const INTERESTS = ['Food', 'Culture', 'Adventure', 'Shopping', 'Nature', 'History', 'Art', 'Nightlife', 'Relaxation', 'Sports'];

@Component({
  selector: 'app-trip-planner',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="max-w-2xl mx-auto px-4 py-10">
      <!-- Back link -->
      <a routerLink="/dashboard" class="text-primary-600 hover:underline text-sm flex items-center gap-1 mb-6">
        ← Back to dashboard
      </a>

      <div class="card">
        <div class="mb-6">
          <h1 class="text-2xl font-bold text-gray-900">Plan a New Trip</h1>
          <p class="text-gray-500 text-sm mt-1">
            Tell our AI about your dream trip and we'll build a personalized itinerary in seconds.
          </p>
        </div>

        @if (generating) {
          <div class="py-16 text-center">
            <div class="text-6xl mb-4 animate-bounce">🤖</div>
            <h3 class="text-lg font-semibold text-gray-800 mb-2">Generating your itinerary...</h3>
            <p class="text-gray-500 text-sm">Our AI is crafting a personalized {{ tripForm.get('numberOfDays')?.value }}-day plan for {{ tripForm.get('destination')?.value }}</p>
            <div class="mt-6 flex justify-center">
              <div class="w-48 h-2 bg-gray-100 rounded-full overflow-hidden">
                <div class="h-full bg-primary-500 rounded-full animate-pulse" style="width: 70%"></div>
              </div>
            </div>
          </div>
        } @else {
          <form [formGroup]="tripForm" (ngSubmit)="onSubmit()" class="space-y-6">
            <!-- Destination -->
            <div>
              <label class="form-label">🌍 Destination</label>
              <input type="text" formControlName="destination"
                     class="form-input"
                     placeholder="e.g. Tokyo, Japan · Paris, France · Bali, Indonesia" />
              @if (f['destination'].invalid && f['destination'].touched) {
                <p class="form-error">Destination is required</p>
              }
            </div>

            <!-- Number of Days -->
            <div>
              <label class="form-label">📅 Number of Days: <span class="text-primary-600 font-semibold">{{ f['numberOfDays'].value }}</span></label>
              <input type="range" formControlName="numberOfDays"
                     min="1" max="14" step="1"
                     class="w-full accent-primary-600 mt-2" />
              <div class="flex justify-between text-xs text-gray-400 mt-1">
                <span>1 day</span>
                <span>14 days</span>
              </div>
            </div>

            <!-- Budget Type -->
            <div>
              <label class="form-label">💰 Budget Level</label>
              <div class="grid grid-cols-3 gap-3 mt-2">
                @for (budget of budgetOptions; track budget.value) {
                  <button type="button"
                          (click)="setBudget(budget.value)"
                          [ngClass]="f['budgetType'].value === budget.value
                            ? 'border-primary-500 bg-primary-50 text-primary-700'
                            : 'border-gray-200 text-gray-600 hover:border-gray-300'"
                          class="border-2 rounded-lg p-3 text-center cursor-pointer transition-all">
                    <div class="text-2xl mb-1">{{ budget.emoji }}</div>
                    <div class="text-sm font-medium">{{ budget.label }}</div>
                    <div class="text-xs text-gray-400">{{ budget.hint }}</div>
                  </button>
                }
              </div>
            </div>

            <!-- Interests -->
            <div>
              <label class="form-label">🎯 Interests <span class="text-gray-400 font-normal">(select at least 1)</span></label>
              <div class="flex flex-wrap gap-2 mt-2">
                @for (interest of availableInterests; track interest) {
                  <button type="button"
                          (click)="toggleInterest(interest)"
                          [ngClass]="isInterestSelected(interest)
                            ? 'bg-primary-600 text-white border-primary-600'
                            : 'bg-white text-gray-600 border-gray-300 hover:border-primary-400'"
                          class="px-3 py-1.5 rounded-full border text-sm font-medium transition-all">
                    {{ interest }}
                  </button>
                }
              </div>
              @if (tripForm.get('interests')?.invalid && tripForm.get('interests')?.touched) {
                <p class="form-error mt-1">Please select at least one interest</p>
              }
            </div>

            @if (errorMessage) {
              <div class="bg-red-50 border border-red-200 rounded-lg p-3">
                <p class="text-red-700 text-sm">{{ errorMessage }}</p>
              </div>
            }

            <button type="submit" class="btn-primary w-full py-3 text-base" [disabled]="generating">
              🚀 Generate AI Itinerary
            </button>
          </form>
        }
      </div>
    </div>
  `
})
export class TripPlannerComponent {
  tripForm: FormGroup;
  generating = false;
  errorMessage = '';
  availableInterests = INTERESTS;

  budgetOptions = [
    { value: 'LOW',    emoji: '💵', label: 'Budget',  hint: 'Hostels, street food' },
    { value: 'MEDIUM', emoji: '💳', label: 'Mid-Range', hint: 'Hotels, restaurants' },
    { value: 'HIGH',   emoji: '💎', label: 'Luxury',  hint: 'Resorts, fine dining' }
  ];

  constructor(
    private fb: FormBuilder,
    private tripService: TripService,
    private router: Router
  ) {
    this.tripForm = this.fb.group({
      destination: ['', [Validators.required, Validators.minLength(2)]],
      numberOfDays: [3, [Validators.required, Validators.min(1), Validators.max(30)]],
      budgetType: ['MEDIUM', Validators.required],
      interests: [[], Validators.required]
    });
  }

  get f() { return this.tripForm.controls; }

  setBudget(value: string): void {
    this.tripForm.patchValue({ budgetType: value });
  }

  toggleInterest(interest: string): void {
    const current: string[] = this.tripForm.get('interests')?.value || [];
    const updated = current.includes(interest)
      ? current.filter(i => i !== interest)
      : [...current, interest];
    this.tripForm.patchValue({ interests: updated });
  }

  isInterestSelected(interest: string): boolean {
    return (this.tripForm.get('interests')?.value || []).includes(interest);
  }

  onSubmit(): void {
    if (this.tripForm.invalid || (this.tripForm.get('interests')?.value?.length ?? 0) === 0) {
      this.tripForm.markAllAsTouched();
      this.errorMessage = 'Please fill in all required fields and select at least one interest.';
      return;
    }
    this.generating = true;
    this.errorMessage = '';

    this.tripService.createTrip(this.tripForm.value).subscribe({
      next: (res) => this.router.navigate(['/trips', res.data.id]),
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to generate itinerary. Please try again.';
        this.generating = false;
      }
    });
  }
}
