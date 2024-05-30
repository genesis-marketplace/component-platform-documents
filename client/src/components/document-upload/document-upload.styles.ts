import { css } from '@genesislcap/web-core';

export const DocumentUploadStyles = css`
  :host {
    height: 100%;
    width: 100%;
    padding: calc(var(--design-unit) * 3px);
    position: absolute;
    background-color: var(--neutral-layer-4);
    box-sizing: border-box;
  }

  .container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    border-radius: calc(var(--control-corner-radius) * 1px);
    border: calc(var(--stroke-width) * 1px) dashed var(--accent-fill-rest);
    background-color: var(--neutral-layer-3);
    box-sizing: border-box;
  }

  .clickable {
    cursor: pointer;
  }

  .dialog {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: calc(var(--design-unit) * 3px);
  }

  .document-name {
    font-size: var(--type-ramp-minus-1-font-size);
    color: var(--neutral-stroke-hover);
    text-align: center;
    line-height: var(--type-ramp-base-line-height);
  }

  .document-size {
    color: var(--neutral-foreground-hint);
    font-size: var(--type-ramp-minus-1-font-size);
    text-align: center;
    line-height: normal;
  }

  .drag-prompt {
    font-size: var(--type-ramp-minus-1-font-size);;
    color: var(--neutral-foreground-rest);
    text-align: center;
    line-height: normal;
  }

  .drag-prompt > span {
    color: var(--accent-fill-rest);
  }

  .progress {
    width: 261px;
    height: calc(var(--design-unit) * 1px);
  }

  .error-progress::part(determinate) {
    background-color: var(--error-color);
  }

  .type-prompt {
    font-size: var(--type-ramp-minus-1-font-size);
    color: var(--neutral-foreground-hint);
    text-align: center;
    line-height: normal;
  }

  .upload-icon,
  .error-icon {
    fill: var(--neutral-foreground-rest);
    -webkit-animation: animation-upload-icon 3s infinite;
    -moz-animation: animation-upload-icon 3s infinite;
    -o-animation: animation-upload-icon 3s infinite;
    animation: animation-upload-icon 3s infinite;
  }

  .upload-successfull::part(determinate) {
    background-color: var(--success-color);
  }

  .uploading-prompt {
    width: 134px;
    margin-left: 75px;
    font-size: var(--type-ramp-minus-1-font-size);
    color: var(--neutral-foreground-rest);
    line-height: normal;
  }

  .uploading-prompt:after {
    overflow: hidden;
    display: inline-block;
    vertical-align: bottom;
    -webkit-animation: ellipsis steps(4, end) 2s infinite;
    animation: ellipsis steps(4, end) 2s infinite;
    content: '...';
    width: 0px;
  }

  .success-icon {
    position: relative;
    height: calc(var(--base-height-multiplier) * 5px);
  }

  .success-icon > svg {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
  }

  @keyframes ellipsis {
    to {
      width: 14px;
    }
  }

  @-webkit-keyframes ellipsis {
    to {
      width: 14px;
    }
  }

  .error-icon {
    fill: var(--error-color);
  }

  @-webkit-keyframes animation-upload-icon {
    0% {
      opacity: 1;
    }
    50% {
      opacity: 0.3;
    }
    100% {
      opacity: 1;
    }
  }
  @keyframes animation-upload-icon {
    0% {
      opacity: 1;
    }
    50% {
      opacity: 0.3;
    }
    100% {
      opacity: 1;
    }
  }
`;
