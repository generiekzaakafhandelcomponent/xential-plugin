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
  GenerateDocumentWithBuildingBlockConfigurationComponent
} from "./components/generate-document-with-buildingblock-configuration/generate-document-with-building-block-configuration.component";
import {PrepareContentConfigurationComponent} from "./components/prepare-content-configuration/prepare-content-configuration.component";
import {ValidateAccessConfigurationComponent} from "./components/validate-access-configuration/validate-access-configuration.component";
import {
  SetSjabloonGroupIdConfigurationComponent
} from "./components/set-sjabloon-group-id-configuration/set-sjabloon-group-id-configuration.component";

const XentialPluginSpecification: PluginSpecification = {
  pluginId: 'xential',
  pluginConfigurationComponent: XentialConfigurationComponent,
  pluginLogoBase64: XENTIAL_PLUGIN_LOGO_BASE64,
  functionConfigurationComponents: {
    'generate-document': GenerateDocumentConfigurationComponent,
    'generate-document-with-building-block': GenerateDocumentWithBuildingBlockConfigurationComponent,
    'prepare-content': PrepareContentConfigurationComponent,
    'validate-xential-toegang': ValidateAccessConfigurationComponent,
    'set-sjabloon-group-id': SetSjabloonGroupIdConfigurationComponent
  },
  pluginTranslations: {
    nl: {
      title: 'Xential',
      description: 'Met de Xential plugin worden documenten gegenereerd',
      contentProcessVariable: 'Document content process variable',
      eventMessageName: 'bpmn event naam als document is ontvangen',
      verzendAdresData: 'geadresseerde data',
      colofonData: 'colofon data',
      documentDetailsData: 'Document details data',
      configurationTitle: 'Configuratie naam',
      clientId: 'Taak applicatie naam',
      clientPassword: 'Taak applicatie wachtwoord',
      'generate-document': 'Genereer document',
      'generate-document-with-building-block': 'Genereer document met bouwblok',
      textContent: 'inhoud voor het genereren van het document',
      sjabloonGroepId: 'Sjabloon groep id',
      sjabloonId: 'Sjabloon id van het te genereren document',
      messageName: 'bpmn bericht naam als document is gegenereerd',
      fileFormatInputType: 'Bestandsformaat invoertype',
      fileFormatInputTypeSelection: 'Selectie',
      fileFormatInputTypeText: 'Tekst',
      'prepare-content': 'kies inhoud op basis van een template',
      'validate-xential-toegang': 'Valideer toegang tot Xential Sjablonen',
      'set-sjabloon-group-id': 'Zet sjabloon groep id',
      sjabloonGroepNaam: 'Sjabloon groep naam',
      templateId: 'Template ID',
      fileFormat: 'Bestandsformaat',
      documentId: 'Document kenmerk',
      xentialGebruikersId: 'Default Xential gebruiker Id',
      templateData: 'Sjabloon vuldata',
      applicationName: 'Xential Taakapplicatie naam',
      applicationPassword: 'Xential Taakapplicatie wachtwoord',
      baseUrl: 'Base url naar Xential via ESB',
      serverCertificate: 'Server certificaat als Base64 encoded string',
      clientPrivateKey: 'Client private key als Base64 encoded string',
      clientCertificate: 'Client certificaat als Base64 encoded string',
      xentialDocumentPropertiesId: 'Document genereren Properties proces variabele',
      xentialDocumentProperties: 'Document genereren Properties',
      xentialData: 'inhoud voor het genereren van het document',
      xentialSjabloonId: 'Sjabloon id van het te genereren document',
      toegangResultaatId: 'Toegang tot xential test resultaat proces variabele',
      documentFilename: 'document bestandsnaam',
      informationObjectType: 'Informatie ObjectType'
    },
    en: {
      title: 'Xential',
      description: 'With the Xential plugin documents are generated',
      contentProcessVariable: 'Document content process variable',
      eventMessageName: 'bpmn event name when document arrives',
      verzendAdresData: 'addressee data',
      colofonData: 'colophon data',
      documentDetailsData: 'Document details data',
      configurationTitle: 'Configuration name',
      clientId: 'Client ID',
      clientPassword: 'Client password',
      'generate-document': 'Generate document',
      'generate-document-with-building-block': 'Generate document with building block',
      textContent: 'content for generating the document',
      sjabloonGroepId: 'Sjabloon group id',
      sjabloonId: 'Template ID of the document to be generated',
      messageName: 'bpmn message name when document is generated',
      fileFormatInputType: 'File format input type',
      fileFormatInputTypeSelection: 'Selection',
      fileFormatInputTypeText: 'Text',
      'prepare-content': 'Generate document content',
      'validate-xential-toegang': 'Validate access to Xential Sjablonen',
      'set-sjabloon-group-id': 'Set sjabloon group id',
      sjabloonGroepNaam: 'Sjabloon group name',
      templateId: 'Sjabloon ID',
      fileFormat: 'File format',
      documentFilename: 'document filename',
      documentId: 'Document ID',
      templateData: 'Template data',
      xentialGebruikersId: 'Default Xential user Id',
      informationObjectType: 'information ObjectType',
      applicationName: 'Xential Taakapplicatie name',
      applicationPassword: 'Xential Taakapplicatie password',
      baseUrl: 'Base url to ESB Xential',
      serverCertificate: 'Server certificate as Base64 encoded string',
      clientPrivateKey: 'Client private key as Base64 encoded string',
      clientCertificate: 'Client certificate as Base64 encoded string',
      xentialDocumentPropertiesId: 'Generate Document Properties Process Variable',
      xentialDocumentProperties: 'Generate Document Properties',
      xentialData: 'content for generating the document',
      xentialSjabloonId: 'Template ID of the document to be generated',
      toegangResultaatId: 'Access to xential test result process variable',
    }
  },
};

export {XentialPluginSpecification};
