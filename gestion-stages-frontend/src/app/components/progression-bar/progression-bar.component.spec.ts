import { TestBed } from '@angular/core/testing';

import { ProgressionBarComponent } from './progression-bar.component';

describe('ProgressionBarComponent', () => {
  function barre(pourcentage: number) {
    TestBed.configureTestingModule({ imports: [ProgressionBarComponent] });
    const fixture = TestBed.createComponent(ProgressionBarComponent);
    fixture.componentRef.setInput('pourcentage', pourcentage);
    fixture.detectChanges();
    return fixture;
  }

  it('is red under 30 %, orange up to 70 % and green above', () => {
    expect(barre(29.9).componentInstance.getColor()).toBe('#ef4444');
    TestBed.resetTestingModule();
    expect(barre(70).componentInstance.getColor()).toBe('#f59e0b');
    TestBed.resetTestingModule();
    expect(barre(70.1).componentInstance.getColor()).toBe('#10b981');
  });

  it('shows the rounded percentage', () => {
    const fixture = barre(13.33);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('13%');
  });
});
