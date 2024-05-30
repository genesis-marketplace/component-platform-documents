import { formatBytes, formatDateLong, getFileType } from '../../utils/formatting';
import { ActionCellRenderer } from './action-cell-renderer';
import type { DocumentManager } from '../../main';
import { downloadIcon } from './icons';
import { badgeTag } from '../../tags';

export enum COLUMN_FIELDS {
  FILE_STORAGE_ID = 'FILE_STORAGE_ID',
  STORAGE_MANAGER = 'STORAGE_MANAGER',
  FILE_NAME = 'FILE_NAME',
  FILE_SIZE = 'FILE_SIZE',
  MODIFIED_AT = 'MODIFIED_AT',
  MODIFIED_BY = 'MODIFIED_BY',
  CREATED_BY = 'CREATED_BY',
  CREATED_AT = 'CREATED_AT',
  LOCATION_DETAILS = 'LOCATION_DETAILS',
}
const fileTypes = ['CSV', 'JPG', 'PNG', 'PDF'];
const customFileTypeRenderer = (params) => {
  const { data } = params;
  const fileType = getFileType(data.FILE_NAME);
  return `<${badgeTag} fill="${fileTypes.includes(fileType) ? fileType : 'default'}" color="black" style="font-size: calc(var(--type-ramp-minus-2-font-size) - 2px); color: black;"> ${fileType}</${badgeTag}>`;
};

export function DOCUMENT_COLUMNS(context: DocumentManager) {
  return [
    {
      headerName: 'Type',
      suppressMenu: true,
      width: 90,
      cellStyle: () => ({
        display: 'flex',
        alignItems: 'center',
      }),
      cellRenderer: customFileTypeRenderer,
      cellRendererParams: (params) => {
        const hyperlinkParams = {
          data: params.data,
        };
        return hyperlinkParams;
      },
    },

    {
      field: COLUMN_FIELDS.FILE_NAME,
      headerName: 'Name',
      suppressMenu: true,
      flex: 3,
    },
    {
      field: COLUMN_FIELDS.CREATED_BY,
      headerName: 'Owner',
      suppressMenu: true,
      flex: 2,
    },
    {
      field: COLUMN_FIELDS.MODIFIED_AT,
      headerName: 'Last Modified',
      suppressMenu: true,
      flex: 2,
      valueFormatter: (props) => {
        return formatDateLong(props?.data?.MODIFIED_AT);
      },
    },
    {
      field: COLUMN_FIELDS.FILE_SIZE,
      headerName: 'Size',
      suppressMenu: true,
      flex: 2,
      valueFormatter: (props) => {
        return formatBytes(props?.data?.FILE_SIZE, 0);
      },
    },
    {
      headerName: '',
      suppressMenu: true,
      flex: 1,
      resizable: false,
      sortable: false,
      cellStyle: () => ({
        display: 'flex',
        alignItems: 'center',
      }),
      cellRenderer: ActionCellRenderer,
      cellRendererParams: (params) => {
        const downloadBtn = {
          name: downloadIcon,
          callback: (detail) => {
            context.downloadDocument(detail.FILE_STORAGE_ID);
          },
          actionName: 'Download',
        };
        const actions = [downloadBtn];
        const enabled = true;
        return {
          actions,
          enabled,
        };
      },
    },
  ];
}
