export const getEndpointUrl = (
  endpoint: string,
  urlRoot: string = sessionStorage.getItem('hostUrl'),
): string => {
  const url = new URL(urlRoot);
  const path =
    url.pathname && url.pathname[url.pathname.length - 1] == '/'
      ? url.pathname
      : `${url.pathname}/`;
  if (urlRoot.includes('localhost')) {
    return `http://${url.host}${path}${endpoint}`;
  } else {
    return `https://${url.host}${path}${endpoint}`;
  }
};
