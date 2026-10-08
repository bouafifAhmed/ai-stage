import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TacheService } from '../../services/tache.service';
import { Tache } from '../../models/tache.model';

@Component({
  selector: 'app-tache-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="modal-overlay" (click)="close()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <button class="close-btn" (click)="close()">×</button>
        <h2 class="modal-title">{{ isCorrection ? 'Corriger la tâche' : 'Nouvelle entrée' }}</h2>
        
        <form [formGroup]="tacheForm" (ngSubmit)="onSubmit()" class="modern-form">
          <div class="form-group">
            <label for="date">Date</label>
            <input type="date" id="date" formControlName="date" [max]="maxDate" class="form-control" />
            <div class="error-msg" *ngIf="tacheForm.get('date')?.invalid && tacheForm.get('date')?.touched">
              La date est requise et doit être valide.
            </div>
          </div>

          <div class="form-group">
            <label for="titre">Titre</label>
            <input type="text" id="titre" formControlName="titre" placeholder="Titre de la tâche" class="form-control" />
            <div class="error-msg" *ngIf="tacheForm.get('titre')?.invalid && tacheForm.get('titre')?.touched">
              Le titre est requis (max 150 caractères).
            </div>
          </div>

          <div class="form-group">
            <label for="description">Description</label>
            <textarea id="description" formControlName="description" rows="4" placeholder="Détaillez votre travail..." class="form-control"></textarea>
            <div class="error-msg" *ngIf="tacheForm.get('description')?.invalid && tacheForm.get('description')?.touched">
              La description est requise.
            </div>
          </div>

          <div class="error-msg server-error" *ngIf="serverError">{{ serverError }}</div>

          <div class="form-actions">
            <button type="button" class="btn-cancel" (click)="close()">Annuler</button>
            <button type="submit" class="btn-submit" [disabled]="tacheForm.invalid || isSubmitting">
              {{ isSubmitting ? 'Enregistrement...' : 'Enregistrer' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed; top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0,0,0,0.5); backdrop-filter: blur(4px);
      display: flex; align-items: center; justify-content: center;
      z-index: 1000;
    }
    .modal-content {
      background: #ffffff;
      border-radius: 20px;
      padding: 32px;
      width: 100%; max-width: 500px;
      position: relative;
      box-shadow: 0 25px 50px -12px rgba(0,0,0,0.25);
      animation: slideUp 0.3s ease-out;
    }
    @keyframes slideUp {
      from { transform: translateY(20px); opacity: 0; }
      to { transform: translateY(0); opacity: 1; }
    }
    .close-btn {
      position: absolute; top: 16px; right: 20px;
      background: none; border: none; font-size: 28px;
      cursor: pointer; color: #6b7280;
    }
    .modal-title {
      font-family: 'Inter', sans-serif;
      margin-top: 0; margin-bottom: 24px;
      color: #111827; font-weight: 700;
    }
    .modern-form .form-group {
      margin-bottom: 20px; display: flex; flex-direction: column;
    }
    .modern-form label {
      font-family: 'Inter', sans-serif;
      font-size: 0.875rem; font-weight: 600; color: #374151;
      margin-bottom: 6px;
    }
    .form-control {
      padding: 10px 14px;
      border: 1px solid #d1d5db; border-radius: 8px;
      font-family: 'Inter', sans-serif; font-size: 1rem;
      transition: all 0.2s; background: #f9fafb;
    }
    .form-control:focus {
      outline: none; border-color: #3b82f6;
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1); background: #fff;
    }
    .error-msg {
      color: #ef4444; font-size: 0.8rem; margin-top: 4px;
      font-family: 'Inter', sans-serif;
    }
    .server-error {
      margin-bottom: 20px; font-weight: 600; text-align: center;
    }
    .form-actions {
      display: flex; justify-content: flex-end; gap: 12px; margin-top: 30px;
    }
    .btn-cancel {
      padding: 10px 20px; border-radius: 8px; border: 1px solid #d1d5db;
      background: #fff; color: #374151; font-weight: 600; cursor: pointer;
      transition: background 0.2s;
    }
    .btn-cancel:hover { background: #f3f4f6; }
    .btn-submit {
      padding: 10px 20px; border-radius: 8px; border: none;
      background: linear-gradient(135deg, #3b82f6, #2563eb);
      color: #fff; font-weight: 600; cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
    }
    .btn-submit:hover:not([disabled]) {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
    }
    .btn-submit[disabled] {
      opacity: 0.6; cursor: not-allowed;
    }
  `]
})
export class TacheFormComponent implements OnInit {
  @Input() stageId!: number;
  @Input() tache?: Tache;
  @Output() formClosed = new EventEmitter<void>();
  @Output() taskSaved = new EventEmitter<void>();

  tacheForm!: FormGroup;
  isCorrection = false;
  isSubmitting = false;
  serverError = '';
  maxDate = new Date().toISOString().split('T')[0];

  constructor(
    private fb: FormBuilder,
    private tacheService: TacheService
  ) {}

  ngOnInit(): void {
    this.isCorrection = !!this.tache;
    this.tacheForm = this.fb.group({
      date: [this.tache?.date || '', Validators.required],
      titre: [this.tache?.titre || '', [Validators.required, Validators.maxLength(150)]],
      description: [this.tache?.description || '', Validators.required]
    });
  }

  close() {
    this.formClosed.emit();
  }

  onSubmit() {
    if (this.tacheForm.invalid) return;
    
    this.isSubmitting = true;
    this.serverError = '';
    
    const formData = this.tacheForm.value;

    const request = this.isCorrection 
      ? this.tacheService.modifierTache(this.stageId, this.tache!.id, formData)
      : this.tacheService.creerTache(this.stageId, formData);

    request.subscribe({
      next: () => {
        this.isSubmitting = false;
        this.taskSaved.emit();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.serverError = err.error?.message || 'Une erreur est survenue';
      }
    });
  }
}
