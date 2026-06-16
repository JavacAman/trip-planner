import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col items-center justify-center" [ngClass]="containerClass">
      <div class="relative">
        <div class="w-12 h-12 rounded-full border-4 border-primary-100 border-t-primary-600 animate-spin"></div>
        <span class="absolute inset-0 flex items-center justify-center text-lg">✈️</span>
      </div>
      @if (message) {
        <p class="mt-3 text-sm text-gray-500 animate-pulse">{{ message }}</p>
      }
    </div>
  `
})
export class LoadingSpinnerComponent {
  @Input() message = '';
  @Input() containerClass = 'py-12';
}
