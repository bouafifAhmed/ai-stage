import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AbstractControl } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { QcmExamen, SoumettreQcmRequest } from '../../models/qcm.model';
import { QcmService } from '../../services/qcm.service';
import { QcmPassageComponent } from './qcm-passage.component';

describe('QcmPassageComponent', () => {
  let fixture: ComponentFixture<QcmPassageComponent>;
  let component: QcmPassageComponent;
  let soumettre: jasmine.Spy;

  const examen: QcmExamen = {
    id: 1, titre: 'Admissibilité', domaine: 'Informatique', dureeMinutes: 2,
    noteMinimalePassage: 50, nombreMaxTentatives: 2,
    questions: [
      { id: 10, enonce: '2 + 2 ?', type: 'CHOIX_UNIQUE', points: 1, ordre: 1,
        options: [{ id: 100, libelle: '4', ordre: 1 }, { id: 101, libelle: '5', ordre: 2 }] },
      { id: 11, enonce: 'Java est-il compilé ?', type: 'VRAI_FAUX', points: 1, ordre: 2,
        options: [{ id: 110, libelle: 'Vrai', ordre: 1 }, { id: 111, libelle: 'Faux', ordre: 2 }] },
    ],
  };

  beforeEach(() => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date(2026, 9, 18, 10, 0, 0));
    soumettre = jasmine.createSpy('soumettre').and.returnValue(of({
      scorePourcentage: 100, reussi: true, noteMinimalePassage: 50, tentativesRestantes: 1, message: 'Félicitations !',
    }));
    TestBed.configureTestingModule({
      imports: [QcmPassageComponent],
      providers: [{ provide: QcmService, useValue: { obtenirExamen: () => of(examen), soumettre } }],
    });
    fixture = TestBed.createComponent(QcmPassageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    jasmine.clock().uninstall();
  });

  const repondre = (questionId: number, optionId: number) =>
    (component.form.get(`q_${questionId}`) as AbstractControl).setValue(String(optionId));

  it('loads the exam with one required field per question and starts the timer', () => {
    expect(component.loading).toBeFalse();
    expect(Object.keys(component.form.controls)).toEqual(['q_10', 'q_11']);
    expect(component.tempsRestantLabel).toBe('02:00');
    expect(component.optionLetter(1)).toBe('B');
  });

  it('does not move to the next question until it is answered', () => {
    component.nextQuestion();
    expect(component.currentIndex).toBe(0);
    expect(component.isQuestionInvalid(examen.questions[0])).toBeTrue();

    repondre(10, 100);
    component.nextQuestion();
    expect(component.currentIndex).toBe(1);
    expect(component.isLastQuestion).toBeTrue();
    expect(component.progressPercent).toBe(50);
  });

  it('jumps to the first unanswered question instead of submitting an incomplete exam', () => {
    repondre(11, 110);
    component.goToQuestion(1);

    component.submit();

    expect(soumettre).not.toHaveBeenCalled();
    expect(component.currentIndex).toBe(0);
  });

  it('submits every answer as numbers and shows the result', () => {
    repondre(10, 100);
    repondre(11, 111);

    component.submit();

    expect(soumettre).toHaveBeenCalledWith({
      reponses: [{ questionId: 10, optionId: 100 }, { questionId: 11, optionId: 111 }],
    } as SoumettreQcmRequest);
    expect(component.resultat?.reussi).toBeTrue();
    expect(component.submitting).toBeFalse();
  });

  it('shows the server message when submission fails', () => {
    soumettre.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'Tentatives épuisées' } })));
    repondre(10, 100);
    repondre(11, 110);

    component.submit();

    expect(component.errorMessage).toBe('Tentatives épuisées');
    expect(component.resultat).toBeNull();
  });

  it('counts down and warns during the last minute', () => {
    jasmine.clock().tick(61_000);
    expect(component.tempsRestantLabel).toBe('00:59');
    expect(component.tempsEcoule).toBeTrue();

    jasmine.clock().tick(60_000);
    expect(component.tempsRestantLabel).toBe('00:00');
  });
});
