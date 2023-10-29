import { Construct } from 'constructs';
import { StorageBucket } from '@cdktf/provider-google/lib/storage-bucket';
import { GoogleStack } from '../google-stack/google-stack';
import { GoogleBackendStack } from '../google-stack/google-backend-stack';

export class TerraformStateStorage extends GoogleStack {
  private static readonly ID = 'terraform-state-storage';

  constructor(scope: Construct) {
    super(scope, TerraformStateStorage.ID);

    new StorageBucket(this, TerraformStateStorage.ID, {
      name: GoogleBackendStack.TERRAFORM_STATE_STORAGE_NAME,
      forceDestroy: false,
      location: GoogleStack.DEFAULT_LOCATION,
      storageClass: 'STANDARD',
      publicAccessPrevention: 'enforced',
      versioning: {
        enabled: true,
      },
    });
  }
}