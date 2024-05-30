import { customElement, GenesisElement, observable } from '@genesislcap/web-core';
import { GridPro } from '@genesislcap/grid-pro';
import { Auth } from '@genesislcap/foundation-comms';
import { INPUT_MIN_LENGTH, SelectedOption, getCriteriaBuilder } from '@genesislcap/foundation-ui';
import { logger } from '../utils';
import { IDocumentService } from '../services/document.service';
import { folderIcon, userIcon } from '../components/utils/icons';
import { DocumentManagerConfig } from '../config/config'; // < keep subpath please
import { DocumentManagerTemplate as template } from './main.template';
import { DocumentManagerStyles as styles } from './main.styles';
import { DIRECTORY, IDirectories } from './main.types';

/**
 * FoundationDocumentManager.
 *
 * @remarks
 * Base MF export used by host application `configure` calls. It does not set up a design system or components. Both are
 * expected to be registered in the host application, which should provide the details of which to the MF via
 * TemplateOptions.
 *
 * See the {@link configure} hook for more information on how to set up and use this micro frontend in your application.
 *
 * @public
 */
@customElement({
  name: 'foundation-document-manager',
  template,
  styles,
})
export class FoundationDocumentManager extends GenesisElement {
  @IDocumentService docService: IDocumentService;
  @Auth auth!: Auth;
  @DocumentManagerConfig documentManagerConfig!: DocumentManagerConfig;

  public documentGrid!: GridPro;
  public resourceName = 'ALL_FILE_STORAGES';
  public searchBarConfig = [
    {
      field: 'FILE_NAME',
      label: (searchTerm) => `${searchTerm} as FILE_NAME`,
      createCriteria: getCriteriaBuilder,
      isEnabled: (searchTerm, selectedOption) => {
        return (
          searchTerm.length >= INPUT_MIN_LENGTH &&
          !selectedOption.some((e) => e.field === 'FILE_NAME')
        );
      },
    },
  ];

  @observable uploadingDocument: boolean = false;
  @observable gridCriteria: string = '';
  @observable directories: IDirectories[] = [
    {
      id: DIRECTORY.ALL,
      icon: folderIcon,
      title: 'All files',
      selected: true,
    },
    {
      id: DIRECTORY.MY_FILES,
      icon: userIcon,
      title: 'My files',
      selected: false,
    },
  ];
  @observable currentdirectory = DIRECTORY.ALL;
  @observable filteredDocumentsCount: number = 0;

  private currentFilter = '';

  getGridOptions() {
    const gridOptions = {
      animateRows: true,
      scrollbarWidth: 10,
      suppressHorizontalScroll: true,
      suppressCellFocus: true,
      getRowId: (params) => params.data.FILE_STORAGE_ID,
      onRowDataUpdated: () => {
        this.filteredDocumentsCount = this.documentGrid.gridProDatasource.rowData.size;
      },
    };
    return gridOptions;
  }

  public searchChanged(event: CustomEvent<Array<SelectedOption>>) {
    const options = event.detail;
    const criteriaBuilder = options
      .map((option) => {
        const { field, term, createCriteria } = option;
        if (createCriteria) {
          return createCriteria(field, term);
        } else {
          logger.debug('Cannot convert search option into criteria', option);
          return;
        }
      })
      .filter((x) => x)
      .join(' && ');
      this.currentFilter = criteriaBuilder;
      this.filterDocumentsByDirectory();
  }

  private filterDocumentsByDirectory() {
    if (this.currentFilter) {
      this.gridCriteria =
        this.currentdirectory === DIRECTORY.ALL
          ? this.currentFilter
          : `${this.currentFilter} && CREATED_BY == '${this.auth.currentUser.username}'`;
    } else {
      this.gridCriteria =
        this.currentdirectory === DIRECTORY.ALL
          ? ''
          : `CREATED_BY == '${this.auth.currentUser.username}'`;
    }
  }

  public startUploading() {
    this.currentdirectory = null;
    this.uploadingDocument = true;
  }

  public changeDirectory(directory: DIRECTORY) {
    this.currentdirectory = directory;
    this.uploadingDocument = false;
    this.filterDocumentsByDirectory();
  }

  public downloadDocument(event) {
    this.docService.downloadDocument(event);
  }
}

/**
 * RapidDocumentManager.
 *
 * @remarks
 * A rapid version that pre-registers rapid components and uses the rapid design system.
 *
 * @example
 * ```ts
 * import { RapidDocumentManager } from '@genesislcap/pbc-documents-ui';
 * ...
 * RapidDocumentManager
 * ```
 *
 * @example Load the micro frontend on-demand
 * ```ts

 *   const { RapidDocumentManager } = await import('@genesislcap/pbc-documents-ui');
 *   return RapidDocumentManager;
 * },
 * ```
 *
 * @public
 */
@customElement({
  name: 'rapid-document-manager',
  template,
  styles,
})
export class RapidDocumentManager extends FoundationDocumentManager {
  /**
   * @internal
   */
  async connectedCallback() {
    super.connectedCallback();
    await this.loadRemotes();
  }

  /**
   * @internal
   */
  protected async loadRemotes() {
    const { registerCommonRapidComponents } = await import('../components/rapid-components');
    await registerCommonRapidComponents();
  }
}

/**
 * ZeroDocumentManager.
 *
 * @remarks
 * A zero version that pre-registers zero components and uses the zero design system.
 *
 * @example
 * ```ts
 * import { ZeroDocumentManager } from '@genesislcap/pbc-documents-ui';
 * ...
 * ZeroDocumentManager
 * ```
 *
 * @example Load the micro frontend on-demand
 * ```ts

 *   const { ZeroDocumentManager } = await import('@genesislcap/pbc-documents-ui');
 *   return ZeroDocumentManager;
 * },
 * ```
 *
 * @public
 */
@customElement({
  name: 'zero-document-manager',
  template,
  styles,
})
export class ZeroDocumentManager extends FoundationDocumentManager {
  /**
   * @internal
   */
  async connectedCallback() {
    super.connectedCallback();
    await this.loadRemotes();
  }

  /**
   * @internal
   */
  protected async loadRemotes() {
    const { registerCommonZeroComponents } = await import('../components/zero-components');
    await registerCommonZeroComponents();
  }
}

/**
 * @privateRemarks
 * Keeps backwards compatability with original export.
 *
 * @public
 */
export class DocumentManager extends ZeroDocumentManager {}
