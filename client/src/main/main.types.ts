export enum DIRECTORY {
    ALL = 'ALL',
    MY_FILES = 'MY_FILES',
}

export interface IDirectories {
    id: DIRECTORY;
    icon: any;
    title: string;
    selected: boolean;
}
