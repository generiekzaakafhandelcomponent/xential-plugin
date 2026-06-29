/*
 * Copyright 2015-2022 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {NgModule} from '@angular/core';
import {XentialConfigurationComponent} from './components/xential-configuration/xential-configuration.component';
import {CommonModule} from '@angular/common';
import {PluginTranslatePipeModule} from '@valtimo/plugin';
import {
    CarbonMultiInputModule,
    FormModule, InputLabelModule,
    InputModule,
    RadioModule,
    SelectModule,
    ValuePathSelectorComponent
} from '@valtimo/components';
import {
    GenerateDocumentConfigurationComponent
} from "./components/generate-document-configuration/generate-document-configuration.component";
import {
    GenerateDocumentWithBuildingBlockConfigurationComponent
} from "./components/generate-document-with-buildingblock-configuration/generate-document-with-building-block-configuration.component";
import {DropdownModule} from "carbon-components-angular";
import {
    PrepareContentConfigurationComponent
} from "./components/prepare-content-configuration/prepare-content-configuration.component";
import {
    ValidateAccessConfigurationComponent
} from "./components/validate-access-configuration/validate-access-configuration.component";
import {
    SetSjabloonGroupIdConfigurationComponent
} from "./components/set-sjabloon-group-id-configuration/set-sjabloon-group-id-configuration.component";

@NgModule({
    declarations: [
        XentialConfigurationComponent,
        GenerateDocumentConfigurationComponent,
        GenerateDocumentWithBuildingBlockConfigurationComponent,
        PrepareContentConfigurationComponent,
        ValidateAccessConfigurationComponent,
        SetSjabloonGroupIdConfigurationComponent
    ],
    imports: [
        CommonModule,
        PluginTranslatePipeModule,
        FormModule,
        InputModule,
        RadioModule,
        SelectModule,
        DropdownModule,
        CarbonMultiInputModule,
        ValuePathSelectorComponent,
        InputLabelModule
    ],
    exports: [
        XentialConfigurationComponent,
        GenerateDocumentConfigurationComponent,
        GenerateDocumentWithBuildingBlockConfigurationComponent,
        PrepareContentConfigurationComponent,
        ValidateAccessConfigurationComponent,
        SetSjabloonGroupIdConfigurationComponent
    ],
})
export class XentialPluginModule {
}
