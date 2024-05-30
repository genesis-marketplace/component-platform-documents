import { SyntheticViewTemplate, TemplateElementDependency } from '@genesislcap/web-core';

/**
 * TemplateComponents.
 * @public
 */
export type TemplateComponents = {
  icon: TemplateElementDependency;
  button: TemplateElementDependency;
  badge: TemplateElementDependency;
  progress: TemplateElementDependency;
  searchBar: TemplateElementDependency;
  treeView: TemplateElementDependency;
  treeItem: TemplateElementDependency;
  grid: TemplateElementDependency;
};

/**
 * TemplateOptions.
 * @public
 */
export type TemplateOptions = Partial<TemplateComponents> & {
  /**
   * @remarks
   * Just for reference that template options may be more than tags.
   * @internal
   */
  somePartial?: string | SyntheticViewTemplate;
};

/**
 * defaultTemplateOptions.
 * @remarks
 * The default template options this MF has been created with.
 * @public
 */
export const defaultTemplateOptions: TemplateOptions = {
  icon: 'rapid-icon',
  button: 'rapid-button',
  badge: 'rapid-badge',
  progress: 'rapid-progress',
  searchBar: 'rapid-search-bar',
  treeView: 'rapid-tree-view',
  treeItem: 'rapid-tree-item',
  grid: 'rapid-grid-pro',
};
