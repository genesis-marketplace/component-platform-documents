const { resolve } = require('node:path');
const { addUIDependency, addServerDependency, runPreRequisiteChecks } = require('@genesislcap/seed-utils');
const checks = require('./checks');
const versions = require('./versions.json');
const packageJson = require('./package.json');

module.exports = async (data, utils) => {
  const { editJSONFile } = utils;
  const json = editJSONFile(packageJson);
  data.pbcVersion = json.get('version');
  data.date = Date();
  data.utils = utils;
  data.versions = versions;
  /**
   * Run checks on project
   */
  runPreRequisiteChecks(data, checks);

  addUIDependency(data, '@genesislcap/pbc-documents-ui', versions.dependencies.pbcDocuments);

  addServerDependency(data, {name: 'file-server'}, versions.dependencies.serverDepId);
};
