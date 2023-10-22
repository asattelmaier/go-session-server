import { Fn, TerraformStack, TerraformVariable } from 'cdktf';
import { Construct } from 'constructs';
import { GoogleProvider } from '@cdktf/provider-google/lib/provider';

// TODO: Create npm library to share common stacks
export class GoogleStack extends TerraformStack {
  protected static readonly DEFAULT_LOCATION = 'europe-west1';
  // TODO: Move the repositoryId to a shared library,
  //  the GoogleStack should not be responsible for
  //  the container registry
  private static readonly CONTAINER_REGISTRY_REPOSITORY_ID: string = 'go-services';
  // TODO: Move the go game socket server name to a library
  protected static readonly GO_GAME_SOCKET_SERVER_NAME: string = 'go-game-socket-server';
  protected readonly project: TerraformVariable = new TerraformVariable(this, 'google-project', {
    type: 'string',
    description: 'Google Cloud Console project'
  });
  private readonly base64EncodedCredentials: TerraformVariable = new TerraformVariable(this, 'base64-encoded-google-credentials', {
    type: 'string',
    sensitive: true,
    description: 'Base64 encoded Google Cloud credentials'
  });

  constructor(scope: Construct, id: string) {
    super(scope, id);

    new GoogleProvider(this, 'google-provider', {
      project: this.project.stringValue,
      credentials: Fn.base64decode(this.base64EncodedCredentials.stringValue)
    });
  }

  protected getContainerRegistryRepositoryName(): string {
    return `${GoogleStack.DEFAULT_LOCATION}-docker.pkg.dev/${this.project.stringValue}/${GoogleStack.CONTAINER_REGISTRY_REPOSITORY_ID}/`
  }
}