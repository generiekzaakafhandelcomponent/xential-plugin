import {PluginConfigurationData} from "@valtimo/plugin";

export interface PrepareContentWithTextTemplate extends PluginConfigurationData {
    xentialContentId: string;
    templateId: string;
    gebruikersId: string;
    fileFormat: FileFormat;
    documentId: string;
    eventMessageName: string;
    textTemplateId: string;
}

type FileFormat = 'WORD' | 'PDF';
