# Postman Testing Guide - Overdraft Facility Management API

Files (in `postman/`):

| File | Purpose |
|---|---|
| `Overdraft-Facility.postman_collection.json` | 52 requests in 12 folders, each with automated `pm.test` assertions |
| `Overdraft-Local.postman_environment.json` | Environment with `baseUrl = http://localhost:8080` |

---

## 1. Prerequisites

1. Start the app on a **fresh H2 database** (restart it for exact, repeatable results):
   ```bash
   ./mvnw spring-boot:run
   ```
   Confirm it is up: open <http://localhost:8080/swagger-ui.html>.
2. Postman desktop (or Newman, see section 6).
3. Scheduler: the cron jobs (23:59 accrual, 01:00 billing) may fire while you test. They are harmless, but for
   clean demos start with `--overdraft.scheduler.enabled=false`:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.arguments=--overdraft.scheduler.enabled=false
   ```

## 2. Import

1. Postman -> **Import** -> select both JSON files in `postman/`.
2. Choose environment **Overdraft - Local** (top right). Change `baseUrl` if the app is on another host/port.
3. Collection variables are pre-filled; the scenario uses fixed business dates so results are deterministic:

| Variable | Value | Meaning |
|---|---|---|
| `day1` / `day2` / `day3` | 2026-03-29 / 30 / 31 | EOD business dates for the 3-day scenario |
| `billingDate` | 2026-04-01 | Billing run date (bills accruals up to 31-Mar) |
| `cycleAccrualDate`, `cycleSkipDate`, `cycleBillDate` | 2026-05-10, 2026-06-01, 2026-06-15 | Cycle-day test |
| `accountNumber`, `cycleAccount`, `runId` | set by scripts | Do not edit |

## 3. How to run

**Whole suite:** right-click the collection -> **Run collection** -> keep the order -> **Run**.
Do not reorder or skip folders 01-07: each depends on the state left by the previous one.
All requests can be re-run any number of times: every run creates new accounts with unique references.

**Manually:** open requests top to bottom and press **Send**; check the **Test Results** tab of each response.

## 4. Step-by-step scenario and expected results

Notation: daily interest = `utilized x 12 / 36500`, 4 dp HALF_EVEN.

### Folder 00 - Health check
| Step | Request | Expected |
|---|---|---|
| 0.1 | `GET /v3/api-docs` | 200; OpenAPI 3; all 7 endpoints present |

### Folder 01 - Account opening
| Step | Request | Expected |
|---|---|---|
| 1.1 | `POST /accounts` HDFC, limit 1,500,000, 12%, cycle day 1 | **201**; number `HDFCOD##########`; utilized 0; available 1,500,000; ACTIVE; number saved to `{{accountNumber}}` |
| 1.2 | `GET /accounts/{{accountNumber}}/statement` | 0% utilization; no transactions; no interest log; `nextBillingDate` present |

### Folder 02 - Day 1: draw 3,00,000 and run EOD
| Step | Request | Expected |
|---|---|---|
| 2.1 | `POST .../drawdown` `{"amount":300000.00,"referenceId":"DD-<runId>-1"}` | 200; `DRAWDOWN`; utilized 300,000; available 1,200,000 |
| 2.2 | `POST /admin/trigger-eod-accrual?businessDate=2026-03-29` | 200; job `EOD_INTEREST_ACCRUAL`; processed >= 1; failed 0 |
| 2.3 | Statement | Unbilled **98.6301**; one ledger row (300,000 @ 12 -> 98.6301, billed=false); principal still 300,000; utilization 20.00% |

### Folder 03 - Day 2: balance unchanged
| 3.1 | EOD for 2026-03-30 | 200 |
|---|---|---|
| 3.2 | Statement | Unbilled **197.2602**; 2 rows; newest row 30-Mar = 98.6301 |

### Folder 04 - Day 3: repay 1,00,000
| Step | Request | Expected |
|---|---|---|
| 4.1 | `POST .../repay` 100,000 | 200; `REPAYMENT`; utilized 200,000; available 1,300,000 (no fee) |
| 4.2 | EOD for 2026-03-31 | 200 |
| 4.3 | Statement | Unbilled **263.0136** (= 98.6301 + 98.6301 + 65.7534); newest row 200,000 -> **65.7534**; utilization 13.33%; transactions newest first: REPAYMENT, DRAWDOWN |

### Folder 05 - Idempotency
| 5.1 | Re-run EOD for 2026-03-31 | processed **0**, skipped >= 1 |
|---|---|---|
| 5.2 | Statement | still 3 rows, unbilled 263.0136 |

### Folder 06 - Monthly billing
| Step | Request | Expected |
|---|---|---|
| 6.1 | `POST /admin/trigger-monthly-billing?businessDate=2026-04-01&forceAllCycles=true` | 200; job `MONTHLY_INTEREST_BILLING`; processed >= 1 |
| 6.2 | Statement | `263.0136 -> 263.01` (HALF_EVEN); **utilized 200,263.01**; available **1,299,736.99**; unbilled **0**; all rows `billed=true`; newest transaction `INTEREST_CHARGED` 263.01 |
| 6.3 | Re-run billing | no failures |
| 6.4 | Statement | utilized still 200,263.01; exactly one `INTEREST_CHARGED` |

### Folder 07 - Full repayment
| 7.1 | Repay 200,263.01 | utilized 0; available 1,500,000 |
|---|---|---|
| 7.2 | Statement | 0%; 4 transactions (DRAWDOWN, REPAYMENT, INTEREST_CHARGED, REPAYMENT) |

### Folder 08 - Billing cycle day (`forceAllCycles=false`)
| Step | Request | Expected |
|---|---|---|
| 8.1 | Open AXIS account, cycle day **15**, limit 500,000 | 201; `AXISOD##########`; saved to `{{cycleAccount}}` |
| 8.2 | Drawdown 100,000 | 200 |
| 8.3 | EOD 2026-05-10 | accrues **32.8767** |
| 8.4 | Billing run 2026-06-01, `forceAllCycles=false` | cycle day 1 only: this account is skipped |
| 8.5 | Statement | utilized 100,000; unbilled 32.8767 |
| 8.6 | Billing run 2026-06-15, `forceAllCycles=false` | account billed |
| 8.7 | Statement | **utilized 100,032.88**; unbilled 0; `INTEREST_CHARGED` 32.88 |

### Folder 09 - Analytics (`GET /analytics/compare`)
| Case | Query | Expected |
|---|---|---|
| 9.1 | limit 1,500,000; used 300,000; 12%; 360 d | Loan: 12 months, EMI **133,273.18**, interest 99,278.16. OD interest **35,506.85** (daily 98.6301). Savings **63,771.31**; cheaper = `OVERDRAFT` |
| 9.2 | used 0; 90 d | OD interest 0; savings = full loan interest |
| 9.3 | used 1,500,000; 360 d | OD interest 177,534.25 > loan interest; cheaper = `PERSONAL_LOAN`; savings negative |
| 9.4 | holdingDays 0 | 400 `VALIDATION_FAILED`, violation on `holdingDays` |
| 9.5 | used > limit | 400 `INVALID_REQUEST` |
| 9.6 | missing holdingDays | 400 |

### Folder 10 - Negative and validation cases (account is fully repaid)
| Case | Expected |
|---|---|
| Drawdown 1,500,000.01 | 422 `INSUFFICIENT_LIMIT` |
| Repay 0.01 with zero utilization | 422 `REPAYMENT_EXCEEDS_OUTSTANDING` |
| Drawdown exactly 1,500,000.00 | 200; available limit 0 (boundary) |
| Drawdown 0.01 afterwards | 422 `INSUFFICIENT_LIMIT` |
| Same `referenceId` twice | 1st 200, 2nd **409** `DUPLICATE_TRANSACTION` |
| Unknown account (statement / drawdown) | 404 `ACCOUNT_NOT_FOUND` |
| Blank customer, no bank, negative limit, rate 150 | 400 `VALIDATION_FAILED` with per-field `violations` |
| `billingCycleDay` 29 | 400 `VALIDATION_FAILED` |
| `bankName: "XYZ"` / malformed JSON | 400 `MALFORMED_REQUEST` |
| amount 0 or 10.123 | 400 `VALIDATION_FAILED` |
| statement `limit=0` | 400 `VALIDATION_FAILED` |
| `businessDate=29-03-2026` | 400 |

Cleanup requests in this folder restore the account to zero utilization.

### Folder 11 - Admin triggers with default date
Both triggers called without `businessDate` return 200 with an ISO date (today, Asia/Kolkata).

## 5. Interpreting results

- **Test Results tab:** every request lists passed/failed assertions. The collection-level script adds a
  response-time (< 3 s) and JSON content-type check to every request.
- **Job summaries cover all accounts** in the DB (`accountsProcessed`, `accountsSkipped`, `accountsFailed`), so exact
  per-account amounts are asserted on the **statement**, not the summary. This is why a fresh DB is recommended
  but not required.
- **Error format:** `{timestamp, status, code, message, path, violations[]}`. Missing-parameter and invalid-date
  errors raised by Spring itself come back as RFC 7807 problem JSON (status 400).
- **Money in JSON:** plain numbers; billing amounts have 2 decimals, accrual amounts 4.

### Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Could not get response` | App not running or wrong `baseUrl` |
| 404 on `{{accountNumber}}` URLs | Request 1.1 was skipped (variable empty); run folder 01 first |
| Day-N statement shows extra ledger rows | Cron job ran on the same account; restart with the scheduler disabled |
| Numbers differ by 0.0001 | `overdraft.interest.leap-year-aware=true` or a different rate was configured |
| 409 `DUPLICATE_TRANSACTION` in folder 02/04 | A request was re-sent manually; `runId` is regenerated by request 1.1 |

## 6. Command line (Newman)

```bash
npm install -g newman
newman run postman/Overdraft-Facility.postman_collection.json \
       -e postman/Overdraft-Local.postman_environment.json \
       --reporters cli,junit --reporter-junit-export newman-report.xml
```
Exit code is non-zero if any assertion fails, so this can run in CI after the app starts.

## 7. Not covered by Postman

`FROZEN` / `CLOSED` account behaviour (no endpoint changes status yet) is covered by the unit test
`OverdraftAccountTest`; the cron triggers themselves are exercised by calling the equivalent admin endpoints.
