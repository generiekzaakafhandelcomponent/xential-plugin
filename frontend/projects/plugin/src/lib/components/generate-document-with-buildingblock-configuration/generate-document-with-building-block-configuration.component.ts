import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FunctionConfigurationComponent} from '@valtimo/plugin';
import {BehaviorSubject, combineLatest, Observable, Subscription, take} from 'rxjs';
import {FileFormatInputType, GenerateDocumentWithBuildingBlockConfig} from "../../models";
import {RadioValue, SelectItem, ValuePathSelectorPrefix} from "@valtimo/components";

@Component({
  standalone: false,
    selector: 'xential-generate-document-with-building-block-configuration',
    templateUrl: './generate-document-with-building-block-configuration.component.html'
})
export class GenerateDocumentWithBuildingBlockConfigurationComponent implements FunctionConfigurationComponent, OnInit, OnDestroy {
    @Input() save$: Observable<void>;
    @Input() disabled$: Observable<boolean>;
    @Input() pluginId: string;
    @Input() prefillConfiguration$: Observable<GenerateDocumentWithBuildingBlockConfig>;
    @Output() valid: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() configuration: EventEmitter<GenerateDocumentWithBuildingBlockConfig> =
        new EventEmitter<GenerateDocumentWithBuildingBlockConfig>();

    private saveSubscription!: Subscription;

    private readonly formValue$ = new BehaviorSubject<GenerateDocumentWithBuildingBlockConfig | null>(null);
    private readonly valid$ = new BehaviorSubject<boolean>(false);

    public fileFormats$ = new BehaviorSubject<SelectItem[]>(
        ['WORD', 'PDF']
            .map(format => {
                return {
                    id: format,
                    text: format
                }
            })
    );

    public readonly fileFormatInputTypeOptions$ = new BehaviorSubject<RadioValue[]>([
        {value: 'selection', title: 'Selection', titleTranslationKey: 'fileFormatInputTypeSelection'},
        {value: 'text', title: 'Text', titleTranslationKey: 'fileFormatInputTypeText'},
    ]);

    public readonly selectedFileFormatInputType$ = new BehaviorSubject<FileFormatInputType>('selection');

    ngOnInit(): void {
        this.openSaveSubscription();
    }

    ngOnDestroy() {
        this.saveSubscription?.unsubscribe();
    }

    formValueChange(formValue: GenerateDocumentWithBuildingBlockConfig): void {
        this.selectedFileFormatInputType$.next(formValue.fileFormatInputType || 'selection');
        this.formValue$.next(formValue);
        this.handleValid(formValue);
    }

    private handleValid(formValue: GenerateDocumentWithBuildingBlockConfig): void {
        const valid = !!(
            formValue.textContent &&
            formValue.sjabloonGroepId &&
            formValue.sjabloonId &&
            formValue.xentialGebruikersId &&
            formValue.fileFormat &&
            formValue.messageName
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

    protected readonly ValuePathSelectorPrefix = ValuePathSelectorPrefix;
}
