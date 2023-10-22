# Go Infrastructure

Provides the infrastructure setup for various components of the Go project. It leverages Terraform and Google Cloud
Platform (GCP) to create and manage the required resources.

## Requirements

- [Terraform](https://developer.hashicorp.com/terraform/tutorials/aws-get-started/install-cli)
- [gcloud](https://cloud.google.com/sdk/docs/install)
- [Node.js](https://nodejs.org/en/download)

## Setup gcloud

Before you proceed with deploying the infrastructure, you need to authenticate with Google Cloud Platform and set your
project. Run the following commands:

```bash
gcloud auth application-default login
gcloud config set project VALUE
```

## Terraform State Storage

If the Terraform state storage doesn't already exist, you need to create it initially before deploying other Terraform
resources. The state storage is responsible for storing the Terraform state of all other resources, so it's crucial to
set it up first:

```bash
yarn build
STACK=terraform-state-storage yarn deploy 
```

After setting up the Terraform state storage, you can proceed to deploy other stacks.

```bash
STACK=go-session-server yarn deploy -var-file=$(pwd)/.tfvars
```
