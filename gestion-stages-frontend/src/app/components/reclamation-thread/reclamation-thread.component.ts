import { Component, Input, Output, EventEmitter, SimpleChanges, inject, OnChanges, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ReclamationService } from '../../services/reclamation.service';
import { ReclamationDetail, MessageReclamation, StatutReclamation } from '../../models/reclamation.model';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-reclamation-thread',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="thread-container">
      <!-- En-tête de la réclamation -->
      <div class="thread-header">
        <div class="header-info">
          <h2 class="title">{{ reclamation?.objet }}</h2>
          <div class="meta-info">
            <span class="meta-item">
              <strong>Type:</strong> {{ getTypeLabel(reclamation?.typeReclamation) }}
            </span>
            <span class="meta-item">
              <strong>Statut:</strong> <span [ngClass]="'status-' + reclamation?.statut">{{ getStatutLabel(reclamation?.statut) }}</span>
            </span>
            <span class="meta-item">
              <strong>Créée:</strong> {{ reclamation?.dateCreation | date:'dd/MM/yyyy HH:mm' }}
            </span>
            <span class="meta-item" *ngIf="reclamation?.nomTraitePar">
              <strong>Traitée par:</strong> {{ reclamation?.nomTraitePar }}
            </span>
          </div>
        </div>

        <!-- Boutons d'action pour les admins -->
        <div class="admin-actions" *ngIf="canManage">
          <button
            *ngIf="reclamation?.statut === 'EN_TRAITEMENT'"
            (click)="resoudre()"
            class="btn btn-success"
            [disabled]="isLoading"
          >
            ✓ Marquer comme résolue
          </button>
          <button
            *ngIf="reclamation?.statut === 'RESOLUE'"
            (click)="cloturer()"
            class="btn btn-danger"
            [disabled]="isLoading"
          >
            🔒 Clôturer
          </button>
        </div>
      </div>

      <!-- Fil de messages -->
      <div class="messages-container">
        <div *ngIf="!messages || messages.length === 0" class="empty-state">
          Aucun message pour le moment
        </div>

        <div *ngFor="let message of messages" class="message" [ngClass]="getMessageClass(message)">
          <div class="message-header">
            <span class="author-name">{{ message.nomAuteur }}</span>
            <span class="author-role" [ngClass]="'role-' + message.roleAuteur">
              {{ message.roleAuteur }}
            </span>
            <span class="timestamp">{{ message.dateEnvoi | date:'dd/MM/yyyy HH:mm' }}</span>
          </div>
          <div class="message-content">{{ message.contenu }}</div>
          <div *ngIf="message.pieceJointe" class="message-attachment">
            <a [href]="message.pieceJointe" target="_blank">📎 Pièce jointe</a>
          </div>
        </div>
      </div>

      <!-- Zone de statut clôturé avec bouton de réouverture -->
      <div *ngIf="reclamation?.statut === 'CLOTUREE'" class="closed-banner">
        <div class="closed-text">
          ✓ Cette réclamation a été clôturée le {{ reclamation?.dateCloture | date:'dd/MM/yyyy HH:mm' }}
        </div>
        <div class="reopen-info" *ngIf="isEtudiant && canReopen">
          <p>Vous pouvez réouvrir cette réclamation si vous n'êtes pas satisfait (délai: 7 jours après clôture).</p>
          <button (click)="rouvrir()" class="btn btn-warning" [disabled]="isLoading">
            🔄 Réouvrir la réclamation
          </button>
        </div>
        <div class="reopen-expired" *ngIf="isEtudiant && !canReopen">
          <p>Délai de réouverture dépassé (7 jours). Cette réclamation ne peut plus être réouverte.</p>
        </div>
      </div>

      <!-- Zone de saisie de message -->
      <div class="message-input-area" *ngIf="reclamation?.statut !== 'CLOTUREE'">
        <div *ngIf="errorMessage" class="alert alert-error">
          {{ errorMessage }}
        </div>

        <form [formGroup]="messageForm" (ngSubmit)="sendMessage()" class="message-form">
          <textarea
            formControlName="contenu"
            class="message-textarea"
            placeholder="Écrivez votre message..."
            rows="4"
          ></textarea>

          <div class="input-footer">
            <button type="submit" class="btn btn-primary" [disabled]="messageForm.invalid || isLoading">
              {{ isLoading ? 'Envoi...' : 'Envoyer' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .thread-container {
      display: flex;
      flex-direction: column;
      height: 100%;
      background: white;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    }

    .thread-header {
      padding: 24px;
      border-bottom: 2px solid #e5e7eb;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      background: linear-gradient(135deg, #f9fafb, #f3f4f6);
    }

    .header-info {
      flex: 1;
    }

    .title {
      margin: 0 0 12px 0;
      font-size: 1.5rem;
      font-weight: 700;
      color: #1f2937;
    }

    .meta-info {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      font-size: 0.9rem;
      color: #6b7280;
    }

    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .status-OUVERTE { color: #3b82f6; font-weight: 600; }
    .status-EN_TRAITEMENT { color: #f59e0b; font-weight: 600; }
    .status-RESOLUE { color: #10b981; font-weight: 600; }
    .status-CLOTUREE { color: #6b7280; font-weight: 600; }
    .status-REOUVERTE { color: #ef4444; font-weight: 600; }

    .admin-actions {
      display: flex;
      gap: 12px;
    }

    .messages-container {
      flex: 1;
      overflow-y: auto;
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .empty-state {
      text-align: center;
      color: #9ca3af;
      padding: 40px 20px;
      font-style: italic;
    }

    .message {
      padding: 16px;
      border-radius: 8px;
      background: #f9fafb;
      border-left: 4px solid #e5e7eb;
      animation: slideIn 0.3s ease-out;
    }

    .message.own {
      margin-left: 40px;
      background: #ecf3ff;
      border-left-color: #6366f1;
    }

    .message.other {
      margin-right: 40px;
      background: #fef3c7;
      border-left-color: #f59e0b;
    }

    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateY(10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .message-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 8px;
      font-size: 0.9rem;
    }

    .author-name {
      font-weight: 700;
      color: #1f2937;
    }

    .author-role {
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
    }

    .role-ETUDIANT { background: #dbeafe; color: #1e40af; }
    .role-ENTREPRISE { background: #e0e7ff; color: #3730a3; }
    .role-CHEF_DEPT_STAGE { background: #fecaca; color: #7f1d1d; }
    .role-CHEF_DEPT_PEDAGOGIQUE { background: #d1d5db; color: #374151; }

    .timestamp {
      margin-left: auto;
      color: #9ca3af;
      font-size: 0.85rem;
    }

    .message-content {
      color: #374151;
      line-height: 1.6;
      white-space: pre-wrap;
      word-wrap: break-word;
    }

    .message-attachment {
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid #e5e7eb;
    }

    .message-attachment a {
      color: #3b82f6;
      text-decoration: none;
      font-weight: 500;
    }

    .message-attachment a:hover {
      text-decoration: underline;
    }

    .closed-banner {
      background: #ecfdf5;
      border-top: 2px solid #10b981;
      padding: 16px 24px;
      font-size: 0.95rem;
    }

    .closed-text {
      color: #047857;
      font-weight: 600;
      margin-bottom: 12px;
    }

    .reopen-info, .reopen-expired {
      margin-top: 12px;
      padding: 12px;
      background: white;
      border-radius: 8px;
      border: 1px solid #d1d5db;
    }

    .reopen-info p {
      margin: 0 0 12px 0;
      color: #374151;
      font-size: 0.9rem;
    }

    .reopen-expired {
      background: #fef2f2;
      border-color: #fecaca;
    }

    .reopen-expired p {
      margin: 0;
      color: #991b1b;
    }

    .message-input-area {
      padding: 24px;
      border-top: 1px solid #e5e7eb;
      background: #f9fafb;
    }

    .message-form {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .message-textarea {
      width: 100%;
      padding: 12px;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      font-family: inherit;
      font-size: 1rem;
      resize: vertical;
      transition: border-color 0.2s;
    }

    .message-textarea:focus {
      outline: none;
      border-color: #6366f1;
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
    }

    .input-footer {
      display: flex;
      justify-content: flex-end;
    }

    .alert {
      padding: 12px 16px;
      border-radius: 8px;
      margin-bottom: 12px;
    }

    .alert-error {
      background: #fee2e2;
      color: #991b1b;
      border: 1px solid #fecaca;
    }

    .btn {
      padding: 10px 16px;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      font-size: 0.9rem;
      transition: all 0.2s;
    }

    .btn-primary {
      background: linear-gradient(135deg, #6366f1, #4f46e5);
      color: white;
    }

    .btn-primary:hover:not([disabled]) {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
    }

    .btn-success {
      background: #10b981;
      color: white;
    }

    .btn-success:hover:not([disabled]) {
      background: #059669;
    }

    .btn-danger {
      background: #ef4444;
      color: white;
    }

    .btn-danger:hover:not([disabled]) {
      background: #dc2626;
    }

    .btn-warning {
      background: #f59e0b;
      color: white;
    }

    .btn-warning:hover:not([disabled]) {
      background: #d97706;
    }

    .btn[disabled] {
      opacity: 0.6;
      cursor: not-allowed;
    }
  `]
})
export class ReclamationThreadComponent implements OnChanges, OnDestroy {
  @Input() reclamation?: ReclamationDetail;
  @Input() canManage = false;
  @Input() isEtudiant = false;
  @Input() currentUserId?: number;
  @Output() reclamationUpdated = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private reclamationService = inject(ReclamationService);
  private destroy$ = new Subject<void>();

  messages: MessageReclamation[] = [];
  messageForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  canReopen = false;

  constructor() {
    this.messageForm = this.fb.group({
      contenu: ['', [Validators.required, Validators.minLength(1)]]
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['reclamation'] && this.reclamation) {
      this.messages = this.reclamation.messages || [];
      this.checkCanReopen();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private checkCanReopen(): void {
    if (this.reclamation?.statut === 'CLOTUREE' && this.reclamation?.dateCloture) {
      const closedDate = new Date(this.reclamation.dateCloture).getTime();
      const now = new Date().getTime();
      const sevenDaysMs = 7 * 24 * 60 * 60 * 1000;

      this.canReopen = (now - closedDate) < sevenDaysMs;
    }
  }

  sendMessage(): void {
    if (this.messageForm.invalid || !this.reclamation) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const { contenu } = this.messageForm.value;

    this.reclamationService.ajouterMessage(this.reclamation.id, contenu)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (message) => {
          this.messages.push(message);
          this.messageForm.reset();
          this.isLoading = false;
          this.reclamationUpdated.emit();
          setTimeout(() => this.scrollToBottom(), 100);
        },
        error: (error: HttpErrorResponse) => {
          this.isLoading = false;
          this.errorMessage = error.error?.message || 'Erreur lors de l\'envoi du message';
        }
      });
  }

  resoudre(): void {
    if (!this.reclamation) return;

    this.isLoading = true;
    this.reclamationService.resoudre(this.reclamation.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isLoading = false;
          this.reclamationUpdated.emit();
        },
        error: (error: HttpErrorResponse) => {
          this.isLoading = false;
          this.errorMessage = error.error?.message || 'Erreur lors de la résolution';
        }
      });
  }

  cloturer(): void {
    if (!this.reclamation) return;

    this.isLoading = true;
    this.reclamationService.cloturer(this.reclamation.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isLoading = false;
          this.reclamationUpdated.emit();
        },
        error: (error: HttpErrorResponse) => {
          this.isLoading = false;
          this.errorMessage = error.error?.message || 'Erreur lors de la clôture';
        }
      });
  }

  rouvrir(): void {
    if (!this.reclamation) return;

    this.isLoading = true;
    this.reclamationService.rouvrir(this.reclamation.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isLoading = false;
          this.reclamationUpdated.emit();
        },
        error: (error: HttpErrorResponse) => {
          this.isLoading = false;
          this.errorMessage = error.error?.message || 'Erreur lors de la réouverture';
        }
      });
  }

  getTypeLabel(type?: string): string {
    const labels: { [key: string]: string } = {
      'NOTE': 'Note',
      'DOCUMENT': 'Document',
      'EVALUATION': 'Évaluation',
      'AUTRE': 'Autre'
    };
    return labels[type || ''] || type || '';
  }

  getStatutLabel(statut?: StatutReclamation): string {
    const labels: { [key in StatutReclamation]: string } = {
      'OUVERTE': 'Ouverte',
      'EN_TRAITEMENT': 'En traitement',
      'RESOLUE': 'Résolue',
      'CLOTUREE': 'Clôturée',
      'REOUVERTE': 'Réouverte'
    };
    return labels[statut || 'OUVERTE'] || '';
  }

  getMessageClass(message: MessageReclamation): string {
    return message.roleAuteur === 'ETUDIANT' ? 'own' : 'other';
  }

  private scrollToBottom(): void {
    const element = document.querySelector('.messages-container');
    if (element) {
      element.scrollTop = element.scrollHeight;
    }
  }
}
