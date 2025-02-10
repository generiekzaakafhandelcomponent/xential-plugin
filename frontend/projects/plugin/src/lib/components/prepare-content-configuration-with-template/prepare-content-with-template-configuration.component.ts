import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FunctionConfigurationComponent} from '@valtimo/plugin';
import {BehaviorSubject, combineLatest, Observable, Subscription, take} from 'rxjs';
import {PrepareContentWithTextTemplate} from "../../models";
import {SelectItem} from "@valtimo/components";

@Component({
    selector: 'xential-prepare-content-with-template-configuration',
    templateUrl: './prepare-content-with-template-configuration.component.html'
})
export class PrepareContentWithTemplateConfigurationComponent implements FunctionConfigurationComponent, OnInit, OnDestroy {
    @Input() save$: Observable<void>;
    @Input() disabled$: Observable<boolean>;
    @Input() pluginId: string;
    @Input() prefillConfiguration$: Observable<PrepareContentWithTextTemplate>;
    @Output() valid: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() configuration: EventEmitter<PrepareContentWithTextTemplate> =
        new EventEmitter<PrepareContentWithTextTemplate>();

    public fileFormats$ = new BehaviorSubject<SelectItem[]>(
        ['WORD', 'PDF']
            .map(format => {
                return {
                    id: format,
                    text: format
                }
            })
    );

    private saveSubscription!: Subscription;

    private readonly formValue$ = new BehaviorSubject<PrepareContentWithTextTemplate | null>(null);
    private readonly valid$ = new BehaviorSubject<boolean>(false);

    ngOnInit(): void {
        this.openSaveSubscription();
    }

    ngOnDestroy() {
        this.saveSubscription?.unsubscribe();
    }

    formValueChange(formValue: PrepareContentWithTextTemplate): void {
        this.formValue$.next(formValue);
        this.handleValid(formValue);
    }

    private handleValid(formValue: PrepareContentWithTextTemplate): void {
        const valid = !!(
            formValue.xentialContentId &&
            formValue.gebruikersId &&
            formValue.templateId &&
            formValue.fileFormat &&
            formValue.documentId &&
            formValue.eventMessageName &&
            formValue.textTemplateId
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
