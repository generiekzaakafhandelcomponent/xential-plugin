interface GenerateDocumentConfig {
    templateId: string;
    fileFormat: FileFormat;
    documentId: string;
    messageName: "sfsefef"
    templateData: Array<{key: string; value: string}>
}

type FileFormat = 'WORD' | 'PDF';

export {GenerateDocumentConfig, FileFormat}
