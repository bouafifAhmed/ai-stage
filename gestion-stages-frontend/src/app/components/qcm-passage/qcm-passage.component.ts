import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, inject, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { QcmExamen, QcmQuestion } from '../../models/qcm.model';
import { QcmService } from '../../services/qcm.service';

@Component({
  selector: 'app-qcm-passage',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './qcm-passage.component.html',
  styleUrl: './qcm-passage.component.scss',
})
export class QcmPassageComponent implements OnInit, OnDestroy {
  private readonly qcmService = inject(QcmService);
  private readonly fb = inject(FormBuilder);

  @Output() readonly passed = new EventEmitter<void>();

  examen: QcmExamen | null = null;
  resultat: {
    scorePourcentage: number;
    reussi: boolean;
    noteMinimalePassage: number;
    tentativesRestantes: number;
    message: string;
  } | null = null;

  form = this.fb.group({});
  loading = true;
  submitting = false;
  errorMessage = '';
  tempsRestantLabel = '';
  tempsEcoule = false;
  currentIndex = 0;
  showValidation = false;

  private timerId: ReturnType<typeof setInterval> | null = null;
  private finAutorisee = 0;

  get currentQuestion(): QcmQuestion | null {
    return this.examen?.questions[this.currentIndex] ?? null;
  }

  get isLastQuestion(): boolean {
    return !!this.examen && this.currentIndex === this.examen.questions.length - 1;
  }

  get answeredCount(): number {
    if (!this.examen) return 0;
    return this.examen.questions.filter((q) => this.isAnswered(q)).length;
  }

  get progressPercent(): number {
    if (!this.examen?.questions.length) return 0;
    return Math.round((this.answeredCount / this.examen.questions.length) * 100);
  }

  ngOnInit(): void {
    this.chargerExamen();
  }

  ngOnDestroy(): void {
    this.arreterTimer();
  }

  controlName(question: QcmQuestion): string {
    return `q_${question.id}`;
  }

  optionLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }

  isSelected(question: QcmQuestion, optionId: number): boolean {
    return Number(this.form.get(this.controlName(question))?.value) === optionId;
  }

  isAnswered(question: QcmQuestion): boolean {
    const value = this.form.get(this.controlName(question))?.value;
    return value !== null && value !== undefined && value !== '';
  }

  isQuestionInvalid(question: QcmQuestion): boolean {
    return this.showValidation && !this.isAnswered(question);
  }

  goToQuestion(index: number): void {
    if (!this.examen || index < 0 || index >= this.examen.questions.length) return;
    this.currentIndex = index;
    this.showValidation = false;
  }

  previousQuestion(): void {
    this.goToQuestion(this.currentIndex - 1);
  }

  nextQuestion(): void {
    const question = this.currentQuestion;
    if (!question) return;
    if (!this.isAnswered(question)) {
      this.showValidation = true;
      return;
    }
    this.goToQuestion(this.currentIndex + 1);
  }

  recommencer(): void {
    this.resultat = null;
    this.errorMessage = '';
    this.currentIndex = 0;
    this.showValidation = false;
    this.chargerExamen();
  }

  submit(): void {
    if (!this.examen) return;
    this.showValidation = true;
    if (this.form.invalid) {
      const firstUnanswered = this.examen.questions.findIndex((q) => !this.isAnswered(q));
      if (firstUnanswered >= 0) {
        this.currentIndex = firstUnanswered;
      }
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMessage = '';
    const reponses = this.examen.questions.map((question) => ({
      questionId: question.id,
      optionId: Number(this.form.get(this.controlName(question))?.value),
    }));

    this.qcmService.soumettre({ reponses }).subscribe({
      next: (response) => {
        this.arreterTimer();
        this.resultat = {
          scorePourcentage: response.scorePourcentage,
          reussi: response.reussi,
          noteMinimalePassage: response.noteMinimalePassage,
          tentativesRestantes: response.tentativesRestantes,
          message: response.message,
        };
        this.submitting = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "Le QCM n'a pas pu être soumis.";
        this.submitting = false;
      },
    });
  }

  private chargerExamen(): void {
    this.loading = true;
    this.qcmService.obtenirExamen().subscribe({
      next: (examen) => {
        this.examen = examen;
        this.initialiserFormulaire(examen);
        this.demarrerTimer(examen.dureeMinutes);
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de charger le QCM.';
        this.loading = false;
      },
    });
  }

  private initialiserFormulaire(examen: QcmExamen): void {
    const controls: Record<string, ReturnType<FormBuilder['control']>> = {};
    for (const question of examen.questions) {
      controls[this.controlName(question)] = this.fb.control('', Validators.required);
    }
    this.form = this.fb.group(controls);
  }

  private demarrerTimer(dureeMinutes: number): void {
    this.arreterTimer();
    this.finAutorisee = Date.now() + dureeMinutes * 60_000;
    this.mettreAJourTimer();
    this.timerId = setInterval(() => this.mettreAJourTimer(), 1000);
  }

  private mettreAJourTimer(): void {
    const restantMs = this.finAutorisee - Date.now();
    if (restantMs <= 0) {
      this.tempsRestantLabel = '00:00';
      this.tempsEcoule = true;
      this.arreterTimer();
      return;
    }
    const totalSeconds = Math.floor(restantMs / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    this.tempsRestantLabel = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    this.tempsEcoule = restantMs < 60_000;
  }

  private arreterTimer(): void {
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }
}
