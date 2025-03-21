import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FunctionConfigurationComponent, PluginManagementService, PluginTranslationService} from '@valtimo/plugin';
import {BehaviorSubject, combineLatest, map, Observable, Subscription, take} from 'rxjs';
import {PrepareContentTemplate} from "../../models";
import {SelectItem} from "@valtimo/components";
import {XentialApiSjabloonService} from "../../modules/xential-api/services/xential-api-sjabloon.service";

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

    public fileFormats$ = new BehaviorSubject<SelectItem[]>(
        ['WORD', 'PDF']
            .map(format => {
                return {
                    id: format,
                    text: format
                }
            })
    );

    constructor(
        private readonly xentialApiSjabloonService: XentialApiSjabloonService
    ) {}

    private saveSubscription!: Subscription;
    private readonly formValue$ = new BehaviorSubject<PrepareContentTemplate | null>(null);
    private readonly valid$ = new BehaviorSubject<boolean>(false);

    readonly xentialSjablonenSelectItems$: Observable<Array<{ id: string; text: string }>> =
        combineLatest([
            this.xentialApiSjabloonService.getTemplates(),
        ]).pipe(
            map(([configurations]) =>
                configurations.sjablonen.map(configuration => ({
                    id: configuration.id,
                    text: configuration.naam
                }))
            )
        );

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
            formValue.xentialContentId &&
            formValue.gebruikersId &&
            formValue.templateId &&
            formValue.fileFormat &&
            formValue.documentId &&
            formValue.eventMessageName &&
            !formValue.documentDetailsData.find((entry) => !(entry.key && entry.value)) &&
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
