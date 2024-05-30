interface IAction {
  name: string;
  callback: (any?) => void;
  actionName: string;
  disabled?: boolean;
}

interface IAgGridActions {
  actions: IAction[];
}

export class ActionCellRenderer {
  content: HTMLElement;

  constructor() {
    this.content = document.createElement('div');
  }

  init(params: IAgGridActions) {
    this.renderButtons(params);
  }

  renderButtons(params) {
    const { actions, data, enabled } = params;
    this.content.setAttribute('style', 'display: flex; gap: calc(var(--design-unit) * 2px); align-items: center;');
    actions.forEach((action: IAction) => {
      const button: HTMLElement = document.createElement('span');
      button.title = action.actionName;
      const defaultStyle = 'display: flex; cursor: pointer;';
      const defaultColor = 'fill: var(--neutral-foreground-hint);';
      button.setAttribute('style', defaultStyle + defaultColor);
      button.addEventListener('mouseover', () => {
        button.setAttribute('style', 'fill: var(--neutral-foreground-rest);' + defaultStyle);
      });
      button.addEventListener('mouseleave', () => [
        button.setAttribute('style', defaultStyle + defaultColor),
      ]);
      button.innerHTML = action.name;
      button.addEventListener('click', () => {
        action.callback(data);
      });
      this.content.appendChild(button);
    });
  }

  refresh(params): boolean {
    this.content.innerHTML = '';
    this.renderButtons(params);
    return true;
  }

  getGui(): HTMLElement {
    return this.content;
  }
}
