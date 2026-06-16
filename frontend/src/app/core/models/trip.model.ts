export type BudgetType = 'LOW' | 'MEDIUM' | 'HIGH';
export type TripStatus = 'GENERATING' | 'COMPLETED' | 'FAILED';

export interface TripRequest {
  destination: string;
  numberOfDays: number;
  budgetType: BudgetType;
  interests: string[];
}

export interface Activity {
  id: number;
  title: string;
  description: string;
  timeOfDay: string;
  location: string;
  estimatedDuration: string;
  estimatedCost: string;
  orderIndex: number;
}

export interface DayItinerary {
  id: number;
  dayNumber: number;
  theme: string;
  activities: Activity[];
}

export interface Trip {
  id: number;
  destination: string;
  numberOfDays: number;
  budgetType: BudgetType;
  interests: string[];
  itinerary: DayItinerary[];
  estimatedFlightCost: number;
  estimatedAccommodationCost: number;
  estimatedFoodCost: number;
  estimatedActivitiesCost: number;
  totalEstimatedBudget: number;
  status: TripStatus;
  hotelSuggestions: string;
  createdAt: string;
  updatedAt: string;
}

export interface ActivityRequest {
  title: string;
  description?: string;
  timeOfDay?: string;
  location?: string;
  estimatedDuration?: string;
  estimatedCost?: string;
  dayNumber: number;
}

export interface ModifyDayRequest {
  instruction: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
