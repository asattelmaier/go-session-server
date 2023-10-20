import { Fn, TerraformStack, TerraformVariable } from 'cdktf';
import { Construct } from 'constructs';
import { GoogleProvider } from '@cdktf/provider-google/lib/provider';

export class GoogleStack extends TerraformStack {
  protected static readonly DEFAULT_LOCATION = 'europe-west1';

  constructor(scope: Construct, id: string) {
    super(scope, id);

    const { stringValue: project } = new TerraformVariable(this, 'google-project', {
      type: 'string',
      description: 'Google Cloud Console project'
    });
    const { stringValue: base64EncodedCredentials } = new TerraformVariable(this, 'base64-encoded-google-credentials', {
      type: 'string',
      sensitive: true,
      description: 'Base64 encoded Google Cloud credentials'
    });

    new GoogleProvider(this, 'google-provider', { project, credentials: Fn.base64decode(base64EncodedCredentials) });
  }
}
