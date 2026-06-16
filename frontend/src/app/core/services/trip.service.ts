import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ActivityRequest,
  ApiResponse,
  ModifyDayRequest,
  PageResponse,
  Trip,
  TripRequest
} from '../models/trip.model';

// SOLID-SRP: Handles only trip API communication
// Pattern-Singleton: Provided at root, one instance for the entire app
@Injectable({ providedIn: 'root' })
export class TripService {
  private readonly apiUrl = `${environment.apiUrl}/trips`;

  constructor(private http: HttpClient) {}

  createTrip(request: TripRequest): Observable<ApiResponse<Trip>> {
    return this.http.post<ApiResponse<Trip>>(this.apiUrl, request);
  }

  getUserTrips(page = 0, size = 10): Observable<ApiResponse<PageResponse<Trip>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');
    return this.http.get<ApiResponse<PageResponse<Trip>>>(this.apiUrl, { params });
  }

  getTripById(id: number): Observable<ApiResponse<Trip>> {
    return this.http.get<ApiResponse<Trip>>(`${this.apiUrl}/${id}`);
  }

  regenerateDay(tripId: number, dayNumber: number, request: ModifyDayRequest): Observable<ApiResponse<Trip>> {
    return this.http.post<ApiResponse<Trip>>(
      `${this.apiUrl}/${tripId}/days/${dayNumber}/regenerate`,
      request
    );
  }

  addActivity(tripId: number, request: ActivityRequest): Observable<ApiResponse<Trip>> {
    return this.http.post<ApiResponse<Trip>>(`${this.apiUrl}/${tripId}/activities`, request);
  }

  removeActivity(tripId: number, activityId: number): Observable<ApiResponse<Trip>> {
    return this.http.delete<ApiResponse<Trip>>(`${this.apiUrl}/${tripId}/activities/${activityId}`);
  }

  deleteTrip(tripId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${tripId}`);
  }
}
