import { isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  const isPublicAuthRequest =
    request.url.includes('/auth/login') ||
    request.url.includes('/auth/register');

  const token = authService.getToken();
  const authedRequest =
    !isPublicAuthRequest && token
      ? request.clone({
          setHeaders: { Authorization: `Bearer ${token}` },
        })
      : request;

  return next(authedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      // Un 403 (accès refusé) ne doit jamais déconnecter.
      // Un 401 pendant le SSR (pas de token) non plus.
      if (
        isBrowser &&
        error.status === 401 &&
        !isPublicAuthRequest &&
        authService.getToken()
      ) {
        authService.logout();
      }
      return throwError(() => error);
    }),
  );
};
