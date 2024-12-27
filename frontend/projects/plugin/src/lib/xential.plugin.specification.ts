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

import {PluginSpecification} from '@valtimo/plugin';
import {XentialConfigurationComponent} from './components/xential-configuration/xential-configuration.component';
import {XENTIAL_PLUGIN_LOGO_BASE64} from './assets';
import {
    GenerateDocumentConfigurationComponent
} from "./components/generate-document-configuration/generate-document-configuration.component";
import {
    PrepareContentConfigurationComponent
} from "./components/prepare-content-configuration/prepare-content-configuration.component";

const XentialPluginSpecification: PluginSpecification = {
    pluginId: 'xential',
    pluginConfigurationComponent: XentialConfigurationComponent,
    pluginLogoBase64: XENTIAL_PLUGIN_LOGO_BASE64,
    functionConfigurationComponents: {
        'generate-document': GenerateDocumentConfigurationComponent,
        'prepare-content': PrepareContentConfigurationComponent
    },
    pluginTranslations: {
        nl: {
            title: 'Xential',
            description: 'Xential plugin',
            configurationTitle: 'Configuratie naam',
            clientId: 'Taak applicatie naam',
            clientPassword: 'Taak applicatie wachtwoord',
            'generate-document': 'Genereer document',
            templateId: 'Template ID',
            fileFormat: 'Bestandsformaat',
            documentId: 'Document kenmerk',
            templateData: 'Sjabloon vuldata',
            zakenApiPluginConfiguration: "Zaken API plugin",
            documentenApiPluginConfiguration: "documenten API plugin",
        },
        en: {
            title: 'Xential',
            description: 'Xential plugin',
            configurationTitle: 'Configuration name',
            clientId: 'Client ID',
            clientPassword: 'Client password',
            'generate-document': 'Generate document',
            templateId: 'Sjabloon ID',
            fileFormat: 'File format',
            documentId: 'Document ID',
            templateData: 'Template data',
            zakenApiPluginConfiguration: "Zaken API plugin",
            documentenApiPluginConfiguration: "documenten API plugin",
        },
        de: {
            title: 'Xential',
            description: 'Xential plugin',
            configurationTitle: 'Konfigurationsname',
            clientId: 'Kunden-ID',
            clientPassword: 'Kundenpasswort',
            'generate-document': 'Dokument generieren',
            templateId: 'Vorlage ID',
            fileFormat: 'Dateiformat',
            documentId: 'Dokument-ID',
            templateData: 'Vorlagendaten',
            zakenApiPluginConfiguration: "Zaken API plugin",
            documentenApiPluginConfiguration: "documenten API plugin",
        },
    },
};

export {XentialPluginSpecification};
