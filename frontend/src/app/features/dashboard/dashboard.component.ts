import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TripService } from '../../core/services/trip.service';
import { AuthService } from '../../core/services/auth.service';
import { Trip } from '../../core/models/trip.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, RouterLink, LoadingSpinnerComponent],
  template: `
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Header -->
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-3xl font-bold text-gray-900">
            My Trips
          </h1>
          <p class="text-gray-500 mt-1">
            Welcome back, {{ authService.currentUser?.fullName?.split(' ')?.[0] }}! Ready to explore?
          </p>
        </div>
        <a routerLink="/trips/new" class="btn-primary flex items-center gap-2">
          <span>+</span> Plan New Trip
        </a>
      </div>

      <!-- Loading State -->
      @if (loading) {
        <app-loading-spinner message="Loading your trips..." />
      }

      <!-- Empty State -->
      @if (!loading && trips.length === 0) {
        <div class="text-center py-20">
          <div class="text-7xl mb-4">🗺️</div>
          <h2 class="text-2xl font-semibold text-gray-700 mb-2">No trips yet</h2>
          <p class="text-gray-400 mb-6">Let AI plan your perfect adventure in seconds</p>
          <a routerLink="/trips/new" class="btn-primary px-8 py-3 text-base">
            Plan My First Trip ✈️
          </a>
        </div>
      }

      <!-- Trip Grid -->
      @if (!loading && trips.length > 0) {
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          @for (trip of trips; track trip.id) {
            <div class="card hover:shadow-md transition-shadow cursor-pointer group"
                 (click)="navigateToTrip(trip.id)">
              <!-- Status Banner -->
              <div class="flex justify-between items-start mb-3">
                <span [ngClass]="getStatusBadgeClass(trip.status)" class="badge">
                  {{ trip.status | titlecase }}
                </span>
                <button (click)="$event.stopPropagation(); confirmDelete(trip)"
                        class="text-gray-300 hover:text-red-500 transition-colors text-lg opacity-0 group-hover:opacity-100">
                  🗑️
                </button>
              </div>

              <!-- Destination -->
              <h3 class="text-xl font-bold text-gray-900 mb-1">{{ trip.destination }}</h3>
              <p class="text-gray-500 text-sm mb-3">
                {{ trip.numberOfDays }} day{{ trip.numberOfDays > 1 ? 's' : '' }} ·
                {{ trip.budgetType | titlecase }} budget
              </p>

              <!-- Interests -->
              <div class="flex flex-wrap gap-1.5 mb-4">
                @for (interest of trip.interests.slice(0, 3); track interest) {
                  <span class="badge-blue">{{ interest }}</span>
                }
                @if (trip.interests.length > 3) {
                  <span class="badge bg-gray-100 text-gray-600">+{{ trip.interests.length - 3 }}</span>
                }
              </div>

              <!-- Budget -->
              @if (trip.totalEstimatedBudget) {
                <div class="flex items-center justify-between pt-3 border-t border-gray-100">
                  <span class="text-xs text-gray-400">Est. budget</span>
                  <span class="font-semibold text-primary-600">
                    {{ trip.totalEstimatedBudget | currency:'USD':'symbol':'1.0-0' }}
                  </span>
                </div>
              }

              <!-- Created date -->
              <p class="text-xs text-gray-300 mt-2">
                Created {{ trip.createdAt | date:'MMM d, y' }}
              </p>
            </div>
          }
        </div>

        <!-- Pagination -->
        @if (totalPages > 1) {
          <div class="flex justify-center items-center gap-2 mt-8">
            <button (click)="loadPage(currentPage - 1)"
                    [disabled]="currentPage === 0"
                    class="btn-secondary text-sm">
              ← Previous
            </button>
            <span class="text-sm text-gray-500">
              Page {{ currentPage + 1 }} of {{ totalPages }}
            </span>
            <button (click)="loadPage(currentPage + 1)"
                    [disabled]="currentPage >= totalPages - 1"
                    class="btn-secondary text-sm">
              Next →
            </button>
          </div>
        }
      }

      <!-- Delete confirmation modal -->
      @if (tripToDelete) {
        <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div class="bg-white rounded-xl p-6 max-w-sm w-full mx-4">
            <h3 class="font-semibold text-gray-900 mb-2">Delete trip?</h3>
            <p class="text-gray-500 text-sm mb-4">
              "{{ tripToDelete.destination }}" will be permanently deleted.
            </p>
            <div class="flex gap-3">
              <button (click)="tripToDelete = null" class="btn-secondary flex-1">Cancel</button>
              <button (click)="deleteTrip()" class="btn-danger flex-1" [disabled]="deleting">
                {{ deleting ? 'Deleting...' : 'Delete' }}
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class DashboardComponent implements OnInit {
  trips: Trip[] = [];
  loading = true;
  currentPage = 0;
  totalPages = 1;
  tripToDelete: Trip | null = null;
  deleting = false;

  constructor(
    private tripService: TripService,
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadPage(0);
  }

  loadPage(page: number): void {
    this.loading = true;
    this.tripService.getUserTrips(page).subscribe({
      next: (res) => {
        this.trips = res.data.content;
        this.totalPages = res.data.totalPages;
        this.currentPage = res.data.number;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  navigateToTrip(id: number): void {
    this.router.navigate(['/trips', id]);
  }

  confirmDelete(trip: Trip): void {
    this.tripToDelete = trip;
  }

  deleteTrip(): void {
    if (!this.tripToDelete) return;
    this.deleting = true;
    this.tripService.deleteTrip(this.tripToDelete.id).subscribe({
      next: () => {
        this.tripToDelete = null;
        this.deleting = false;
        this.loadPage(this.currentPage);
      },
      error: () => { this.deleting = false; }
    });
  }

  getStatusBadgeClass(status: string): string {
    return {
      COMPLETED: 'badge-green',
      GENERATING: 'badge-yellow',
      FAILED: 'badge-red'
    }[status] ?? 'badge-blue';
  }
}
