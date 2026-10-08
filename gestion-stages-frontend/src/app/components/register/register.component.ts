import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import {
  CreateEntrepriseRequest,
  TailleEntreprise,
} from '../../models/entreprise.model';
import { Role } from '../../models/user.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly roles: ReadonlyArray<{ value: Role; label: string }> = [
    { value: 'ETUDIANT', label: 'Étudiant' },
    { value: 'ENTREPRISE', label: 'Entreprise' },
    { value: 'CHEF_DEPT_STAGE', label: 'Chef de département stage' },
    {
      value: 'CHEF_DEPT_PEDAGOGIQUE',
      label: 'Chef de département pédagogique',
    },
  ];
  readonly tailles: ReadonlyArray<{ value: TailleEntreprise; label: string }> = [
    { value: 'TPE', label: 'TPE' },
    { value: 'PME', label: 'PME' },
    { value: 'ETI', label: 'ETI' },
    { value: 'GRANDE_ENTREPRISE', label: 'Grande entreprise' },
  ];

  readonly form = this.formBuilder.nonNullable.group({
    nom: ['', [Validators.required, Validators.maxLength(150)]],
    prenom: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    motDePasse: [
      '',
      [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    ],
    role: this.formBuilder.nonNullable.control<Role>('ETUDIANT', Validators.required),
    adresse: ['', Validators.maxLength(255)],
    ville: ['', Validators.maxLength(100)],
    secteurActivite: ['', Validators.maxLength(150)],
    taille: this.formBuilder.nonNullable.control<TailleEntreprise | ''>(''),
    telephone: ['', Validators.maxLength(30)],
    siteWeb: ['', Validators.maxLength(255)],
  });

  loading = false;
  serverError = '';
  successMessage = '';

  get isEntreprise(): boolean {
    return this.form.controls.role.value === 'ENTREPRISE';
  }

  onRoleChange(): void {
    const prenomControl = this.form.controls.prenom;
    prenomControl.setValidators(
      this.isEntreprise
        ? [Validators.maxLength(100)]
        : [Validators.required, Validators.maxLength(100)],
    );
    prenomControl.updateValueAndValidity();
  }

  submit(): void {
    this.onRoleChange();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.serverError = '';
    this.successMessage = '';
    const values = this.form.getRawValue();
    const request$ = this.isEntreprise
      ? this.authService.registerEntreprise(this.toEntrepriseRequest(values))
      : this.authService.register(
          values.nom,
          values.prenom,
          values.email,
          values.motDePasse,
          values.role,
        );

    request$
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ user, token }) => {
          if (user.role === 'ENTREPRISE' && !token) {
            this.successMessage =
              'Compte créé. Vous pourrez vous connecter après la validation du super administrateur.';
            this.form.reset({ role: 'ETUDIANT' });
            return;
          }
          void this.router.navigateByUrl(this.routeForRole(user.role));
        },
        error: (error: HttpErrorResponse) => {
          this.serverError =
            error.error?.message ??
            'Inscription impossible. Vérifiez que le serveur est démarré.';
        },
      });
  }

  private toEntrepriseRequest(
    values: ReturnType<typeof this.form.getRawValue>,
  ): CreateEntrepriseRequest {
    return {
      nom: values.nom.trim(),
      adresse: values.adresse.trim(),
      ville: values.ville.trim(),
      secteurActivite: values.secteurActivite.trim(),
      ...(values.taille ? { taille: values.taille } : {}),
      emailContact: values.email.trim(),
      telephone: values.telephone.trim(),
      siteWeb: values.siteWeb.trim(),
      motDePasse: values.motDePasse,
    };
  }

  private routeForRole(role: Role): string {
    if (role === 'ETUDIANT') {
      return '/espace-etudiant';
    }
    if (role === 'ENTREPRISE') {
      return '/espace-entreprise';
    }
    if (role === 'SUPER_ADMIN') {
      return '/espace-super-admin';
    }
    return '/espace-administration';
  }
}
