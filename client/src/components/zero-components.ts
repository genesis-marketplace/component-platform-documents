import { foundationLayoutComponents } from '@genesislcap/foundation-layout';
import {
  provideDesignSystem,
  zeroIcon,
  zeroButton,
  zeroProgress,
  zeroSearchBar,
  zeroTreeView,
  zeroTreeItem,
} from '@genesislcap/foundation-zero';
import { zeroGridComponents } from '@genesislcap/foundation-zero-grid-pro';

/**
 * @public
 */
export const registerCommonZeroComponents = async () => {
  /**
   * Register the components the app is using with the system.
   */
  provideDesignSystem()
    .register(
      /**
       * Common across most routes, so batch register these lightweight components upfront.
       */
      zeroIcon(),
      zeroButton(),
      zeroProgress(),
      zeroSearchBar(),
      zeroTreeView(),
      zeroTreeItem(),
      zeroGridComponents,
      foundationLayoutComponents,
    );
};
