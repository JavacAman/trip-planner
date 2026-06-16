import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-blue-100 px-4">
      <div class="w-full max-w-md">
        <div class="card">
          <div class="text-center mb-8">
            <span class="text-5xl">✈️</span>
            <h1 class="text-2xl font-bold text-gray-900 mt-3">Welcome back</h1>
            <p class="text-gray-500 text-sm mt-1">Sign in to your Trip Planner account</p>
          </div>

          @if (errorMessage) {
            <div class="bg-red-50 border border-red-200 rounded-lg p-3 mb-4">
              <p class="text-red-700 text-sm">{{ errorMessage }}</p>
            </div>
          }

          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="space-y-4">
            <div>
              <label class="form-label">Email address</label>
              <input type="email" formControlName="email"
                     class="form-input"
                     placeholder="you@example.com"
                     autocomplete="email" />
              @if (f['email'].invalid && f['email'].touched) {
                <p class="form-error">Please enter a valid email address</p>
              }
            </div>

            <div>
              <label class="form-label">Password</label>
              <div class="relative">
                <input [type]="showPassword ? 'text' : 'password'"
                       formControlName="password"
                       class="form-input pr-10"
                       placeholder="••••••••"
                       autocomplete="current-password" />
                <button type="button"
                        (click)="showPassword = !showPassword"
                        class="absolute inset-y-0 right-3 text-gray-400 hover:text-gray-600">
                  {{ showPassword ? '🙈' : '👁️' }}
                </button>
              </div>
              @if (f['password'].invalid && f['password'].touched) {
                <p class="form-error">Password is required</p>
              }
            </div>

            <button type="submit"
                    class="btn-primary w-full py-2.5 text-base"
                    [disabled]="loading">
              @if (loading) {
                <span class="flex items-center justify-center gap-2">
                  <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.4 0 0 5.4 0 12h4z"/>
                  </svg>
                  Signing in...
                </span>
              } @else {
                Sign in
              }
            </button>
          </form>

          <p class="text-center text-sm text-gray-500 mt-6">
            Don't have an account?
            <a routerLink="/auth/register" class="text-primary-600 hover:underline font-medium">
              Create one free
            </a>
          </p>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  loginForm: FormGroup;
  loading = false;
  errorMessage = '';
  showPassword = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  get f() { return this.loginForm.controls; }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.errorMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Invalid email or password. Please try again.';
        this.loading = false;
      }
    });
  }
}
