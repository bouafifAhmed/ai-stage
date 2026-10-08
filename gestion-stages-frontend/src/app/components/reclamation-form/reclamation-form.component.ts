import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ReclamationService } from '../../services/reclamation.service';
import { TypeReclamation } from '../../models/reclamation.model';

@Component({
  selector: 'app-reclamation-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="modal-overlay" (click)="closeForm()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h2>Créer une réclamation</h2>
          <button class="close-btn" (click)="closeForm()">×</button>
        </div>

        <form [formGroup]="form" (ngSubmit)="submitForm()" class="form-container">
          <div *ngIf="errorMessage" class="alert alert-error">
            {{ errorMessage }}
          </div>

          <div class="form-group">
            <label for="type">Type de réclamation</label>
            <select id="type" formControlName="typeReclamation" class="form-control">
              <option value="">Sélectionnez un type</option>
              <option value="NOTE">Note</option>
              <option value="DOCUMENT">Document</option>
              <option value="EVALUATION">Évaluation</option>
              <option value="AUTRE">Autre</option>
            </select>
            <div *ngIf="form.get('typeReclamation')?.invalid && form.get('typeReclamation')?.touched" class="error-text">
              Le type est obligatoire
            </div>
          </div>

          <div class="form-group">
            <label for="objet">Objet</label>
            <input
              id="objet"
              type="text"
              formControlName="objet"
              class="form-control"
              placeholder="Résumé du problème (max 150 caractères)"
              maxlength="150"
            />
            <div *ngIf="form.get('objet')?.invalid && form.get('objet')?.touched" class="error-text">
              L'objet est obligatoire
            </div>
          </div>

          <div class="form-group">
            <label for="message">Message initial</label>
            <textarea
              id="message"
              formControlName="messageInitial"
              class="form-control textarea"
              placeholder="Décrivez votre réclamation en détail..."
              rows="6"
            ></textarea>
            <div *ngIf="form.get('messageInitial')?.invalid && form.get('messageInitial')?.touched" class="error-text">
              Le message initial est obligatoire
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" (click)="closeForm()">
              Annuler
            </button>
            <button type="submit" class="btn btn-primary" [disabled]="form.invalid || isSubmitting">
              {{ isSubmitting ? 'Envoi...' : 'Créer la réclamation' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    .modal-content {
      background: white;
      border-radius: 16px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
      width: 90%;
      max-width: 500px;
      max-height: 90vh;
      overflow-y: auto;
    }
    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24px;
      border-bottom: 1px solid #e5e7eb;
    }
    .modal-header h2 {
      margin: 0;
      font-size: 1.5rem;
      font-weight: 700;
      color: #1f2937;
    }
    .close-btn {
      background: none;
      border: none;
      font-size: 2rem;
      cursor: pointer;
      color: #6b7280;
      padding: 0;
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .close-btn:hover {
      color: #1f2937;
    }
    .form-container {
      padding: 24px;
    }
    .form-group {
      margin-bottom: 20px;
    }
    .form-group label {
      display: block;
      font-weight: 600;
      margin-bottom: 8px;
      color: #374151;
      font-size: 0.95rem;
    }
    .form-control {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      font-size: 1rem;
      font-family: inherit;
      transition: border-color 0.2s;
    }
    .form-control:focus {
      outline: none;
      border-color: #6366f1;
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
    }
    .textarea {
      resize: vertical;
      font-family: 'Monaco', 'Menlo', monospace;
    }
    .error-text {
      color: #dc2626;
      font-size: 0.85rem;
      margin-top: 4px;
    }
    .alert {
      padding: 12px 16px;
      border-radius: 8px;
      margin-bottom: 20px;
    }
    .alert-error {
      background: #fee2e2;
      color: #991b1b;
      border: 1px solid #fecaca;
    }
    .form-actions {
      display: flex;
      gap: 12px;
      justify-content: flex-end;
      margin-top: 24px;
      padding-top: 24px;
      border-top: 1px solid #e5e7eb;
    }
    .btn {
      padding: 10px 20px;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      font-size: 0.95rem;
      transition: all 0.2s;
    }
    .btn-primary {
      background: linear-gradient(135deg, #6366f1, #4f46e5);
      color: white;
      box-shadow: 0 4px 6px -1px rgba(99, 102, 241, 0.2);
    }
    .btn-primary:hover:not([disabled]) {
      transform: translateY(-2px);
      box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.3);
    }
    .btn-primary[disabled] {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .btn-secondary {
      background: #f3f4f6;
      color: #374151;
      border: 1px solid #d1d5db;
    }
    .btn-secondary:hover {
      background: #e5e7eb;
    }
  `]
})
export class ReclamationFormComponent {
  @Input() stageId!: number;
  @Output() formClosed = new EventEmitter<void>();
  @Output() reclamationCreated = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private reclamationService = inject(ReclamationService);

  form: FormGroup;
  isSubmitting = false;
  errorMessage = '';

  constructor() {
    this.form = this.fb.group({
      typeReclamation: ['', Validators.required],
      objet: ['', [Validators.required, Validators.maxLength(150)]],
      messageInitial: ['', Validators.required]
    });
  }

  submitForm(): void {
    if (this.form.invalid) {
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const { typeReclamation, objet, messageInitial } = this.form.value;

    this.reclamationService.creerReclamation(
      this.stageId,
      typeReclamation,
      objet,
      messageInitial
    ).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.reclamationCreated.emit();
        this.closeForm();
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting = false;
        this.errorMessage = error.error?.message || 'Erreur lors de la création de la réclamation';
      }
    });
  }

  closeForm(): void {
    this.formClosed.emit();
  }
}
