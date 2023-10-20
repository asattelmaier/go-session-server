import { App } from 'cdktf';
import { ContainerRegistry } from './src/resources/container-registry';
import { TerraformStateStorage } from './src/resources/terraform-state-storage';

const app = new App();

new TerraformStateStorage(app);
new ContainerRegistry(app);

app.synth();
