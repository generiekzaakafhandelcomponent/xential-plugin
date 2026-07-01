import {PluginConfigurationData} from "@valtimo/plugin";

export interface GenerateDocumentWithBuildingBlockConfig extends PluginConfigurationData {
  textContent: string;
  sjabloonGroepId: string;
  sjabloonId: string;
  xentialGebruikersId: string;
  fileFormatInputType: FileFormatInputType;
  fileFormat: FileFormat | string;
  messageName: string;
}

type FileFormat = 'WORD' | 'PDF';

export type FileFormatInputType = 'selection' | 'text';
