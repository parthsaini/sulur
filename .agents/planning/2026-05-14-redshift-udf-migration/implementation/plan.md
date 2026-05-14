# Implementation Plan: Migrate RedshiftAmznIdMask Python UDFs to Lambda UDFs

## Checklist

- [ ] Step 1: Environment Discovery & Validation
- [ ] Step 2: CDK Integration — Add Construct to AddressService Stack
- [ ] Step 3: Deploy CDK Stack
- [ ] Step 4: Attach IAM Role to Redshift Cluster
- [ ] Step 5: Create External Functions in Redshift
- [ ] Step 6: Validate New Lambda UDFs
- [ ] Step 7: Migration Cutover — Rename Old Python UDFs
- [ ] Step 8: Post-Migration Validation & Monitoring
- [ ] Step 9: Resolve Policy Engine Risk

---

## Step 1: Environment Discovery & Validation

**Objective**: Discover the VPC configuration, subnets, security groups, and database for the `address-service-prod` cluster to generate accurate CDK construct parameters.

**Implementation Guidance**:

1. Validate AWS access to account `420331918018` (AddressService-Tier2-NA):
   ```bash
   aws sts get-caller-identity --profile <profile> --region us-east-1
   ```

2. Discover cluster VPC details:
   ```bash
   aws redshift describe-clusters --cluster-identifier address-service-prod --region us-east-1 \
     --query 'Clusters[0].{VpcId:VpcId,EnhancedVpcRouting:EnhancedVpcRouting,SecurityGroups:VpcSecurityGroups[].VpcSecurityGroupId,DBName:DBName}'
   ```

3. Discover private subnets in the VPC:
   ```bash
   aws ec2 describe-subnets --filters "Name=vpc-id,Values=<vpc_id>" \
     --query 'Subnets[?MapPublicIpOnLaunch==`false`].{SubnetId:SubnetId,AZ:AvailabilityZone}' --output table
   ```

4. Verify NAT Gateway exists (required for Lambda in VPC to invoke external services):
   ```bash
   aws ec2 describe-nat-gateways --filter "Name=vpc-id,Values=<vpc_id>" \
     --query 'NatGateways[?State==`available`].NatGatewayId'
   ```

5. Discover databases on the cluster:
   ```bash
   aws redshift-data execute-statement --cluster-identifier address-service-prod --database dev \
     --sql "SELECT datname FROM pg_database WHERE datistemplate = false AND datname NOT IN ('padb_harvest','awsdatacatalog','andes','sys:internal','template0','template1','postgres','rdsdb');" \
     --region us-east-1 --query 'Id' --output text
   ```

6. Check for existing Python UDFs:
   ```bash
   aws redshift-data execute-statement --cluster-identifier address-service-prod --database <db> \
     --sql "SELECT n.nspname, p.proname, l.lanname FROM pg_proc p JOIN pg_language l ON p.prolang = l.oid JOIN pg_namespace n ON p.pronamespace = n.oid WHERE l.lanname = 'plpythonu' AND p.proname IN ('encrypt_id','decrypt_id','decrypt_id_safe','encrypt_shipment_id','decrypt_shipment_id','encrypt_rma_id','decrypt_rma_id');" \
     --region us-east-1 --query 'Id' --output text
   ```

**Test Requirements**:
- AWS credentials work and return expected account ID
- Cluster is in `available` status
- At least one private subnet with NAT Gateway exists
- Existing Python UDFs are confirmed present

**Integration**: Outputs (VPC ID, subnet IDs, AZs, security group ID, database name) feed directly into Step 2.

**Demo**: A document with all discovered infrastructure values ready for CDK configuration.

---

## Step 2: CDK Integration — Add Construct to AddressService Stack

**Objective**: Add the `RedshiftAmznIdMask` CDK construct to the AddressService CDK stack using discovered infrastructure values.

**Implementation Guidance**:

1. Add the dependency to `package.json`:
   ```json
   "@amzn/redshift-amzn-id-mask-cdk-constructs": "^1.0.0"
   ```

2. Run dependency installation:
   ```bash
   npm install
   ```

3. Add the construct to the appropriate CDK stack file:
   ```typescript
   import { RedshiftAmznIdMask, ApplicationLogLevel } from '@amzn/redshift-amzn-id-mask-cdk-constructs';
   import { Vpc, SecurityGroup } from 'aws-cdk-lib/aws-ec2';

   new RedshiftAmznIdMask(this, 'AddrSvcProd', {
     cluster: 'address-service-prod',
     clusterType: 'provisioned',
     databaseName: '<discovered_database>',
     vpc: Vpc.fromVpcAttributes(this, 'IdMaskVpc', {
       vpcId: '<discovered_vpc_id>',
       availabilityZones: ['<discovered_az1>', '<discovered_az2>'],
       privateSubnetIds: ['<discovered_subnet1>', '<discovered_subnet2>'],
     }),
     securityGroups: [SecurityGroup.fromSecurityGroupId(this, 'IdMaskSG', '<discovered_sg_id>')],
     enableIdFunctions: true,
     enableShipmentIdFunctions: false,
     enableRmaIdFunctions: false,
     logLevel: ApplicationLogLevel.INFO,
   });
   ```

4. Synthesize to validate:
   ```bash
   cdk synth <StackName>
   ```

**Test Requirements**:
- `cdk synth` succeeds without errors
- Generated CloudFormation template contains expected resources (2 Lambda functions, IAM role, CloudWatch resources)
- Construct ID `AddrSvcProd` is within 27-character limit

**Integration**: Builds on Step 1 outputs. Produces a deployable CDK stack for Step 3.

**Demo**: `cdk synth` produces a valid CloudFormation template; `cdk diff` shows the new resources to be created.

---

## Step 3: Deploy CDK Stack

**Objective**: Deploy the CDK stack to provision Lambda functions, IAM role, and monitoring resources in the `us-east-1` region.

**Implementation Guidance**:

1. Deploy the stack:
   ```bash
   cdk deploy <StackName> --profile <profile> --region us-east-1
   ```
   Or trigger deployment through the AddressService pipeline if that's the standard process.

2. Verify deployment succeeded:
   ```bash
   aws cloudformation describe-stacks --stack-name <StackName> \
     --query 'Stacks[0].StackStatus' --region us-east-1
   ```

3. Extract outputs (IAM role ARN, SQL commands):
   ```bash
   aws cloudformation describe-stacks --stack-name <StackName> \
     --query 'Stacks[0].Outputs[*].[OutputKey,OutputValue]' --output table --region us-east-1
   ```

4. Verify Lambda functions exist:
   ```bash
   aws lambda get-function --function-name RedshiftAmznIdMask-AddrSvcProd-EncryptId --region us-east-1
   aws lambda get-function --function-name RedshiftAmznIdMask-AddrSvcProd-DecryptId --region us-east-1
   ```

**Test Requirements**:
- Stack status is `CREATE_COMPLETE` or `UPDATE_COMPLETE`
- Both Lambda functions exist and are in `Active` state
- IAM role exists with correct trust policy
- CloudFormation outputs contain SQL commands

**Integration**: Builds on Step 2. Outputs (role ARN, function ARNs) feed into Steps 4 and 5.

**Demo**: CloudFormation stack deployed successfully; Lambda functions visible in console; outputs contain SQL setup commands.

---

## Step 4: Attach IAM Role to Redshift Cluster

**Objective**: Attach the `RedshiftLambdaInvokeRole` to the `address-service-prod` cluster so Redshift can invoke the Lambda functions.

**Implementation Guidance**:

1. Get the role ARN from CloudFormation outputs:
   ```bash
   ROLE_ARN=$(aws cloudformation describe-stacks --stack-name <StackName> \
     --query 'Stacks[0].Outputs[?contains(OutputKey,`RoleArn`)].OutputValue' --output text --region us-east-1)
   ```

2. Attach role to cluster:
   ```bash
   aws redshift modify-cluster-iam-roles \
     --cluster-identifier address-service-prod \
     --add-iam-roles $ROLE_ARN \
     --region us-east-1
   ```

3. Wait for attachment and verify:
   ```bash
   aws redshift describe-clusters --cluster-identifier address-service-prod \
     --query 'Clusters[0].IamRoles[?IamRoleArn==`'$ROLE_ARN'`].{ARN:IamRoleArn,Status:ApplyStatus}' \
     --region us-east-1
   ```

**Test Requirements**:
- Role appears in cluster's IAM roles list
- `ApplyStatus` is `in-sync`
- Cluster remains in `available` status (role attachment doesn't cause downtime)

**Integration**: Builds on Step 3 output (role ARN). Required before Step 5 (function creation needs the role).

**Demo**: `describe-clusters` shows the new IAM role attached with `in-sync` status.

---

## Step 5: Create External Functions in Redshift

**Objective**: Execute the SQL commands from CloudFormation outputs to create the Lambda-backed external functions in Redshift.

**Implementation Guidance**:

1. Get SQL commands from CloudFormation outputs:
   ```bash
   aws cloudformation describe-stacks --stack-name <StackName> \
     --query 'Stacks[0].Outputs[?contains(OutputKey,`SQL`)].{Key:OutputKey,Value:OutputValue}' --region us-east-1
   ```

2. Create schema and grant language permissions:
   ```bash
   aws redshift-data execute-statement --cluster-identifier address-service-prod --database <db> \
     --sql "CREATE SCHEMA IF NOT EXISTS amzn_id_mask; GRANT USAGE ON LANGUAGE EXFUNC TO PUBLIC;" \
     --region us-east-1
   ```

3. Create each external function using the exact SQL from outputs:
   ```bash
   aws redshift-data execute-statement --cluster-identifier address-service-prod --database <db> \
     --sql "<CREATE EXTERNAL FUNCTION SQL from outputs>" \
     --region us-east-1
   ```

4. Grant execute permissions:
   ```bash
   aws redshift-data execute-statement --cluster-identifier address-service-prod --database <db> \
     --sql "GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA amzn_id_mask TO PUBLIC;" \
     --region us-east-1
   ```

5. Verify functions were created:
   ```bash
   aws redshift-data execute-statement --cluster-identifier address-service-prod --database <db> \
     --sql "SELECT n.nspname, p.proname, l.lanname FROM pg_proc p JOIN pg_language l ON p.prolang = l.oid JOIN pg_namespace n ON p.pronamespace = n.oid WHERE l.lanname = 'external' AND n.nspname = 'amzn_id_mask';" \
     --region us-east-1
   ```

**Test Requirements**:
- All CREATE EXTERNAL FUNCTION statements succeed
- Functions appear in `pg_proc` with language `external` in schema `amzn_id_mask`
- GRANT statements succeed

**Integration**: Builds on Steps 3 (SQL outputs) and 4 (IAM role attached). Required before Step 6 (validation).

**Demo**: Query `pg_proc` shows new external functions in `amzn_id_mask` schema alongside existing Python UDFs.

---

## Step 6: Validate New Lambda UDFs

**Objective**: Confirm the new Lambda-backed functions produce correct results, including side-by-side comparison with existing Python UDFs.

**Implementation Guidance**:

1. Basic functional tests:
   ```sql
   SELECT amzn_id_mask.encrypt_id('A', 1::BIGINT);
   -- Expected: ATVPDKIKX0DER

   SELECT amzn_id_mask.decrypt_id('ATVPDKIKX0DER');
   -- Expected: 1
   ```

2. Side-by-side comparison (if old functions still exist under original names in a different schema, or test with known values):
   ```sql
   SELECT
     amzn_id_mask.decrypt_id('ATVPDKIKX0DER') as new_result,
     1 as expected_result;
   ```

3. Edge case tests:
   ```sql
   SELECT amzn_id_mask.decrypt_id_safe('INVALID_VALUE');  -- Expected: NULL
   SELECT amzn_id_mask.decrypt_id_safe(NULL);             -- Expected: NULL
   SELECT amzn_id_mask.encrypt_id('A', 9223372036854775807::BIGINT);  -- Max BIGINT
   ```

4. Batch performance test:
   ```sql
   WITH test_data AS (
     SELECT row_number() over (order by true) as id FROM pg_attribute LIMIT 10000
   )
   SELECT COUNT(*) FROM (SELECT amzn_id_mask.encrypt_id('A', id) FROM test_data);
   ```

5. Check Lambda invocation in CloudWatch:
   ```bash
   aws cloudwatch get-metric-statistics --namespace AWS/Lambda \
     --metric-name Invocations --dimensions Name=FunctionName,Value=RedshiftAmznIdMask-AddrSvcProd-DecryptId \
     --start-time $(date -u -v-1H +%Y-%m-%dT%H:%M:%S) --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
     --period 300 --statistics Sum --region us-east-1
   ```

**Test Requirements**:
- All known encrypt/decrypt pairs return correct values
- `decrypt_id_safe` returns NULL for invalid inputs
- Batch test completes without errors
- CloudWatch shows successful invocations with no errors

**Integration**: Builds on Step 5. Must pass before proceeding to Step 7 (cutover).

**Demo**: All test queries return expected results; CloudWatch confirms successful Lambda invocations with zero errors.

---

## Step 7: Migration Cutover — Rename Old Python UDFs

**Objective**: Rename existing Python UDFs to `_old` suffix during a low-utilization window, making the new Lambda UDFs the active functions.

**Implementation Guidance**:

1. **Schedule during low-utilization window** (coordinate with team).

2. Rename existing Python UDFs (do NOT drop):
   ```sql
   ALTER FUNCTION amzn_id_mask.encrypt_id(VARCHAR, ANYELEMENT) RENAME TO encrypt_id_old;
   ALTER FUNCTION amzn_id_mask.decrypt_id(VARCHAR) RENAME TO decrypt_id_old;
   ALTER FUNCTION amzn_id_mask.decrypt_id_safe(VARCHAR) RENAME TO decrypt_id_safe_old;
   ```
   Note: Adjust signatures based on what's actually installed (discovered in Step 1).

3. Verify old functions are renamed:
   ```sql
   SELECT n.nspname, p.proname, l.lanname
   FROM pg_proc p JOIN pg_language l ON p.prolang = l.oid JOIN pg_namespace n ON p.pronamespace = n.oid
   WHERE n.nspname = 'amzn_id_mask'
   ORDER BY p.proname;
   ```

4. Verify new external functions are now the active ones:
   ```sql
   SELECT amzn_id_mask.decrypt_id('ATVPDKIKX0DER');
   -- Should invoke Lambda and return 1
   ```

**Rollback procedure** (if issues detected):
```sql
-- Drop new Lambda UDFs
DROP FUNCTION amzn_id_mask.encrypt_id(VARCHAR, BIGINT);
DROP FUNCTION amzn_id_mask.decrypt_id(VARCHAR);
DROP FUNCTION amzn_id_mask.decrypt_id_safe(VARCHAR);

-- Rename old functions back
ALTER FUNCTION amzn_id_mask.encrypt_id_old RENAME TO encrypt_id;
ALTER FUNCTION amzn_id_mask.decrypt_id_old RENAME TO decrypt_id;
ALTER FUNCTION amzn_id_mask.decrypt_id_safe_old RENAME TO decrypt_id_safe;
```

**Test Requirements**:
- Old functions are renamed (visible as `*_old` in pg_proc)
- New external functions resolve when called by original name
- Existing production queries continue to work
- No errors in CloudWatch or Redshift logs

**Integration**: Builds on Step 6 (validation passed). This is the actual cutover point.

**Demo**: Production queries using `amzn_id_mask.decrypt_id` now route through Lambda; old Python UDFs exist as `*_old` backups.

---

## Step 8: Post-Migration Validation & Monitoring

**Objective**: Monitor the new Lambda UDFs for 24-48 hours to confirm stable operation in production.

**Implementation Guidance**:

1. Monitor CloudWatch dashboard (created by construct):
   - Invocation count
   - Error rate
   - Duration (p50, p99)
   - Throttles

2. Check for errors in Lambda logs:
   ```bash
   aws logs filter-log-events \
     --log-group-name "/aws/lambda/RedshiftAmznIdMask-AddrSvcProd-DecryptId" \
     --filter-pattern "ERROR" \
     --start-time $(date -u -v-24H +%s)000 \
     --region us-east-1
   ```

3. Check Redshift UDF execution logs:
   ```sql
   SELECT funcname, message, starttime
   FROM stl_udf_log
   WHERE funcname LIKE '%decrypt%' AND message LIKE '%ERROR%'
   ORDER BY starttime DESC LIMIT 10;
   ```

4. Confirm no query failures related to ID masking functions in application logs.

**Test Requirements**:
- Zero Lambda errors over 24-48 hours
- No throttling events
- p99 latency within acceptable bounds
- No application-level query failures

**Integration**: Builds on Step 7. Must pass before Step 9 (risk resolution).

**Demo**: CloudWatch dashboard shows healthy metrics; zero errors over monitoring period; production queries operating normally.

---

## Step 9: Resolve Policy Engine Risk

**Objective**: Confirm the PE risk will auto-resolve now that Python UDFs are no longer being executed.

**Implementation Guidance**:

1. Verify no Python UDF execution in trailing period:
   ```sql
   SELECT p.proname, n.nspname, l.lanname
   FROM pg_proc p JOIN pg_language l ON p.prolang = l.oid JOIN pg_namespace n ON p.pronamespace = n.oid
   WHERE l.lanname = 'plpythonu' AND p.proname IN ('encrypt_id','decrypt_id','decrypt_id_safe')
     AND n.nspname = 'amzn_id_mask';
   -- Should return only *_old functions (which are not being called)
   ```

2. Acknowledge the PE risk at https://policyengine.amazon.com/entity/6386943 with a comment explaining:
   - Migration completed on [date]
   - Old Python UDFs renamed to `*_old`
   - New Lambda UDFs active and validated
   - Risk will auto-resolve after 30 days of no Python UDF usage

3. (Optional) After 30+ days with no issues, drop the old `*_old` Python UDFs to clean up:
   ```sql
   DROP FUNCTION amzn_id_mask.encrypt_id_old(VARCHAR, ANYELEMENT);
   DROP FUNCTION amzn_id_mask.decrypt_id_old(VARCHAR);
   DROP FUNCTION amzn_id_mask.decrypt_id_safe_old(VARCHAR);
   ```

**Test Requirements**:
- No Python UDF functions with original names exist
- PE risk acknowledged with migration details
- (After 30 days) PE risk auto-resolves

**Integration**: Final step. Completes the migration lifecycle.

**Demo**: PE risk acknowledged; after 30 days, risk status changes to resolved automatically.
