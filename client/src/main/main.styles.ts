import { css } from '@genesislcap/web-core';
import { treeItemTag } from '../tags';

export const DocumentManagerStyles = css`
  :host {
    font-family: var(--body-font);

    --badge-fill-CSV: var(--success-color);
    --badge-fill-PDF: var(--error-color);
    --badge-fill-default: #fff;
    --badge-fill-PNG: #8a98f5;
    --badge-fill-JPG: #8a98f5;
  }

  ::-webkit-scrollbar {
    width: calc(var(--base-height-multiplier) * 1px);
  }

  ::-webkit-scrollbar-track {
    background: var(--neutral-layer-1);
  }

  ::-webkit-scrollbar-thumb {
    background: var(--neutral-fill-rest);
  }

  .body {
    display: flex;
    height: inherit;
  }

  .container {
    height: 100%;
    width: 100%;
    background: var(--neutral-layer-3);
    position: relative;
    border-radius: calc((var(--control-corner-radius) + 2) * 1px);
    box-shadow: 0px 0px 20px 0px rgba(0, 0, 0, 0.44);
    border: calc(var(--stroke-width) * 1px) solid var(--neutral-stroke-divider-rest);
    display: flex;
    flex-direction: column;
  }

  .directory-list {
    display: flex;
    flex-direction: column;
  }

  .directory-btn,
  .directory-btn-selected {
    border-left: calc(var(--stroke-width) * 3px) solid transparent;
    margin: 0;
    border-radius: 0px;
    height: calc(((var(--base-height-multiplier) + 1) * 2px) + var(--design-unit) * 1px);
    background-color: transparent;
    box-sizing: border-box;
    font-size: var(--type-ramp-minus-1-font-size);
    color: var(--neutral-foreground-hint);
    text-align: left;
    line-height: var(--type-ramp-base-line-height);
  }

  .directory-btn-selected {
    border-left: calc(var(--stroke-width) * 3px) solid;
    color: var(--neutral-foreground-rest);
    border-image-slice: 1;
    border-image-source: linear-gradient(to bottom, #47bce0, #654df9);
    background-color: color-mix(in srgb, var(--neutral-foreground-rest), transparent 96%);
  }

  .footer {
    display: flex;
    align-items: center;
    justify-content: end;
    padding: calc(var(--design-unit) * 3px) calc(var(--design-unit) * 4px);
    font-size: var(--type-ramp-minus-2-font-size);
    border-top: calc(var(--stroke-width) * 1px) solid var(--neutral-stroke-divider-rest);
    color: var(--neutral-foreground-rest);
  }

  .header {
    padding: calc(var(--design-unit) * 4px);
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: calc(var(--stroke-width) * 1px) solid var(--neutral-stroke-divider-rest);;
  }

  .title {
    font-weight: 500;
    font-size: var(--type-ramp-plus-2-font-size);
    color: var(--neutral-foreground-rest);
    text-align: left;
    line-height: normal;
  }

  .pinned-left {
    width: 163px;
    padding: 0;
    border-right: calc(var(--stroke-width) * 1px) solid var(--neutral-stroke-divider-rest);;
    background-color: var(--neutral-layer-4);
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
  }

  .pinned-left-header {
    display: flex;
    height: calc(var(--base-height-multiplier) * 4px);
    padding: calc(var(--design-unit) * 2px) calc(var(--design-unit) * 4px);
    font-weight: 500;
    font-size: var(--type-ramp-minus-2-font-size);
    color: var(--neutral-foreground-hint);
    line-height: var(--type-ramp-base-line-height);
    align-items: center;
  }

  .pinned-right {
    display: flex;
    height: 100%;
    width: 100%;
    flex-direction: column;
  }

  .pinned-right-body {
    height: 100%;
    position: relative;
  }

  .pinned-right-header {
    display: flex;
    align-items: center;
    gap: calc(var(--design-unit) * 4px);
    height: calc(var(--base-height-multiplier) * 4px);
    border-bottom: calc(var(--stroke-width) * 1px) solid var(--neutral-stroke-divider-rest);;
    padding: calc(var(--design-unit) * 2px) calc(var(--design-unit) * 4px);
    justify-content: space-between;
  }

  .pinned-right-header-left-group {
    flex: 1 1;
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: calc(var(--design-unit) * 4px);
  }

  .upload-btn {
    color: var(--neutral-foreground-hint);
    font-size: var(--type-ramp-minus-1-font-size);
    background-color: var(--neutral-layer-3);
    margin: 0;
  }

  .uploading {
    color: var(--neutral-foreground-rest);
    background-color: var(--neutral-layer-1);
  }

  .upload-btn:hover {
    color: var(--neutral-foreground-rest);
  }

  .upload-btn > svg {
    width: calc((var(--base-height-multiplier) - 2 )* 3px);
  }

  .upload-btn::part(start) {
    margin-inline-end: calc(var(--design-unit) * 2px);
  }

 
  ${treeItemTag}::part(positioning-region),
  ${treeItemTag}::part(content-region) {
    background: var(--neutral-layer-4);
    height: calc(((var(--base-height-multiplier) + 1) * 2px) + var(--design-unit) * 1px);
  }
`;
