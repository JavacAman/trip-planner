import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <a routerLink="/" class="flex items-center gap-2">
            <span class="text-2xl">✈️</span>
            <span class="font-bold text-xl text-primary-700">TripPlanner AI</span>
          </a>

          @if (authService.isAuthenticated()) {
            <div class="flex items-center gap-4">
              <a routerLink="/dashboard"
                 routerLinkActive="text-primary-600 font-semibold"
                 class="text-sm text-gray-600 hover:text-primary-600 transition-colors">
                Dashboard
              </a>
              <a routerLink="/trips/new"
                 class="btn-primary text-sm">
                + New Trip
              </a>
              <div class="flex items-center gap-3">
                <span class="text-sm text-gray-600">
                  Hello, {{ authService.currentUser?.fullName?.split(' ')?.[0] }}
                </span>
                <button (click)="authService.logout()"
                        class="text-sm text-gray-500 hover:text-red-600 transition-colors">
                  Logout
                </button>
              </div>
            </div>
          } @else {
            <div class="flex items-center gap-3">
              <a routerLink="/auth/login" class="btn-secondary text-sm">Login</a>
              <a routerLink="/auth/register" class="btn-primary text-sm">Get Started</a>
            </div>
          }
        </div>
      </div>
    </nav>
  `
})
export class NavbarComponent {
  constructor(public authService: AuthService) {}
}
