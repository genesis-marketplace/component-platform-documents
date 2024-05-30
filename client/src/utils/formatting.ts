/**
 * Number formatter, can be used directly on ag-grid columns as valueFormatter.
 *
 * @param minDP min decimal points
 * @param maxDP max decimal points
 */
export function formatNumber(minDP = 2, maxDP = minDP) {
  return (params) => {
    if (!(params && typeof params.value === 'number')) return '';
    const lang = (navigator && navigator.language) || 'en-GB';
    return Intl.NumberFormat(lang, {
      minimumFractionDigits: minDP,
      maximumFractionDigits: maxDP,
    }).format(params.value);
  };
}

export const formatDateLong = (param: number): string => {
  if (!(param && typeof param === 'number' && param > 0)) return '';
  const date = new Date(param);

  const formattedDate = new Intl.DateTimeFormat(Intl.DateTimeFormat().resolvedOptions().locale, {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
  }).format(date);
  return formattedDate;
};

export const getFileType = (fileName: string): string => {
  const typeIndex = fileName.lastIndexOf('.');
  return fileName.substring(typeIndex + 1).toUpperCase();
};

export const getFileColor = (fileType: string): string => {
  switch (fileType) {
    case 'CSV':
      return '#63a103';
    case 'JPG':
    case 'PNG':
      return '#8a98f5';
    case 'PDF':
      return '#f9644d';
    default:
      return '#ffffff';
  }
};

export const formatBytes = (bytes, decimals = 2): string => {
  if (!+bytes) return '0 Bytes';

  const k = 1000;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];

  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
};
