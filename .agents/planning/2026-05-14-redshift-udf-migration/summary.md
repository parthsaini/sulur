# Project Summary: Migrate RedshiftAmznIdMask Python UDFs to Lambda UDFs

## Directory Structure

```
.agents/planning/2026-05-14-redshift-udf-migration/
├── rough-idea.md                          # Initial concept from Policy Engine risk
├── idea-honing.md                         # Requirements clarification (skipped — self-sufficient)
├── research/
│   ├── cdk-construct.md                   # RedshiftAmznIdMaskCDKConstructs analysis
│   ├── existing-python-udfs.md            # Legacy Python UDF documentation
│   └── campaign-timeline.md              # Deadlines, SLA, rollback strategy
├── design/
│   └── detailed-design.md                 # Architecture, components, error handling, testing
├── implementation/
│   └── plan.md                            # 9-step implementation plan with checklist
└── summary.md                             # This document
```

## Key Design Decisions

| Decision | Rationale |
|---|---|
| Only enable ID functions (2 Lambdas) | Only `decrypt_id` is used; minimizes resource footprint |
| VPC deployment | Cluster is in private subnet; required for security |
| Rename (not drop) old UDFs | Preserves rollback path until June 30 deadline |
| Construct ID: `AddrSvcProd` | Short, descriptive, within 27-char limit |

## Implementation Overview (9 Steps)

1. **Environment Discovery** — VPC, subnets, security groups, database
2. **CDK Integration** — Add construct to AddressService stack
3. **Deploy Stack** — Provision Lambda functions + IAM role
4. **Attach IAM Role** — Enable Redshift → Lambda invocation
5. **Create External Functions** — SQL from CloudFormation outputs
6. **Validate** — Functional tests, edge cases, performance
7. **Cutover** — Rename old Python UDFs during maintenance window
8. **Monitor** — 24-48 hours of CloudWatch observation
9. **Resolve PE Risk** — Acknowledge in Policy Engine

## Timeline

- **Estimated effort**: 4-8 hours (team already uses CDK)
- **Must complete by**: June 15, 2026 (avoid Sev2 escalation)
- **Hard deadline**: June 30, 2026 (Python UDFs stop working)

## Next Steps

1. Add project files to Kiro context: `/context add .agents/planning/2026-05-14-redshift-udf-migration/**/*.md`
2. Begin Step 1: Run environment discovery commands against `address-service-prod` cluster
3. Identify the AddressService CDK stack where the construct should be added
4. Schedule a maintenance window for the cutover (Step 7)

## Risks & Mitigations

| Risk | Mitigation |
|---|---|
| VPC connectivity issues | Validate NAT Gateway + security groups in Step 1 |
| Query failures after cutover | Rollback procedure documented in Step 7 |
| Missing deadline | Start immediately; total effort is 4-8 hours |
| Lambda throttling | Default 1000 concurrency is shared; monitor in Step 8 |
