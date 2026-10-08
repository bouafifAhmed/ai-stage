import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AfterViewInit,
  Component,
  ElementRef,
  inject,
  OnInit,
  ViewChild,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { StageCloture } from '../../models/stage-suivi.model';
import { StageSuiviService } from '../../services/stage-suivi.service';

@Component({
  selector: 'app-cloture-stage',
  standalone: true,
  imports: [DatePipe, RouterLink],
  template: `
    <main class="portal-page"><section class="portal-shell">
      <header class="portal-nav">
        <a class="brand" [routerLink]="homeLink">Gestion des stages</a>
        <nav>
          <a [routerLink]="tachesLink">Tâches</a>
          <a [routerLink]="calendrierLink">Calendrier</a>
        </nav>
      </header>

      <div class="page-header">
        <div>
          <p class="eyebrow">Fin de stage</p>
          <h1>Signature électronique</h1>
          @if (cloture) {
            <p>{{ cloture.offreTitre }} · {{ cloture.entrepriseNom }}</p>
          }
        </div>
        <button type="button" class="button primary" (click)="downloadPdf()" [disabled]="downloadingPdf">
          {{ downloadingPdf ? 'Génération…' : 'Télécharger le rapport PDF' }}
        </button>
      </div>

      @if (errorMessage) { <div class="alert" role="alert">{{ errorMessage }}</div> }
      @if (successMessage) { <div class="success" role="status">{{ successMessage }}</div> }
      @if (loading) { <div class="card empty">Chargement…</div> }
      @else if (cloture) {
        <div class="status-grid">
          <article class="card">
            <h2>État du stage</h2>
            <p><strong>Statut :</strong> {{ stageLabel(cloture.statutStage) }}</p>
            <p><strong>Étudiant :</strong> {{ cloture.nomEtudiant }}</p>
            <p><strong>Encadrant :</strong> {{ cloture.nomEncadrant || 'Non assigné' }}</p>
            @if (cloture.dateCloture) {
              <p><strong>Clôturé le :</strong> {{ cloture.dateCloture | date: 'dd/MM/yyyy à HH:mm' }}</p>
            }
          </article>

          <article class="card">
            <h2>Signature étudiant</h2>
            @if (cloture.signatureEtudiantPresente) {
              <p class="signed">Signée le {{ cloture.dateSignatureEtudiant | date: 'dd/MM/yyyy à HH:mm' }}</p>
            } @else {
              <p class="pending">En attente</p>
            }
          </article>

          <article class="card">
            <h2>Signature encadrant</h2>
            @if (cloture.signatureEncadrantPresente) {
              <p class="signed">Signée le {{ cloture.dateSignatureEncadrant | date: 'dd/MM/yyyy à HH:mm' }}</p>
            } @else {
              <p class="pending">En attente</p>
            }
          </article>
        </div>

        @if (showSignaturePad) {
          <section class="card signature-panel">
            <h2>Votre signature</h2>
            <p>Dessinez votre signature dans la zone ci-dessous, puis validez.</p>
            <canvas #signatureCanvas width="480" height="150" aria-label="Zone de signature"></canvas>
            <div class="actions">
              <button type="button" class="button" (click)="clearSignature()">Effacer</button>
              <button type="button" class="button primary" (click)="submitSignature()" [disabled]="signing">
                {{ signing ? 'Enregistrement…' : 'Enregistrer ma signature' }}
              </button>
            </div>
          </section>
        } @else if (cloture.statutStage === 'CLOTURE') {
          <div class="card success">Le stage est clôturé. Le rapport PDF inclut les deux signatures.</div>
        }
      }
    </section></main>
  `,
  styles: `
    .status-grid { display: grid; gap: 1rem; grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr)); margin-bottom: 1rem; }
    .signed { color: #166534; font-weight: 700; }
    .pending { color: #92400e; font-weight: 700; }
    .signature-panel canvas {
      display: block;
      width: 100%;
      max-width: 30rem;
      border: 2px dashed #94a3b8;
      border-radius: .5rem;
      background: #fff;
      touch-action: none;
      cursor: crosshair;
    }
    .actions { display: flex; gap: .75rem; margin-top: 1rem; flex-wrap: wrap; }
  `,
})
export class ClotureStageComponent implements OnInit, AfterViewInit {
  private readonly service = inject(StageSuiviService);
  private readonly route = inject(ActivatedRoute);

  @ViewChild('signatureCanvas') signatureCanvas?: ElementRef<HTMLCanvasElement>;

  readonly stageId = Number(this.route.snapshot.paramMap.get('stageId'));
  readonly role = this.route.snapshot.data['role'] as 'ETUDIANT' | 'ENTREPRISE';
  readonly homeLink = this.role === 'ETUDIANT' ? '/espace-etudiant' : '/espace-entreprise';
  readonly tachesLink = this.role === 'ETUDIANT'
    ? ['/espace-etudiant/stages', this.stageId, 'taches-assignees']
    : ['/espace-entreprise/stages', this.stageId, 'taches-assignees', 'validation'];
  readonly calendrierLink = this.role === 'ETUDIANT'
    ? ['/espace-etudiant/stages', this.stageId, 'calendrier']
    : ['/espace-entreprise/stages', this.stageId, 'calendrier'];

  cloture: StageCloture | null = null;
  loading = true;
  signing = false;
  downloadingPdf = false;
  errorMessage = '';
  successMessage = '';

  private drawing = false;
  private ctx: CanvasRenderingContext2D | null = null;

  ngOnInit(): void {
    this.load();
  }

  ngAfterViewInit(): void {
    this.initCanvas();
  }

  get showSignaturePad(): boolean {
    if (!this.cloture) {
      return false;
    }
    return this.role === 'ETUDIANT'
      ? this.cloture.peutSignerEtudiant
      : this.cloture.peutSignerEncadrant;
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.service.etatCloture(this.stageId).subscribe({
      next: (cloture) => {
        this.cloture = cloture;
        this.loading = false;
        setTimeout(() => this.initCanvas());
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de charger la clôture du stage.';
        this.loading = false;
      },
    });
  }

  downloadPdf(): void {
    this.downloadingPdf = true;
    this.errorMessage = '';
    this.service.telechargerRapport(this.stageId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `rapport-stage-${this.stageId}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
        this.downloadingPdf = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Le rapport PDF n\'a pas pu être généré.';
        this.downloadingPdf = false;
      },
    });
  }

  clearSignature(): void {
    const canvas = this.signatureCanvas?.nativeElement;
    if (!canvas || !this.ctx) {
      return;
    }
    this.ctx.clearRect(0, 0, canvas.width, canvas.height);
  }

  submitSignature(): void {
    const canvas = this.signatureCanvas?.nativeElement;
    if (!canvas) {
      return;
    }
    const blank = document.createElement('canvas');
    blank.width = canvas.width;
    blank.height = canvas.height;
    if (canvas.toDataURL('image/png') === blank.toDataURL('image/png')) {
      this.errorMessage = 'Veuillez dessiner votre signature avant de valider.';
      return;
    }

    this.signing = true;
    this.errorMessage = '';
    this.successMessage = '';
    const request = { signatureBase64: canvas.toDataURL('image/png') };
    const call = this.role === 'ETUDIANT'
      ? this.service.signerEtudiant(this.stageId, request)
      : this.service.signerEncadrant(this.stageId, request);

    call.subscribe({
      next: (cloture) => {
        this.cloture = cloture;
        this.successMessage = 'Signature enregistrée avec succès.';
        this.signing = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'La signature n\'a pas pu être enregistrée.';
        this.signing = false;
      },
    });
  }

  stageLabel(status: StageCloture['statutStage']): string {
    return status === 'CLOTURE' ? 'Clôturé' : 'Actif';
  }

  private initCanvas(): void {
    const canvas = this.signatureCanvas?.nativeElement;
    if (!canvas || !this.showSignaturePad) {
      return;
    }
    this.ctx = canvas.getContext('2d');
    if (!this.ctx) {
      return;
    }
    this.ctx.strokeStyle = '#0f172a';
    this.ctx.lineWidth = 2;
    this.ctx.lineCap = 'round';

    const start = (event: PointerEvent) => {
      this.drawing = true;
      this.ctx?.beginPath();
      this.ctx?.moveTo(this.pointerX(canvas, event), this.pointerY(canvas, event));
    };
    const draw = (event: PointerEvent) => {
      if (!this.drawing || !this.ctx) {
        return;
      }
      this.ctx.lineTo(this.pointerX(canvas, event), this.pointerY(canvas, event));
      this.ctx.stroke();
    };
    const stop = () => {
      this.drawing = false;
    };

    canvas.onpointerdown = start;
    canvas.onpointermove = draw;
    canvas.onpointerup = stop;
    canvas.onpointerleave = stop;
  }

  private pointerX(canvas: HTMLCanvasElement, event: PointerEvent): number {
    const rect = canvas.getBoundingClientRect();
    return ((event.clientX - rect.left) / rect.width) * canvas.width;
  }

  private pointerY(canvas: HTMLCanvasElement, event: PointerEvent): number {
    const rect = canvas.getBoundingClientRect();
    return ((event.clientY - rect.top) / rect.height) * canvas.height;
  }
}
