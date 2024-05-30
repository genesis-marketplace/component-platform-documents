import { customElement, DOM, GenesisElement, observable } from '@genesislcap/web-core';
import { IDocumentService } from '../../services/document.service';
import { logger } from '../../utils';
import { DocumentUploadStyles } from './document-upload.styles';
import { DocumentUploadTemplate } from './document-upload.template';
import { UploadState } from './document-upload.types';

@customElement({
  name: 'document-upload',
  template: DocumentUploadTemplate,
  styles: DocumentUploadStyles,
})
export class DocumentUpload extends GenesisElement {
  @IDocumentService docService: IDocumentService;

  @observable currentState: UploadState = UploadState.START;
  @observable fileName: string;
  @observable fileSize: number;
  @observable fileUploadProgress: number = 0;

  public dropArea: HTMLDivElement;
  public fileSelect: HTMLInputElement;
  public endpoint: string = '';

  private readonly errorViewTime: number = 7000;
  private readonly uploadingUploadedViewTime: number = 3000;
  private readonly uploadedViewTime: number = 3000;

  private file: any;

  connectedCallback() {
    super.connectedCallback();

    DOM.queueUpdate(() => {
      this.createFileUploadInput();

      ['dragenter', 'dragover', 'dragleave', 'drop'].forEach((eventName) => {
        this.dropArea.addEventListener(eventName, this.preventDefaults, false);
      });
      this.dropArea.addEventListener('drop', this.handleDrop.bind(this), false);
    });
  }

  public fileClick() {
    this.fileSelect.click();
  }

  private handleDrop(e) {
    if (this.currentState === UploadState.START) {
      const dt = e.dataTransfer;
      const files = dt.files;
      const contentTypes = [
        'text/csv',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'application/pdf',
        'image/jpg',
        'image/png',
      ];

      const firstElement = 0;

      if (files[firstElement] && contentTypes.includes(files[firstElement].type)) {
        const file = files[firstElement];
        this.updateSelectedFile(file);
      }
    }
  }

  private createFileUploadInput() {
    this.fileSelect = document.createElement('input') as HTMLInputElement;
    this.fileSelect.type = 'file';
    this.fileSelect.accept =
      'application/pdf,image/jpg,image/png,.csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    this.fileSelect.onchange = (event) => {
      logger.debug('file upload click');
      this.fileSelected(event);
    };
  }

  private fileSelected(event) {
    // Only take the first file
    const file = event.target.files[0];
    this.updateSelectedFile(file);
  }

  private updateSelectedFile(file: any) {
    this.file = file;
    this.fileName = this.file.name;
    this.fileSize = this.file.size;
    this.fileSelect.value = '';
    this.fileUploadProgress = 0;
    this.uploadedFile();
  }

  private uploadedFile() {
    this.currentState = UploadState.UPLOADING;
    this.docService.uploadDocument(this.file).then((x) => {
      if (!x.ERROR) {
        this.fileUploadProgress = 100;
        this.handleFinishUpload();
      } else {
        this.handleError(); // Add logging later
      }
    });
  }

  public cleanData() {
    this.file = undefined;
    this.fileName = undefined;
    this.fileSize = undefined;
    this.fileSelect.value = '';
    this.fileUploadProgress = 0;
    this.currentState = UploadState.START;
  }

  private preventDefaults(e) {
    e.preventDefault();
    e.stopPropagation();
  }

  private handleError() {
    this.currentState = UploadState.ERROR;
    setTimeout(() => {
      this.currentState = UploadState.START;
    }, this.errorViewTime);
  }

  private handleFinishUpload() {
    this.currentState = UploadState.UPLOADING_UPLOADED;
    setTimeout(() => {
      if (this.currentState === UploadState.UPLOADING_UPLOADED) {
        this.currentState = UploadState.UPLOADED;
        setTimeout(() => {
          this.cleanData();
        }, this.uploadedViewTime);
      }
    }, this.uploadingUploadedViewTime);
  }
}
