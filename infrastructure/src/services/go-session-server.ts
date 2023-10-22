import { Construct } from 'constructs';
import { CloudRunService, CloudRunServiceTemplateSpecContainers } from '@cdktf/provider-google/lib/cloud-run-service';
import { GoogleBackendStack } from '../google-stack/google-backend-stack';
import { Fn, TerraformVariable } from 'cdktf';
import { DataGoogleCloudRunService } from '@cdktf/provider-google/lib/data-google-cloud-run-service';
import { CloudRunServiceIamBinding } from '@cdktf/provider-google/lib/cloud-run-service-iam-binding';

export class GoSessionServer extends GoogleBackendStack {
  private static readonly ID = 'go-session-server';
  private static readonly CONTAINER_PORT = 8080;
  private static readonly CONTAINER_CONCURRENCY = 80;
  private static readonly MAX_INSTANCES = '1';
  private static readonly MIN_INSTANCES = '0';
  private readonly gitHash: TerraformVariable = new TerraformVariable(this, 'git-hash', {
    type: 'string',
    description: 'Current Git Hash'
  });

  constructor(scope: Construct) {
    super(scope, GoSessionServer.ID);

    const goSessionServer = new CloudRunService(this, GoSessionServer.ID, {
      location: GoSessionServer.DEFAULT_LOCATION,
      name: GoSessionServer.ID,
      template: {
        metadata: {
          annotations: {
            'autoscaling.knative.dev/maxScale': GoSessionServer.MAX_INSTANCES,
            'autoscaling.knative.dev/minScale': GoSessionServer.MIN_INSTANCES,
          }
        },
        spec: {
          containerConcurrency: GoSessionServer.CONTAINER_CONCURRENCY,
          containers: [this.getContainerTemplate()],
        },
      },
    });

    new CloudRunServiceIamBinding(this, 'allow-public-access', {
      location: GoSessionServer.DEFAULT_LOCATION,
      service: goSessionServer.name,
      role: 'roles/run.invoker',
      members: ['allUsers'],
    });
  }

  private getContainerTemplate(): CloudRunServiceTemplateSpecContainers {
    const image = `${this.getContainerRegistryRepositoryName()}${GoSessionServer.ID}:${this.gitHash.stringValue}`;
    const gameClientSocketHost = new DataGoogleCloudRunService(this, 'game-client-socket-host', {
      location: GoSessionServer.DEFAULT_LOCATION,
      name: GoSessionServer.GO_GAME_SOCKET_SERVER_NAME,
    });
    // TODO: There is currently no simple way to get the host:
    //  - https://github.com/hashicorp/terraform-provider-google/issues/9277
    //  - https://github.com/hashicorp/terraform/issues/23893
    const host = Fn.one(Fn.regex('^(?:https?://)?([^:/?]+)', Fn.tostring(gameClientSocketHost.status.get(0).url)));

    return {
      image,
      ports: [{ containerPort: GoSessionServer.CONTAINER_PORT }],
      env: [
        { name: 'GAME_CLIENT_SOCKET_HOST', value: host },
        // TODO: Investigate why https port not works as expected
        { name: 'GAME_CLIENT_SOCKET_PORT', value: '80' },
      ]
    };
  }
}