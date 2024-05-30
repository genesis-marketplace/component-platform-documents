import { DI } from '@genesislcap/web-core';
import { Auth, Connect } from '@genesislcap/foundation-comms';
import { getEndpointUrl } from '../utils/endpoint';
import { logger } from '../utils';

export interface IDocumentService {
  uploadDocument(file: File): Promise<any>;
  downloadDocument(documentId: string);
}

class DocumentServiceImpl implements IDocumentService {
  constructor(
    @Connect private connect: Connect,
    @Auth private auth: Auth,
  ) {}

  async uploadDocument(file: File): Promise<any> {
    if (!file) {
      logger.error('No file provided.');
      return;
    }

    const headers = new Headers();
    headers.append('SESSION_AUTH_TOKEN', this.auth.loggedUserResult.authToken);
    const formData = new FormData();
    formData.append(file.name, file);

    const endpoint = getEndpointUrl('file-server/upload/');
    const response = await fetch(endpoint, {
      method: 'POST',
      body: formData,
      headers: headers,
    });

    return response.json();
  }

  downloadDocument(documentId: string) {
    const headers = new Headers();
    headers.append('SESSION_AUTH_TOKEN', sessionStorage.getItem('authToken'));

    const endpoint = getEndpointUrl(`file-server/download?fileStorageId=${documentId}`);

    let filename = '';
    fetch(endpoint, {
      method: 'GET',
      headers: headers,
    })
      .then((response) => {
        const contentDispositionHeader = response.headers.get('Content-disposition');
        filename = contentDispositionHeader.split(';')[1].split('filename')[1].split('=')[1].trim();
        return response.blob();
      })
      .then((blob) => URL.createObjectURL(blob))
      .then((url) => {
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
      })
      .catch((err) => {
        logger.error(err);
      });
  }
}

export const IDocumentService = DI.createInterface<IDocumentService>((x) =>
  x.singleton(DocumentServiceImpl),
);
