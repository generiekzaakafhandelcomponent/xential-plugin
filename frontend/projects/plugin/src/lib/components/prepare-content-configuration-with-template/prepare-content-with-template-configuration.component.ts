import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FunctionConfigurationComponent} from '@valtimo/plugin';
import {
    BehaviorSubject,
    combineLatest,
    map,
    Observable,
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
    protected readonly user$ = new BehaviorSubject<string>("");
    firstLevelGroupSelectItems$: BehaviorSubject<Array<{ id: string; text: string }>>;
    secondLevelGroupSelectItems$: BehaviorSubject<Array<{ id: string; text: string }>>;
    thirdLevelGroupSelectItems$: BehaviorSubject<Array<{ id: string; text: string }>>;

    constructor(
        private readonly xentialApiSjabloonService: XentialApiSjabloonService,
        private readonly keycloakUserService: KeycloakUserService
    ) {
        // this.user$.subscribe(user =>
        //     this.user$.next(user)
        // )
    }

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

    private saveSubscription!: Subscription;

    private readonly formValue$ = new BehaviorSubject<PrepareContentWithTextTemplate | null>(null);
    private readonly valid$ = new BehaviorSubject<boolean>(false);

    ngOnInit(): void {
        console.log("ngOnInit")
        this.openSaveSubscription();
        this.getFirstLevelTemplate()
        console.log("ngOnInit 2")
        this.keycloakUserService.getUserSubject()
            .subscribe(
                userIdentity => {
                    console.log("ngOnInit 3 " + userIdentity.username)
                    this.user$.next(userIdentity.username)

                }
            )
    }

    getFirstLevelTemplate() {
        console.log("getFirstLevelTemplate")
        this.user$.subscribe(user => {
                console.log("getFirstLevelTemplate user subscribed")
                combineLatest([
                    this.xentialApiSjabloonService.getTemplates(user),
                ]).pipe(
                    map(([sjablonenList]) => {
                            let val = sjablonenList.sjabloongroepen.map(configuration => ({
                                id: configuration.id,
                                text: configuration.naam
                            }))

                            this.firstLevelGroupSelectItems$.next(val)
                        }
                    )
                )
            }
        )
    }

    ngOnDestroy() {
        this.saveSubscription?.unsubscribe();
    }

    formValueChange(formValue: PrepareContentWithTextTemplate): void {

        if (formValue.firstTemplateGroupId &&
            formValue.firstTemplateGroupId != this.currentFirstTemplateGroupId) {
            this.currentFirstTemplateGroupId = formValue.firstTemplateGroupId
            this.firstGroupId$.next(formValue.firstTemplateGroupId)
        }
        if (formValue.secondTemplateGroupId &&
            formValue.secondTemplateGroupId != this.currentSecondTemplateGroupId) {
            this.secondGroupId$.next(formValue.secondTemplateGroupId)
            this.currentSecondTemplateGroupId = formValue.secondTemplateGroupId
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

    currentFirstTemplateGroupId: string = "notset"
    currentSecondTemplateGroupId: string = "notset"

    handleFirstLevelSelected($event: Event) {
        // handle me and emit new row value
    }

    // handleSecondLevelSelected(selectedIndex: SelectedValue) {
    //     combineLatest([
    //         this.user$,
    //         this.firstLevelGroupSelectItems$,
    //         this.firstGroupId$
    //     ])
    //         .pipe(take(1))
    //         .subscribe(([user, firstLevelGroup, firstGroupId]) => {
    //             this.xentialApiSjabloonService.getTemplates(
    //                 user,
    //                 firstLevelGroup[selectedIndex]
    //             ).pipe(
    //                 map((sjablonenList) =>
    //                     sjablonenList.sjabloongroepen.map(configuration => ({
    //                         id: configuration.id,
    //                         text: configuration.naam
    //                     }))
    //                 ),
    //                 map((filteredList) => {
    //
    //                     this.secondLevelGroupSelectItems$.next(filteredList)
    //                 }))
    //
    //         })
    // }
}
