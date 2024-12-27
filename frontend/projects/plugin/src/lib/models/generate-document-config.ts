interface GenerateDocumentConfig {
    templateId: string;
    fileFormat: FileFormat;
    documentId: string;
    messageName: string;
    contentProcessVariable: string;
}

type FileFormat = 'WORD' | 'PDF';

export {GenerateDocumentConfig, FileFormat}
