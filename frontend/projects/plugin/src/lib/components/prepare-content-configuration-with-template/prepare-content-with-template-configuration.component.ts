import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FunctionConfigurationComponent} from '@valtimo/plugin';
import {
    BehaviorSubject,
    combineLatest, filter,
    map,
    Observable, startWith,
    Subscription,
    take,
} from 'rxjs';
import {PrepareContentWithTextTemplate} from "../../models";
import {SelectedValue, SelectItem} from "@valtimo/components";
import {XentialApiSjabloonService} from "../../modules/xential-api/services/xential-api-sjabloon.service";
import {KeycloakUserService} from "@valtimo/keycloak";

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

    protected readonly username$ = new BehaviorSubject<string>("");
    firstLevelGroupSelectItems$: BehaviorSubject<Array<{ id: string; text: string }>> = new BehaviorSubject<Array<{
        id: string;
        text: string
    }>>([]);
    secondLevelGroupSelectItems$: BehaviorSubject<Array<{ id: string; text: string }>> = new BehaviorSubject<Array<{
        id: string;
        text: string
    }>>([]);
    thirdLevelGroupSelectItems$: BehaviorSubject<Array<{ id: string; text: string }>> = new BehaviorSubject<Array<{
        id: string;
        text: string
    }>>([]);

    constructor(
        private readonly xentialApiSjabloonService: XentialApiSjabloonService,
        private readonly keycloakUserService: KeycloakUserService
    ) {
        this.getFirstLevelTemplate()
        this.keycloakUserService.getUserSubject()
            .subscribe(
                userIdentity => {
                    this.username$.next(userIdentity.username)
                }
            )
    }

    private saveSubscription!: Subscription;

    private readonly formValue$ = new BehaviorSubject<PrepareContentWithTextTemplate | null>(null);
    private readonly valid$ = new BehaviorSubject<boolean>(false);

    readonly firstGroupId$ = new BehaviorSubject<string>('')
    readonly secondGroupId$ = new BehaviorSubject<string>('')

    public fileFormats$ = new BehaviorSubject<SelectItem[]>(
        ['WORD', 'PDF']
            .map(format => {
                return {
                    id: format,
                    text: format
                }
            })
    );

    private currentFirstTemplateGroupId: string = "notset"
    private currentSecondTemplateGroupId: string = "notset"

    handleLevelSelected(groupId$: BehaviorSubject<string>, levelGroupSelectItems$: BehaviorSubject<Array<{
        id: string;
        text: string
    }>>) {
        combineLatest([
            this.username$,
            this.xentialApiSjabloonService.getTemplates(this.username$.getValue(), groupId$.getValue()),
        ])
            .pipe(
                map(([username, sjablonenList]) => {
                        levelGroupSelectItems$.next(
                            sjablonenList.sjabloongroepen.map(configuration => ({
                                        id: configuration.id,
                                        text: configuration.naam
                                    }
                                )
                            )
                        )
                    }
                )
            ).subscribe()
    }

    ngOnInit(): void {
        this.openSaveSubscription();
    }

    getFirstLevelTemplate() {
        this.username$
            .pipe(
                filter(gebruikersId => !!gebruikersId) // Filters out empty strings, null, or undefined
            )
            .subscribe(gebruikersId => {
                this.handleLevelSelected(this.firstGroupId$, this.firstLevelGroupSelectItems$);
            });
    }

    ngOnDestroy() {
        this.saveSubscription?.unsubscribe();
    }

    formValueChange(formValue: PrepareContentWithTextTemplate): void {
        if (formValue.firstTemplateGroupId &&
            formValue.firstTemplateGroupId != this.currentFirstTemplateGroupId) {
            this.currentFirstTemplateGroupId = formValue.firstTemplateGroupId
            this.firstGroupId$.next(formValue.firstTemplateGroupId)
            this.handleLevelSelected(this.firstGroupId$, this.secondLevelGroupSelectItems$)
        }
        if (formValue.secondTemplateGroupId &&
            formValue.secondTemplateGroupId != this.currentSecondTemplateGroupId) {
            this.currentSecondTemplateGroupId = formValue.secondTemplateGroupId
            this.secondGroupId$.next(formValue.secondTemplateGroupId)
            this.handleLevelSelected(this.secondGroupId$, this.thirdLevelGroupSelectItems$)
        }

        this.formValue$.next(formValue);
        this.handleValid(formValue);
    }

    private handleValid(formValue: PrepareContentWithTextTemplate): void {
        const valid = !!(
            formValue.xentialContentId &&
            formValue.firstTemplateGroupId &&
            formValue.fileFormat &&
            formValue.eventMessageName
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
