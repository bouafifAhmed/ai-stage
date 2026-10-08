import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { Role } from '../../models/user.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    motDePasse: ['', Validators.required],
  });

  loading = false;
  serverError = '';

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.serverError = '';
    const { email, motDePasse } = this.form.getRawValue();

    this.authService
      .login(email, motDePasse)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ user }) => {
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
          const destination =
            returnUrl?.startsWith('/') && !returnUrl.startsWith('//')
              ? returnUrl
              : this.routeForRole(user.role);
          void this.router.navigateByUrl(destination);
        },
        error: (error: HttpErrorResponse) => {
          this.serverError =
            error.error?.message ??
            'Connexion impossible. Vérifiez que le serveur est démarré.';
        },
      });
  }

  private routeForRole(role: Role): string {
    switch (role) {
      case 'ETUDIANT':
        return '/espace-etudiant';
      case 'ENTREPRISE':
        return '/espace-entreprise';
      case 'CHEF_DEPT_STAGE':
      case 'CHEF_DEPT_PEDAGOGIQUE':
        return '/espace-administration';
      case 'SUPER_ADMIN':
        return '/espace-super-admin';
    }
  }
}
