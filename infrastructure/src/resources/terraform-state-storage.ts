import { Construct } from 'constructs';
import { StorageBucket } from '@cdktf/provider-google/lib/storage-bucket';
import { GoogleStack } from '../google-stack/google-stack';

export class TerraformStateStorage extends GoogleStack {
  public static readonly NAME = 'go-infrastructure-terraform-state-storage';
  private static readonly ID = 'terraform-state-storage';

  constructor(scope: Construct) {
    super(scope, TerraformStateStorage.ID);

    new StorageBucket(this, 'terraform-state-storage', {
      name: TerraformStateStorage.NAME,
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