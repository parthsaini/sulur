# Research: Existing Python UDFs (Legacy)

## Overview

The PythonRedshiftAmznIdMask wiki page is now **DEPRECATED**. It documents the legacy Python-based UDFs that are being replaced.

## How Legacy UDFs Work

- Python library (`amzn_id_mask`) uploaded to Redshift via `CREATE LIBRARY` from S3
- Functions created as `plpythonu` UDFs in the `amzn_id_mask` schema
- Uses Tiny Encryption Algorithm (TEA) for obfuscation/de-obfuscation of 64-bit numbers to/from base36 strings

## Legacy Function Signatures

```sql
-- Uses ANYELEMENT pseudo-type (not available for Lambda UDFs)
AMZN_ID_MASK.ENCRYPT_ID(typetag VARCHAR, num ANYELEMENT) → VARCHAR
AMZN_ID_MASK.DECRYPT_ID(typetag VARCHAR) → BIGINT
AMZN_ID_MASK.DECRYPT_ID_SAFE(typetag VARCHAR) → BIGINT
AMZN_ID_MASK.ENCRYPT_SHIPMENT_ID(decrypted_shipment_id ANYELEMENT) → VARCHAR
AMZN_ID_MASK.DECRYPT_SHIPMENT_ID(encrypted_shipment_id VARCHAR) → BIGINT
AMZN_ID_MASK.ENCRYPT_RMA_ID(decrypted_rma_id ANYELEMENT) → VARCHAR
AMZN_ID_MASK.DECRYPT_RMA_ID(encrypted_rma_id VARCHAR) → BIGINT
```

## AddressService Usage

Per the policy engine risk, AddressService uses only:
- **Function**: `decrypt_id`
- **Cluster**: `address-service-prod`
- **Region**: `us-east-1`

## Key Difference: ANYELEMENT vs Explicit Types

The legacy Python UDFs used `ANYELEMENT` which accepted any numeric type without ambiguity. The new Lambda UDFs require explicit type signatures (BIGINT by default, optional NUMERIC(38,0) overloads). This means:
- Column references work fine (they have concrete types)
- Integer literals may need explicit casts if both overloads are installed

## Type Tags

| Tag | Description |
|---|---|
| A | Customer (also CommId, DeviceTypeId, MarketplaceId, MerchantCustomerId) |
| B | Batch |
| C | Chargeback claim |
| G | Brand ID |
| J | FPPurchase |
| P | Paypage |
| R | Reviews |
| RA | ReuseCustomer |
| RC | RegistryClaim |
| ROC | RmtOneClick |
| S | Storefront |
| T | UploadedExch |
| x | Forums |

## References

- [PythonRedshiftAmznIdMask Wiki (DEPRECATED)](https://w.amazon.com/bin/view/PythonRedshiftAmznIdMask/)
- [RedshiftAmznIdMask (replacement)](https://w.amazon.com/bin/view/RedshiftAmznIdMask/)
