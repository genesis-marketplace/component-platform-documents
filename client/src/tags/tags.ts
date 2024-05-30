import { tagFor } from '@genesislcap/foundation-ui';
import { DI } from '@genesislcap/web-core';
import { DocumentManagerConfig } from '../config/config';
import { defaultTemplateOptions } from '../config/templates';

/**
 * It's important this file isn't referenced ahead of a `configure` call, otherwise these values may remain fixed at
 * their defaults. Consumers must use the `/config` subpath to help avoid this. Files with references to tags should be
 * lazily loaded. There is an alternative `getTags` utility at the end which could offer another approach, but direct
 * tag exports and inline template references feel cleaner than having to convert all component `template` and `styles`
 * exports to functions to call `getTags` on execution.
 */

/**
 * @internal
 */
export const { templateOptions = defaultTemplateOptions } =
  DI.getOrCreateDOMContainer().get(DocumentManagerConfig);

/**
 * @internal
 */
export const iconTag = tagFor(templateOptions.icon);

/**
 * @internal
 */
export const buttonTag = tagFor(templateOptions.button);

/**
 * @internal
 */
export const badgeTag = tagFor(templateOptions.badge);

/**
 * @internal
 */
export const progressTag = tagFor(templateOptions.progress);

/**
 * @internal
 */
export const searchBarTag = tagFor(templateOptions.searchBar);
/**
 * @internal
 */
export const treeViewTag = tagFor(templateOptions.treeView);
/**
 * @internal
 */
export const treeItemTag = tagFor(templateOptions.treeItem);
/**
 * @internal
 */
export const gridTag = tagFor(templateOptions.grid);
