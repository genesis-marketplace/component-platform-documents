import { customEvent } from '@genesislcap/foundation-events';
import { html, ref, repeat, when } from '@genesislcap/web-core';
import type { DocumentManager } from './main';
import type { IDirectories } from './main.types';
import { uploadIcon } from '../components/utils/icons';
import { DOCUMENT_COLUMNS } from '../components/utils/column-config';
import { DocumentUpload } from '../components/document-upload/document-upload';
import { buttonTag, gridTag, searchBarTag, treeItemTag, treeViewTag } from '../tags';

DocumentUpload;

export const DocumentManagerTemplate = html<DocumentManager>`
  <div class="container">
    <div class="header">
      <a class="title">Document Manager</a>
    </div>
    <div class="body">
      <div class="pinned-left">
        <a class="pinned-left-header">DIRECTORIES</a>
        <div class="directory-list">
          <${treeViewTag}>
            ${repeat(
              (x) => x.directories,
              html<IDirectories>`
                <${treeItemTag}
                  class="directory-btn directory-btn${(x, c) =>
                    x.id == c.parent.currentdirectory ? '-selected' : ''}"
                  @click=${(x, c) => c.parent.changeDirectory(x.id)}
                >
                  ${(x) => x.icon} ${(x) => x.title}
                </${treeItemTag}>
              `,
            )}
          </${treeViewTag}>
        </div>
      </div>
      <div class="pinned-right">
        <div class="pinned-right-header">
          <span class="pinned-right-header-left-group">
            <${searchBarTag}
              :options="${(x) => x.searchBarConfig}"
              @selectionChange=${(x, ctx) => x.searchChanged(customEvent(ctx))}
            ></${searchBarTag}>
          </span>
          <${buttonTag}
            class="upload-btn ${(x) => (x.uploadingDocument ? 'uploading' : '')}"
            @click=${(x) => x.startUploading()}
          >
            ${uploadIcon('start')} Upload Document
          </${buttonTag}>
        </div>
        <div class="pinned-right-body">
          ${when(
            (x) => !x.uploadingDocument,
            html<DocumentManager>`
              <${gridTag} ${ref('documentGrid')} only-template-col-defs rowHeight="40">
                <grid-pro-client-side-datasource
                  resource-name=${(x) => x.resourceName}
                  :criteria="${(x) => x.gridCriteria}"
                  :deferredGridOptions="${(x) => x.getGridOptions()}"
                ></grid-pro-client-side-datasource>
                ${repeat(
                  (x) => DOCUMENT_COLUMNS(x),
                  html`
                    <grid-pro-column :definition="${(x) => x}"></grid-pro-column>
                  `,
                )}
              </${gridTag}-grid-pro>
            `,
          )}
          ${when(
            (x) => x.uploadingDocument,
            html<DocumentManager>`
              <document-upload></document-upload>
            `,
          )}
        </div>
      </div>
    </div>
    <div class="footer">
      ${when(
        (x) => !x.uploadingDocument && x.filteredDocumentsCount > 0,
        html<DocumentManager>`
          <a>${(x) => x.filteredDocumentsCount} Documents</a>
        `,
      )}
    </div>
  </div>
`;
