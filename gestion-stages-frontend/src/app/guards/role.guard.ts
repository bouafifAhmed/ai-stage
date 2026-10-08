import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Role } from '../models/user.model';
import { AuthService } from '../services/auth.service';

export const roleGuard = (rolesAutorises: readonly Role[]): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const user = authService.currentUserSnapshot();

    if (!user) {
      return router.createUrlTree(['/login']);
    }

    return rolesAutorises.includes(user.role)
      ? true
      : router.createUrlTree(['/interdit']);
  };
};
