import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import {
  CreateEntrepriseRequest,
  Entreprise,
  TailleEntreprise,
} from '../../models/entreprise.model';
import { EntrepriseService } from '../../services/entreprise.service';

@Component({
  selector: 'app-entreprise-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './entreprise-form.component.html',
  styleUrl: './entreprise-form.component.scss',
})
export class EntrepriseFormComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly entrepriseService = inject(EntrepriseService);
  private currentEntreprise: Entreprise | null = null;

  @Input()
  set entreprise(value: Entreprise | null | undefined) {
    this.currentEntreprise = value ?? null;
    const passwordControl = this.form.controls.motDePasse;
    passwordControl.setValidators(
      value
        ? [Validators.minLength(8), Validators.maxLength(72)]
        : [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    );
    if (value) {
      this.form.reset({
        nom: value.nom,
        adresse: value.adresse ?? '',
        ville: value.ville ?? '',
        secteurActivite: value.secteurActivite ?? '',
        taille: value.taille ?? '',
        emailContact: value.emailContact,
        telephone: value.telephone ?? '',
        siteWeb: value.siteWeb ?? '',
        motDePasse: '',
      });
    } else {
      this.form.reset({
        nom: '',
        adresse: '',
        ville: '',
        secteurActivite: '',
        taille: '',
        emailContact: '',
        telephone: '',
        siteWeb: '',
        motDePasse: '',
      });
    }
    passwordControl.updateValueAndValidity();
  }

  @Output() readonly saved = new EventEmitter<Entreprise>();
  @Output() readonly cancelled = new EventEmitter<void>();

  readonly tailles: ReadonlyArray<{ value: TailleEntreprise; label: string }> = [
    { value: 'TPE', label: 'TPE' },
    { value: 'PME', label: 'PME' },
    { value: 'ETI', label: 'ETI' },
    { value: 'GRANDE_ENTREPRISE', label: 'Grande entreprise' },
  ];

  readonly form = this.formBuilder.nonNullable.group({
    nom: ['', [Validators.required, Validators.maxLength(150)]],
    adresse: ['', Validators.maxLength(255)],
    ville: ['', Validators.maxLength(100)],
    secteurActivite: ['', Validators.maxLength(150)],
    taille: this.formBuilder.nonNullable.control<TailleEntreprise | ''>(''),
    emailContact: [
      '',
      [Validators.required, Validators.email, Validators.maxLength(190)],
    ],
    telephone: ['', Validators.maxLength(30)],
    siteWeb: ['', Validators.maxLength(255)],
    motDePasse: [
      '',
      [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    ],
  });

  saving = false;
  serverError = '';
  emailAlreadyUsed = false;

  get editing(): boolean {
    return this.currentEntreprise !== null;
  }

  submit(): void {
    this.emailAlreadyUsed = false;
    this.form.controls.emailContact.updateValueAndValidity();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.serverError = '';
    const data = this.toRequest();
    const request$ = this.currentEntreprise
      ? this.entrepriseService.modifierEntreprise(
          this.currentEntreprise.id,
          this.toUpdateRequest(data),
        )
      : this.entrepriseService.creerEntreprise(data);

    request$.pipe(finalize(() => (this.saving = false))).subscribe({
      next: (entreprise) => this.saved.emit(entreprise),
      error: (error: HttpErrorResponse) => this.handleError(error),
    });
  }

  cancel(): void {
    this.cancelled.emit();
  }

  private toRequest(): CreateEntrepriseRequest {
    const values = this.form.getRawValue();
    return {
      nom: values.nom.trim(),
      adresse: values.adresse.trim(),
      ville: values.ville.trim(),
      secteurActivite: values.secteurActivite.trim(),
      ...(values.taille ? { taille: values.taille } : {}),
      emailContact: values.emailContact.trim(),
      telephone: values.telephone.trim(),
      siteWeb: values.siteWeb.trim(),
      motDePasse: values.motDePasse,
    };
  }

  private toUpdateRequest(
    data: CreateEntrepriseRequest,
  ): Omit<CreateEntrepriseRequest, 'motDePasse'> {
    const { motDePasse: _motDePasse, ...updateData } = data;
    return updateData;
  }

  private handleError(error: HttpErrorResponse): void {
    const message =
      error.error?.message ?? "L'entreprise n'a pas pu être enregistrée.";
    if (error.status === 409 && message.toLowerCase().includes('email')) {
      this.emailAlreadyUsed = true;
      this.form.controls.emailContact.setErrors({ emailAlreadyUsed: true });
      return;
    }
    this.serverError = message;
  }
}
