import { html, ref, when } from '@genesislcap/web-core';
import { progressTag } from '../../tags';
import { formatBytes } from '../../utils/formatting';
import { checkmark, checkmarkBackground, checkmarkCircle, uploadIconLarge, } from '../utils/icons';
import type { DocumentUpload } from './document-upload';
import { UploadState } from './document-upload.types';

export const DocumentUploadTemplate = html<DocumentUpload>`
  <div class="container" ${ref('dropArea')}>
    ${when(
      (x) => x.currentState === UploadState.START,
      html<DocumentUpload>`
        <div class="dialog">
          <span class="upload-icon">${uploadIconLarge}</span>
          <a class="drag-prompt clickable" @click=${(x) => x.fileClick()}>
            Drag and drop or
            <span>select</span>
            a file to upload.
          </a>
          <a class="type-prompt">Allows file type: PDF, CSV, JPG, PNG</a>
        </div>
      `,
    )}
    ${when(
      (x) =>
        x.currentState === UploadState.UPLOADING ||
        x.currentState === UploadState.UPLOADING_UPLOADED,
      html<DocumentUpload>`
        <div class="dialog">
          <span class="upload-icon">${uploadIconLarge}</span>
          <a
            class="${(x) =>
              x.currentState === UploadState.UPLOADING ? 'uploading-prompt' : 'drag-prompt'}"
          >
            ${(x) => (x.currentState === UploadState.UPLOADING ? 'Uploading' : 'File Uploaded')}
          </a>
          <a class="document-name">${(x) => x.fileName}</a>
          <a class="document-size">${(x) => formatBytes(x.fileSize, 2)}</a>
          <${progressTag}
            class="progress ${(x) =>
              x.currentState === UploadState.UPLOADING_UPLOADED || x.fileUploadProgress >= 100
                ? 'upload-successfull'
                : ''}"
            min="0"
            max="100"
            value=${(x) => x.fileUploadProgress}
          ></${progressTag}>
        </div>
      `,
    )}
    ${when(
      (x) => x.currentState === UploadState.UPLOADED,
      html<DocumentUpload>`
        <div class="dialog">
          <span class="success-icon">${checkmarkBackground} ${checkmarkCircle} ${checkmark}</span>
          <a class="drag-prompt">Document successfully uploaded</a>
        </div>
      `,
    )}
    ${when(
      (x) => x.currentState === UploadState.ERROR,
      html<DocumentUpload>`
        <div class="dialog">
          <span class="error-icon">${uploadIconLarge}</span>
          <a class="drag-prompt">Error uploading file. Please try again.</a>
          <a class="document-name">${(x) => x.fileName}</a>
          <a class="document-size">${(x) => formatBytes(x.fileSize, 2)}</a>
          <${progressTag}
            class="progress error-progress"
            min="0"
            max="100"
            value="100"
          ></${progressTag}>
        </div>
      `,
    )}
  </div>
`;
