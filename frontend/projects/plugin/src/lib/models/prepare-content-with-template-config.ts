import {PluginConfigurationData} from "@valtimo/plugin";

export interface PrepareContentWithTextTemplate extends PluginConfigurationData {
    xentialDocumentPropertiesId: string;
    firstTemplateGroupId: string;
    secondTemplateGroupId: string;
    thirdTemplateGroupId: string;
    fileFormat: FileFormat;
    eventMessageName: string;
}

type FileFormat = 'WORD' | 'PDF';
