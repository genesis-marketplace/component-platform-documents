import { DI, PartialGenesisElementDefinition } from '@genesislcap/web-core';
import { defaultTemplateOptions, TemplateOptions } from './templates';

/**
 * Do not reference any ../main files here. We must avoid ../tags being referenced and therefore setup pre-configuration.
 */

/**
 * DocumentManagerConfig DI interface.
 *
 * @public
 */
export interface DocumentManagerConfig extends PartialGenesisElementDefinition {
  /**
   * Template options.
   *
   * @remarks
   * Used by host applications to assign MF template options and subcomponent tags to align with the host design system.
   */
  templateOptions: TemplateOptions;
  /**
   * Design System prefix for foundation-form
   *
   * @remarks
   * Used by foundation-form to specify the design system used by the form elements
   */
  designSystemPrefix: string;
}

/**
 * Default DocumentManagerConfig DI implementation.
 * @public
 */
export const defaultDocumentManagerConfig: DocumentManagerConfig = {
  name: 'foundation-document-manager',
  templateOptions: defaultTemplateOptions,
  designSystemPrefix: 'rapid',
};

/**
 * DocumentManagerConfig DI key.
 *
 * @internal
 * @privateRemarks
 * Marked as internal to stop api-extractor becoming confused cross-linking tokens with the same name.
 */
export const DocumentManagerConfig = DI.createInterface<DocumentManagerConfig>((x) => x.instance(defaultDocumentManagerConfig));
