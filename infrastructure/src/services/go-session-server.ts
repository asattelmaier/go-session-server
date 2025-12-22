import { Construct } from 'constructs';
import { CloudRunService, CloudRunServiceTemplateSpecContainers } from '@cdktf/provider-google/lib/cloud-run-service';
import { GoogleBackendStack } from '../google-stack/google-backend-stack';
import { TerraformVariable } from 'cdktf';
import { CloudRunServiceIamBinding } from '@cdktf/provider-google/lib/cloud-run-service-iam-binding';
import { FirestoreDatabase } from '@cdktf/provider-google/lib/firestore-database';

export class GoSessionServer extends GoogleBackendStack {
  private static readonly ID = 'go-session-server';
  // TODO: the database id is configurable since
  //  `spring-cloud-gcp-starter-firestore` version `3.7.3`:
  //  https://github.com/GoogleCloudPlatform/spring-cloud-gcp/pull/2164
  //  Unfortunately, it is not possible to update to this version,
  //  otherwise the Firestore emulator will no longer work as expected:
  //  https://github.com/GoogleCloudPlatform/spring-cloud-gcp/issues/2286
  private static readonly DATABASE_ID = '(default)';
  private static readonly FIRESTORE_TYPE = 'FIRESTORE_NATIVE';
  private static readonly FIRESTORE_DEFAULT_HOST_PORT = 'firestore.googleapis.com:443';
  // The location is different from the default location because Firestore is not available there
  private static readonly FIRESTORE_LOCATION = 'europe-west3';
  private static readonly CONTAINER_PORT = 8080;
  private static readonly CONTAINER_CONCURRENCY = 80;
  private static readonly MAX_INSTANCES = '1';
  private static readonly MIN_INSTANCES = '0';
  private readonly gitHash: TerraformVariable = new TerraformVariable(this, 'git-hash', {
    type: 'string',
    description: 'Current Git Hash'
  });
  private readonly guestPassword: TerraformVariable = new TerraformVariable(this, 'guest-password', {
    sensitive: true,
    type: 'string',
    description: 'Guest user password'
  });
  private readonly jwtAccessTokenExpiration: TerraformVariable = new TerraformVariable(this, 'jwt-access-token-expiration', {
    sensitive: true,
    type: 'string',
    description: 'JWT access token expiration in milliseconds'
  });
  private readonly jwtRefreshTokenExpiration: TerraformVariable = new TerraformVariable(this, 'jwt-refresh-token-expiration', {
    sensitive: true,
    type: 'string',
    description: 'JWT refresh token expiration in milliseconds'
  });
  private readonly jwtSecretKey: TerraformVariable = new TerraformVariable(this, 'jwt-secret-key', {
    sensitive: true,
    type: 'string',
    description: 'JWT secret key'
  });

  constructor(scope: Construct) {
    super(scope, GoSessionServer.ID);

    const database = new FirestoreDatabase(this, `${GoSessionServer.ID}-database`, {
      name: GoSessionServer.DATABASE_ID,
      type: GoSessionServer.FIRESTORE_TYPE,
      locationId: GoSessionServer.FIRESTORE_LOCATION,
    });

    const goSessionServer = new CloudRunService(this, GoSessionServer.ID, {
      dependsOn: [database],
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
          containers: this.getContainers(),
        },
      },
    });

    new CloudRunServiceIamBinding(this, 'allow-public-access', {
      dependsOn: [goSessionServer],
      location: GoSessionServer.DEFAULT_LOCATION,
      service: goSessionServer.name,
      role: 'roles/run.invoker',
      members: ['allUsers'],
    });
  }

  private getContainers(): CloudRunServiceTemplateSpecContainers[] {
    const image = `${this.getContainerRegistryRepositoryName()}${GoSessionServer.ID}:${this.gitHash.stringValue}`;

    return [
      {
        image,
        ports: [{ containerPort: GoSessionServer.CONTAINER_PORT }],
        env: [
          { name: 'GNUGO_HOST', value: 'localhost' },
          { name: 'GNUGO_PORT', value: '8001' },
          { name: 'FIRESTORE_EMULATOR_ENABLED', value: 'false' },
          { name: 'FIRESTORE_EMULATOR_HOST_PORT', value: GoSessionServer.FIRESTORE_DEFAULT_HOST_PORT },
          { name: 'FIRESTORE_EMULATOR_PROJECT_ID', value: this.project.stringValue },
          { name: 'SECURITY_GUEST_PASSWORD', value: this.guestPassword.stringValue },
          { name: 'SECURITY_JWT_ACCESS_TOKEN_EXPIRATION', value: this.jwtAccessTokenExpiration.stringValue },
          { name: 'SECURITY_JWT_REFRESH_TOKEN_EXPIRATION', value: this.jwtRefreshTokenExpiration.stringValue },
          { name: 'SECURITY_JWT_SECRET_KEY', value: this.jwtSecretKey.stringValue },

        ]
      },
      {
        image: `${this.getContainerRegistryRepositoryName()}gnugo:${this.gitHash.stringValue}`,
      }
    ];
  }
}