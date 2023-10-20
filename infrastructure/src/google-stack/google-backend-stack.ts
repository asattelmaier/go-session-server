import { GcsBackend } from 'cdktf';
import { Construct } from 'constructs';
import { GoogleStack } from './google-stack';
import { TerraformStateStorage } from '../resources/terraform-state-storage';

export class GoogleBackendStack extends GoogleStack {
  constructor(scope: Construct, id: string) {
    super(scope, id);

    new GcsBackend(this, {
      bucket: TerraformStateStorage.NAME,
      prefix: `terraform/state/${id}`,
    });
  }
}
