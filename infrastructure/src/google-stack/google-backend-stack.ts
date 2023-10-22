import { GcsBackend } from 'cdktf';
import { Construct } from 'constructs';
import { GoogleStack } from './google-stack';

export class GoogleBackendStack extends GoogleStack {
  // TODO: Move the terraform state storage name to a library
  public static readonly TERRAFORM_STATE_STORAGE_NAME = 'go-infrastructure-terraform-state-storage';

  constructor(scope: Construct, id: string) {
    super(scope, id);

    new GcsBackend(this, {
      bucket: GoogleBackendStack.TERRAFORM_STATE_STORAGE_NAME,
      prefix: `terraform/state/${id}`,
    });
  }
}
