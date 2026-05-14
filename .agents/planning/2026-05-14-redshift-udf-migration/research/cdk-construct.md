# Research: RedshiftAmznIdMaskCDKConstructs

## Overview

BDT provides a paved-road CDK construct (`@amzn/redshift-amzn-id-mask-cdk-constructs`) that automates provisioning of Lambda UDFs to replace deprecated Python Redshift UDFs.

## What Gets Deployed

- Up to 6 Lambda Functions (Rust-based, x86_64 default):
  - EncryptId / DecryptId (customer IDs)
  - EncryptShipmentId / DecryptShipmentId
  - EncryptRmaId / DecryptRmaId
- IAM Role: `RedshiftLambdaInvokeRole` for Redshift → Lambda invocation
- CloudWatch: Log groups, dashboards, alarms (when monitoring enabled)
- CloudFormation Outputs: SQL commands and setup instructions

## CDK Construct Usage

```typescript
import { RedshiftAmznIdMask, ApplicationLogLevel } from '@amzn/redshift-amzn-id-mask-cdk-constructs';
import { Vpc, SecurityGroup } from 'aws-cdk-lib/aws-ec2';

new RedshiftAmznIdMask(this, 'Cluster01', {
  cluster: 'address-service-prod',
  clusterType: 'provisioned',
  databaseName: '<database_name>',
  vpc: Vpc.fromVpcAttributes(this, 'Vpc', {
    vpcId: '<vpc-id>',
    availabilityZones: ['us-east-1a', 'us-east-1b'],
    privateSubnetIds: ['subnet-xxx', 'subnet-yyy'],
  }),
  securityGroups: [SecurityGroup.fromSecurityGroupId(this, 'SG', '<sg-id>')],
  logLevel: ApplicationLogLevel.INFO,
});
```

## Key Configuration Options

| Property | Default | Notes |
|---|---|---|
| clusterType | 'provisioned' | or 'serverless' |
| deployInVpc | true | Recommended; false only for public subnet clusters |
| enableIdFunctions | true | encrypt_id / decrypt_id |
| enableShipmentIdFunctions | true | Can disable if not needed |
| enableRmaIdFunctions | true | Can disable if not needed |
| logRetention | ONE_DAY | CloudWatch log retention |
| lambdaFunctionMemorySize | 512 | MB |
| lambdaFunctionTimeout | 30s | |
| lambdaArchitecture | X86_64 | Optional ARM64/Graviton |

## For AddressService

Since the policy engine shows only `decrypt_id` is used, we could potentially set:
- `enableShipmentIdFunctions: false`
- `enableRmaIdFunctions: false`

This would deploy only 2 Lambda functions instead of 6. However, deploying all 6 is low-cost and provides future flexibility.

## Migration Steps (from README)

1. Deploy CDK construct (provisions Lambda functions + IAM role)
2. Attach IAM role to Redshift cluster (`aws redshift modify-cluster-iam-roles --add-iam-roles`)
3. Create external functions in Redshift using SQL from CloudFormation outputs
4. **Rename** (not drop) existing Python UDFs: `ALTER FUNCTION ... RENAME TO ..._old`
5. Validate new functions work correctly
6. Rollback path: drop new Lambda UDFs, rename old functions back (only until June 30, 2026)

## Performance

- Lambda UDFs significantly outperform Python UDFs
- Rust implementation with near-instant cold starts (~10ms)
- Benchmark: 500M row encrypt_id — Python: >3.5 hours (didn't finish); Rust Lambda: 5.5 minutes
- Throughput: 400K-800K+ records/sec depending on batch size

## Function Signatures (New Lambda UDFs)

```sql
amzn_id_mask.encrypt_id(typetag VARCHAR, id BIGINT) → VARCHAR
amzn_id_mask.decrypt_id(encrypted_id VARCHAR) → BIGINT
amzn_id_mask.decrypt_id_safe(encrypted_id VARCHAR) → BIGINT  -- returns NULL on invalid input
```

## Important Notes

- Python UDFs cannot be recreated after being dropped (Redshift blocks new Python UDF creation)
- Always RENAME, never DROP existing Python UDFs
- The `decrypt_id_safe` function is an alias for `decrypt_id` for backwards compatibility
- NUMERIC(38,0) overloads are optional and only needed if tables have NUMERIC-typed ID columns

## References

- [Package](https://code.amazon.com/packages/RedshiftAmznIdMaskCDKConstructs)
- [README / Quick Start](https://code.amazon.com/packages/RedshiftAmznIdMaskCDKConstructs/blobs/mainline/--/README.md)
- [Kiro Setup Context](https://code.amazon.com/packages/RedshiftAmznIdMaskCDKConstructs/blobs/mainline/--/docs/redshiftamznidmask-cdk-setup-context.md)
- [Slack: #redshift-amzn-id-mask-deprecation-campaign](https://amazon.enterprise.slack.com/archives/C0ATE2E2JJ1)
