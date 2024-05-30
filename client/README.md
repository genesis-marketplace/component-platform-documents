# Client (Web)

:hammer_and_wrench: **Development**
- Do the [npm link](https://docs.npmjs.com/cli/v10/commands/npm-link) between the projects:
  - On genesis-file-server: ```npm i``` and ```npm link```
  - On client app: ```npm i``` and ```npm link @genesislcap/pbc-documents-ui```
- On [package.json](package.json) change the following lines:
  - ```"main": "dist/esm/index.js",``` to ```"main": "src/index.ts",```
  - ```"default": "./dist/esm/index.js"``` to ```"default": "./src/index.ts"```
  - **[OPTIONAL]** if you want to use configure method to use a different design system, rather than zero as default, then change the following line :
    ```"default": "./dist/esm/config/index.js"``` to ```"default": "./src/config/index.ts"```
- **[OPTIONAL]** On client app (blank-seed, position-seed, etc.), create a file in ```/src``` with the name ```global.d.ts``` with this line inside ```declare module '@genesislcap/pbc-documents-ui';```
- :warning: Always do a ```git rollback``` on the ```package.json``` before a commit or just don't commit it

:package: **Build**
- The recommended build command to be used is ```genx build -b ts```
- Undo the following lines on [package.json](package.json):
  - ```"main": "src/index.ts",``` to ```"main": "dist/esm/index.js",```
  - ```"default": "./src/index.ts"``` to ```"default": "./dist/esm/index.js"```
- Do a ```npm run build``` on genesis-file-server
- Do a ```npm run build``` on client app
- A ```dist``` folder will be generated

## License

Note: this project provides front end dependencies and uses licensed components listed in the next section, thus licenses for those components are required during development. Contact [Genesis Global](https://genesis.global/contact-us/) for more details.

### Licensed components
Genesis low-code platform