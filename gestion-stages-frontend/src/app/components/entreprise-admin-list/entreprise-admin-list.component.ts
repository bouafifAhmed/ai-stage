import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { EntrepriseFormComponent } from '../entreprise-form/entreprise-form.component';
import {
  Entreprise,
  StatutValidation,
} from '../../models/entreprise.model';
import { EntrepriseService } from '../../services/entreprise.service';

@Component({
  selector: 'app-entreprise-admin-list',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink, EntrepriseFormComponent],
  templateUrl: './entreprise-admin-list.component.html',
  styleUrl: './entreprise-admin-list.component.scss',
})
export class EntrepriseAdminListComponent implements OnInit {
  private readonly entrepriseService = inject(EntrepriseService);

  entreprises: Entreprise[] = [];
  statutFilter: StatutValidation | '' = '';
  page = 0;
  totalPages = 0;
  totalElements = 0;
  loading = false;
  errorMessage = '';
  modalOpen = false;
  selectedEntreprise: Entreprise | null = null;
  pendingDeactivationId: number | null = null;
  processingValidationId: number | null = null;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.entrepriseService
      .listerEntreprises(this.statutFilter || undefined, this.page)
      .subscribe({
        next: (response) => {
          this.entreprises = response.content;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          this.loading = false;
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage =
            error.error?.message ?? 'Impossible de charger les entreprises.';
          this.loading = false;
        },
      });
  }

  applyFilter(): void {
    this.page = 0;
    this.load();
  }

  previousPage(): void {
    if (this.page > 0) {
      this.page--;
      this.load();
    }
  }

  nextPage(): void {
    if (this.page + 1 < this.totalPages) {
      this.page++;
      this.load();
    }
  }

  openCreate(): void {
    this.selectedEntreprise = null;
    this.modalOpen = true;
  }

  openEdit(entreprise: Entreprise): void {
    this.selectedEntreprise = entreprise;
    this.modalOpen = true;
  }

  closeModal(): void {
    this.modalOpen = false;
    this.selectedEntreprise = null;
  }

  onSaved(): void {
    this.closeModal();
    this.load();
  }

  requestDeactivation(id: number): void {
    this.pendingDeactivationId = id;
  }

  cancelDeactivation(): void {
    this.pendingDeactivationId = null;
  }

  confirmDeactivation(id: number): void {
    this.entrepriseService.desactiverEntreprise(id).subscribe({
      next: () => {
        this.pendingDeactivationId = null;
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage =
          error.error?.message ?? "L'entreprise n'a pas pu être désactivée.";
        this.pendingDeactivationId = null;
      },
    });
  }

  validateEntreprise(id: number): void {
    this.processingValidationId = id;
    this.errorMessage = '';
    this.entrepriseService.validerEntreprise(id).subscribe({
      next: () => {
        this.processingValidationId = null;
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage =
          error.error?.message ?? "L'entreprise n'a pas pu être validée.";
        this.processingValidationId = null;
      },
    });
  }

  statusLabel(statut: StatutValidation): string {
    switch (statut) {
      case 'EN_ATTENTE':
        return 'En attente';
      case 'VALIDEE':
        return 'Validée';
      case 'REJETEE':
        return 'Rejetée';
    }
  }
}
