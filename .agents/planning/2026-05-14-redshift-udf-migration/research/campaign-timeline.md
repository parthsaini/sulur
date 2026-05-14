# Research: Campaign & Timeline

## Campaign Details

| Field | Value |
|---|---|
| Risk Level | Medium (escalates to High on June 15, 2026) |
| Ticket Severity | Sev3 (escalates to Sev2 on June 15, 2026) |
| SLA | 60 days |
| Scope | SDO-wide |
| Automatic Remediation | No |
| Deadline | June 30, 2026 |

## Timeline

- **Now (May 2026)**: Medium risk, Sev3
- **June 15, 2026**: Escalates to Sev2 — must be resolved before this
- **June 30, 2026**: Python UDF execution suspended — queries will FAIL

## Detection Criteria

Teams are flagged based on Redshift Python UDF execution logs for the trailing 30 days. Once the old Python UDFs are renamed and no longer executed, the PE risk auto-resolves after 30 days of no usage.

## Estimated Effort

- **Teams already using CDK**: 4-8 hours per cluster
- **Teams new to CDK**: Additional 4-8 hours for CDK onboarding
- Includes: VPC discovery, CDK configuration, deployment, SQL function swap, validation

## Rollback Strategy

1. Rename (not drop) existing Python UDFs before creating Lambda UDFs
2. If issues arise: drop new Lambda UDFs, rename old functions back
3. Rollback only available until June 30, 2026 (after that, Python UDFs won't execute regardless)

## How to Resolve PE Risk

- Rename existing Python UDFs so they no longer match detection criteria
- Stop executing them
- After 30 days of no usage, PE risk auto-resolves

## FAQ Highlights

- No exceptions allowed — this is a Redshift platform decision
- Existing SQL queries don't need to change if schema/naming matches (amzn_id_mask.function_name)
- Can migrate one cluster at a time
- Lambda UDFs outperform Python UDFs significantly

## References

- [Campaign Wiki](https://w.amazon.com/bin/view/BDT/Campaigns/Migrate_RedshiftAmznIdMask_Python_UDFs_to_Lambda_UDFs/)
- [AWS Announcement](https://aws.amazon.com/blogs/big-data/amazon-redshift-python-user-defined-functions-will-reach-end-of-support-after-june-30-2026/)
- [Slack Support](https://amazon.enterprise.slack.com/archives/C0ATE2E2JJ1)
