import { App } from 'cdktf';
import { ContainerRegistry } from './resources/container-registry';
import { TerraformStateStorage } from './resources/terraform-state-storage';
import { GoSessionServer } from './services/go-session-server';

const app = new App();

// TODO: Move base infrastructure to an dedicated repository
new TerraformStateStorage(app);
new ContainerRegistry(app);
new GoSessionServer(app);

app.synth();
