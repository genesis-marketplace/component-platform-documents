import { DI, GenesisElement, Registration } from '@genesislcap/web-core';
import { DocumentManagerConfig, defaultDocumentManagerConfig } from './config';

/**
 * @remarks
 * Configure the alert micro frontend for host app integration.
 * 
 * @example Providing template options to align to your host application.
 * ```ts
 * const { configure } = await import('@genesislcap/pbc-documents-ui/config');
 *  return configure({
 *    name: 'rapid-document-manager',
 *    templateOptions: {
 *     icon: 'rapid-icon',
 *     button: 'rapid-button',
 *      badge: 'rapid-badge',
 *     progress: 'rapid-progress',
 *     searchBar: 'rapid-search-bar',
 *     treeView: 'rapid-tree-view',
 *     treeItem: 'rapid-tree-item',
 *     grid: 'rapid-grid-pro',
 *   },
 *   designSystemPrefix: 'rapid',
 *  });
 * },
 * ```
 * 
 * This is just an example, as there is a `RapidDocumentManager` which you can import and use.
 *
 * @param config - A partial DocumentManagerConfig.
 * @public
 */
export async function configure(config: Partial<DocumentManagerConfig>) {
  /**
   * Merge the configs
   */
  const value: DocumentManagerConfig = {
    ...defaultDocumentManagerConfig,
    ...config,
  };
  if (config.templateOptions) {
    value.templateOptions = {
      ...defaultDocumentManagerConfig.templateOptions,
      ...config.templateOptions,
    };
  }
  /**
   * Register a new DocumentManagerConfig
   */
  DI.getOrCreateDOMContainer().register(
    Registration.instance<DocumentManagerConfig>(DocumentManagerConfig, value),
  );
  /**
   * Lazily reference and define the micro frontend element post config setting.
   */
  const { DocumentManager, DocumentManagerStyles, DocumentManagerTemplate } = await import(
    '../main'
  );
  const { name, attributes, shadowOptions, elementOptions } = value;
  return GenesisElement.define(DocumentManager, {
    name,
    template: DocumentManagerTemplate,
    styles: DocumentManagerStyles,
    attributes,
    shadowOptions,
    elementOptions,
  });
}
