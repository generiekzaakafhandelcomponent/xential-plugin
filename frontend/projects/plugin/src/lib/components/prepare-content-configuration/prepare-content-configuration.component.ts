import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FunctionConfigurationComponent} from '@valtimo/plugin';
import {BehaviorSubject, combineLatest, Observable, Subscription, take} from 'rxjs';
import {PrepareContentTemplate} from "../../models";

@Component({
    selector: 'xential-prepare-content-configuration',
    templateUrl: './prepare-content-configuration.component.html'
})
export class PrepareContentConfigurationComponent implements FunctionConfigurationComponent, OnInit, OnDestroy {
    @Input() save$: Observable<void>;
    @Input() disabled$: Observable<boolean>;
    @Input() pluginId: string;
    @Input() prefillConfiguration$: Observable<PrepareContentTemplate>;
    @Output() valid: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() configuration: EventEmitter<PrepareContentTemplate> =
        new EventEmitter<PrepareContentTemplate>();

    private saveSubscription!: Subscription;

    private readonly formValue$ = new BehaviorSubject<PrepareContentTemplate | null>(null);
    private readonly valid$ = new BehaviorSubject<boolean>(false);

    ngOnInit(): void {
        this.openSaveSubscription();
    }

    ngOnDestroy() {
        this.saveSubscription?.unsubscribe();
    }

    formValueChange(formValue: PrepareContentTemplate): void {
        this.formValue$.next(formValue);
        this.handleValid(formValue);
    }

    private handleValid(formValue: PrepareContentTemplate): void {
        const valid = !!(
            formValue.resultProcessVariableName &&
            !formValue.creatieData.find((entry) => !(entry.key && entry.value)) &&
            !formValue.colofonData.find((entry) => !(entry.key && entry.value)) &&
            !formValue.verzendAdresData.find((entry) => !(entry.key && entry.value))
        );

        this.valid$.next(valid);
        this.valid.emit(valid);
    }

    private openSaveSubscription(): void {
        this.saveSubscription = this.save$?.subscribe(save => {
            combineLatest([this.formValue$, this.valid$])
                .pipe(take(1))
                .subscribe(([formValue, valid]) => {
                    if (valid) {
                        this.configuration.emit(formValue);
                    }
                });
        });
    }
}
