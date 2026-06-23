import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FunctionConfigurationComponent} from '@valtimo/plugin';
import {BehaviorSubject, combineLatest,Observable,Subscription,take} from 'rxjs';
import {SetSjabloonGroupIdConfig} from "../../models";

@Component({
  standalone: false,
    selector: 'xential-set-sjabloon-group-id-configuration',
    templateUrl: './set-sjabloon-group-id-configuration.component.html'
})
export class SetSjabloonGroupIdConfigurationComponent implements FunctionConfigurationComponent, OnInit, OnDestroy {
    @Input() save$: Observable<void>;
    @Input() disabled$: Observable<boolean>;
    @Input() pluginId: string;
    @Input() prefillConfiguration$: Observable<SetSjabloonGroupIdConfig>;
    @Output() valid: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() configuration: EventEmitter<SetSjabloonGroupIdConfig> = new EventEmitter<SetSjabloonGroupIdConfig>();

    private saveSubscription!: Subscription;

    private readonly formValue$ = new BehaviorSubject<SetSjabloonGroupIdConfig | null>(null);
    private readonly valid$ = new BehaviorSubject<boolean>(false);

    ngOnInit(): void {
        this.openSaveSubscription();
    }

    ngOnDestroy() {
        this.saveSubscription?.unsubscribe();
    }

    formValueChange(formValue: SetSjabloonGroupIdConfig): void {

        this.formValue$.next(formValue);
        this.handleValid(formValue);
    }

    private handleValid(formValue: SetSjabloonGroupIdConfig): void {
        const valid = !!(
            formValue.toegangResultaatId &&
            formValue.xentialGebruikersId &&
            formValue.sjabloonGroepNaam
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