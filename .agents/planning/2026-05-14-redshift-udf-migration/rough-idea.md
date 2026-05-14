# Rough Idea

Migrate the `decrypt_id` Python Redshift UDF to a Lambda UDF for the AddressService team's `address-service-prod` cluster in `us-east-1` (AWS account: AddressService-Tier2-NA, 420331918018).

## Context

- Amazon Redshift has announced end of support for Scalar Python UDFs effective **June 30, 2026**
- BDT (Business Data Technologies) has built [RedshiftAmznIdMaskCDKConstructs](https://code.amazon.com/packages/RedshiftAmznIdMaskCDKConstructs) — a paved-road CDK construct that automates provisioning of replacement Lambda UDFs, IAM roles, and permissions
- The policy risk is tracked at: https://policyengine.amazon.com/entity/6386943?campaign=pythonRedshiftAmznIdMaskUsage

## Impacted Resources

| Field | Value |
|---|---|
| AWS Account | AddressService-Tier2-NA (420331918018) |
| Region | us-east-1 |
| Bindle | AddressService |
| Cluster | address-service-prod |
| Function | decrypt_id |
| Cluster Count | 1 |
| Function Count | 1 |

## Recommended Approach (from BDT)

1. Identify impacted Redshift clusters and UDFs
2. Integrate [RedshiftAmznIdMaskCDKConstructs](https://code.amazon.com/packages/RedshiftAmznIdMaskCDKConstructs) in CDK stack
3. Swap UDFs during a low-utilization maintenance window
4. Validate migration by confirming queries execute successfully

## References

- [Quick Start Guide](https://code.amazon.com/packages/RedshiftAmznIdMaskCDKConstructs/blobs/mainline/--/README.md)
- [Migration Guide](https://code.amazon.com/packages/RedshiftAmznIdMaskCDKConstructs/blobs/mainline/--/README.md#migration-guide)
- [Campaign Wiki](https://w.amazon.com/bin/view/BDT/Campaigns/Migrate_RedshiftAmznIdMask_Python_UDFs_to_Lambda_UDFs/)
- [PythonRedshiftAmznIdMask Wiki](https://w.amazon.com/bin/view/PythonRedshiftAmznIdMask/)
- [Slack: #redshift-amzn-id-mask-deprecation-campaign](https://amazon.enterprise.slack.com/archives/C0ATE2E2JJ1)
