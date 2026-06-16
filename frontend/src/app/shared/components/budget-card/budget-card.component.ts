import { Component, Input } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { Trip } from '../../../core/models/trip.model';

@Component({
  selector: 'app-budget-card',
  standalone: true,
  imports: [CommonModule, CurrencyPipe],
  template: `
    <div class="card">
      <h3 class="text-lg font-semibold text-gray-800 mb-4">💰 Budget Estimate</h3>
      <div class="space-y-3">
        @for (item of budgetItems; track item.label) {
          <div class="flex justify-between items-center">
            <span class="text-gray-600 text-sm flex items-center gap-2">
              <span>{{ item.icon }}</span> {{ item.label }}
            </span>
            <span class="font-medium text-gray-800">
              {{ item.value | currency:'USD':'symbol':'1.0-0' }}
            </span>
          </div>
        }
        <hr class="border-gray-200" />
        <div class="flex justify-between items-center">
          <span class="font-semibold text-gray-900">Total</span>
          <span class="text-xl font-bold text-primary-600">
            {{ trip.totalEstimatedBudget | currency:'USD':'symbol':'1.0-0' }}
          </span>
        </div>
        <p class="text-xs text-gray-400 mt-2">
          * Estimates based on {{ trip.budgetType | titlecase }} budget preferences.
          Actual costs may vary.
        </p>
      </div>
    </div>
  `
})
export class BudgetCardComponent {
  @Input({ required: true }) trip!: Trip;

  get budgetItems() {
    return [
      { icon: '✈️', label: 'Flights',       value: this.trip.estimatedFlightCost },
      { icon: '🏨', label: 'Accommodation', value: this.trip.estimatedAccommodationCost },
      { icon: '🍽️', label: 'Food',          value: this.trip.estimatedFoodCost },
      { icon: '🎭', label: 'Activities',    value: this.trip.estimatedActivitiesCost }
    ];
  }
}
