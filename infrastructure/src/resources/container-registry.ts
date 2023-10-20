import { Construct } from 'constructs';
import { ArtifactRegistryRepository } from '@cdktf/provider-google/lib/artifact-registry-repository';
import { GoogleBackendStack } from '../google-stack/google-backend-stack';

export class ContainerRegistry extends GoogleBackendStack {
  private static readonly ID = 'container-registry';

  constructor(scope: Construct) {
    super(scope, ContainerRegistry.ID);

    new ArtifactRegistryRepository(this, 'container-registry', {
      format: 'DOCKER',
      repositoryId: 'go-services',
      location: GoogleBackendStack.DEFAULT_LOCATION,
    });
  }
}
