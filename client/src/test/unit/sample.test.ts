import { createLogicSuite } from '@genesislcap/foundation-testing';
import { getEndpointUrl } from '../../utils/endpoint';

const Suite = createLogicSuite('getEndpointURL');
Suite('getendPointURL should provide expected results', ({ runCases }) => {
  runCases(getEndpointUrl, [
    [['file-server/upload/', 'ws://localhost/gwf'], 'http://localhost/gwf/file-server/upload/'],
    [['file-server/upload/', 'ws://localhost:9064'], 'http://localhost:9064/file-server/upload/'],
    [['file-server/upload/', 'ws://localhost/gwf/'], 'http://localhost/gwf/file-server/upload/'],
    [['file-server/upload/', 'wss://dev-pbc-sandbox2.cddev.genesis.global/gwf'], 'https://dev-pbc-sandbox2.cddev.genesis.global/gwf/file-server/upload/'],
    [['file-server/upload/', 'wss://dev-pbc-sandbox2.cddev.genesis.global/gwf/'], 'https://dev-pbc-sandbox2.cddev.genesis.global/gwf/file-server/upload/'],
  ]);
});

Suite.run();