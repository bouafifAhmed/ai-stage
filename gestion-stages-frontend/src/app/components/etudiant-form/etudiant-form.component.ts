import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import {
  CreateEtudiantRequest,
  EtudiantAdmin,
} from '../../models/etudiant-admin.model';
import { AdminEtudiantService } from '../../services/admin-etudiant.service';

@Component({
  selector: 'app-etudiant-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './etudiant-form.component.html',
  styleUrl: './etudiant-form.component.scss',
})
export class EtudiantFormComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly adminEtudiantService = inject(AdminEtudiantService);
  private currentEtudiant: EtudiantAdmin | null = null;

  @Input()
  set etudiant(value: EtudiantAdmin | null | undefined) {
    this.currentEtudiant = value ?? null;
    const passwordControl = this.form.controls.motDePasse;
    passwordControl.setValidators(
      value
        ? [Validators.minLength(8), Validators.maxLength(72)]
        : [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    );

    if (value) {
      this.form.reset({
        nom: value.nom,
        prenom: value.prenom,
        email: value.email,
        telephone: value.telephone ?? '',
        filiere: value.filiere ?? '',
        niveauEtudes: value.niveauEtudes ?? '',
        competences: value.competences.join(', '),
        motDePasse: '',
      });
    } else {
      this.form.reset({
        nom: '',
        prenom: '',
        email: '',
        telephone: '',
        filiere: '',
        niveauEtudes: '',
        competences: '',
        motDePasse: '',
      });
    }
    passwordControl.updateValueAndValidity();
  }

  @Output() readonly saved = new EventEmitter<EtudiantAdmin>();
  @Output() readonly cancelled = new EventEmitter<void>();

  readonly form = this.formBuilder.nonNullable.group({
    nom: ['', [Validators.required, Validators.maxLength(100)]],
    prenom: ['', [Validators.required, Validators.maxLength(100)]],
    email: [
      '',
      [Validators.required, Validators.email, Validators.maxLength(190)],
    ],
    telephone: ['', Validators.maxLength(30)],
    filiere: ['', Validators.maxLength(150)],
    niveauEtudes: ['', Validators.maxLength(100)],
    competences: [''],
    motDePasse: [
      '',
      [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    ],
  });

  saving = false;
  serverError = '';
  emailAlreadyUsed = false;

  get editing(): boolean {
    return this.currentEtudiant !== null;
  }

  submit(): void {
    this.emailAlreadyUsed = false;
    this.form.controls.email.updateValueAndValidity();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.serverError = '';
    const data = this.toRequest();
    const request$ = this.currentEtudiant
      ? this.adminEtudiantService.modifierEtudiant(
          this.currentEtudiant.id,
          this.toUpdateRequest(data),
        )
      : this.adminEtudiantService.creerEtudiant(data);

    request$.pipe(finalize(() => (this.saving = false))).subscribe({
      next: (etudiant) => this.saved.emit(etudiant),
      error: (error: HttpErrorResponse) => this.handleError(error),
    });
  }

  cancel(): void {
    this.cancelled.emit();
  }

  private toRequest(): CreateEtudiantRequest {
    const values = this.form.getRawValue();
    return {
      nom: values.nom.trim(),
      prenom: values.prenom.trim(),
      email: values.email.trim(),
      motDePasse: values.motDePasse,
      telephone: values.telephone.trim(),
      filiere: values.filiere.trim(),
      niveauEtudes: values.niveauEtudes.trim(),
      competences: this.parseCompetences(values.competences),
    };
  }

  private toUpdateRequest(
    data: CreateEtudiantRequest,
  ): Omit<CreateEtudiantRequest, 'motDePasse'> {
    const { motDePasse: _motDePasse, ...updateData } = data;
    return updateData;
  }

  private parseCompetences(value: string): string[] {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
  }

  private handleError(error: HttpErrorResponse): void {
    const message =
      error.error?.message ?? "L'étudiant n'a pas pu être enregistré.";
    if (error.status === 409 && message.toLowerCase().includes('email')) {
      this.emailAlreadyUsed = true;
      this.form.controls.email.setErrors({ emailAlreadyUsed: true });
      return;
    }
    this.serverError = message;
  }
}
