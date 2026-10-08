import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { EtudiantFormComponent } from '../etudiant-form/etudiant-form.component';
import { EtudiantAdmin } from '../../models/etudiant-admin.model';
import { AdminEtudiantService } from '../../services/admin-etudiant.service';

@Component({
  selector: 'app-etudiant-admin-list',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink, EtudiantFormComponent],
  templateUrl: './etudiant-admin-list.component.html',
  styleUrl: './etudiant-admin-list.component.scss',
})
export class EtudiantAdminListComponent implements OnInit {
  private readonly adminEtudiantService = inject(AdminEtudiantService);

  etudiants: EtudiantAdmin[] = [];
  actifFilter: 'all' | 'active' | 'inactive' = 'all';
  filiereFilter = '';
  page = 0;
  totalPages = 0;
  totalElements = 0;
  loading = false;
  errorMessage = '';
  modalOpen = false;
  selectedEtudiant: EtudiantAdmin | null = null;
  pendingDeactivationId: number | null = null;
  processingActivationId: number | null = null;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.adminEtudiantService
      .listerEtudiants({
        actif:
          this.actifFilter === 'all'
            ? undefined
            : this.actifFilter === 'active',
        filiere: this.filiereFilter.trim() || undefined,
        page: this.page,
      })
      .subscribe({
        next: (response) => {
          this.etudiants = response.content;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          this.loading = false;
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage =
            error.error?.message ?? 'Impossible de charger les étudiants.';
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
    this.selectedEtudiant = null;
    this.modalOpen = true;
  }

  openEdit(etudiant: EtudiantAdmin): void {
    this.selectedEtudiant = etudiant;
    this.modalOpen = true;
  }

  closeModal(): void {
    this.modalOpen = false;
    this.selectedEtudiant = null;
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
    this.adminEtudiantService.desactiverEtudiant(id).subscribe({
      next: () => {
        this.pendingDeactivationId = null;
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage =
          error.error?.message ?? "L'étudiant n'a pas pu être désactivé.";
        this.pendingDeactivationId = null;
      },
    });
  }

  activateEtudiant(id: number): void {
    this.processingActivationId = id;
    this.errorMessage = '';
    this.adminEtudiantService.activerEtudiant(id).subscribe({
      next: () => {
        this.processingActivationId = null;
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage =
          error.error?.message ?? "L'étudiant n'a pas pu être activé.";
        this.processingActivationId = null;
      },
    });
  }
}
