import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-progression-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="progression-container">
      <div class="progression-header">
        <span class="progression-title">Progression du Journal</span>
        <span class="progression-text" [ngStyle]="{'color': getColor()}">
          {{ pourcentage | number:'1.0-0' }}%
        </span>
      </div>
      <div class="progress-track">
        <div class="progress-fill" 
             [ngStyle]="{
               'width': pourcentage + '%',
               'background': getGradient()
             }">
        </div>
      </div>
    </div>
  `,
  styles: [`
    .progression-container {
      background: rgba(255, 255, 255, 0.05);
      backdrop-filter: blur(10px);
      border-radius: 16px;
      padding: 20px;
      margin-bottom: 30px;
      box-shadow: 0 8px 32px rgba(0,0,0,0.1);
      border: 1px solid rgba(255,255,255,0.1);
      transition: transform 0.3s ease;
    }
    .progression-container:hover {
      transform: translateY(-2px);
    }
    .progression-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      font-family: 'Inter', sans-serif;
    }
    .progression-title {
      font-weight: 600;
      color: #333;
      font-size: 1.1rem;
    }
    .progression-text {
      font-weight: 700;
      font-size: 1.25rem;
    }
    .progress-track {
      width: 100%;
      height: 12px;
      background: #e0e0e0;
      border-radius: 10px;
      overflow: hidden;
    }
    .progress-fill {
      height: 100%;
      border-radius: 10px;
      transition: width 1s cubic-bezier(0.4, 0, 0.2, 1);
    }
  `]
})
export class ProgressionBarComponent {
  @Input() pourcentage: number = 0;

  getColor(): string {
    if (this.pourcentage < 30) return '#ef4444'; // Red
    if (this.pourcentage <= 70) return '#f59e0b'; // Orange
    return '#10b981'; // Green
  }

  getGradient(): string {
    if (this.pourcentage < 30) return 'linear-gradient(90deg, #f87171, #ef4444)';
    if (this.pourcentage <= 70) return 'linear-gradient(90deg, #fbbf24, #f59e0b)';
    return 'linear-gradient(90deg, #34d399, #10b981)';
  }
}
