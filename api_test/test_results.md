# API Test Results
**Date:** 2026-01-10 17:24:06
**Total Tests:** 13  
**Passed:** 13  
**Failed:** 0  
**Success Rate:** 100%

## Test Details

| Test | Method | Status | Result |
|------|--------|---------|--------|
| IAM Health Check | GET | 200 | âœ… |
| IAM Ready Check | GET | 200 | âœ… |
| IAM Live Check | GET | 200 | âœ… |
| Get All Users | GET | 200 | âœ… |
| Get Notifications | GET | 200 | âœ… |
| Unread Count | GET | 200 | âœ… |
| Get Policies | GET | 200 | âœ… |
| Get Settings | GET | 200 | âœ… |
| Get Audit Logs | GET | 200 | âœ… |
| Audit Statistics | GET | 200 | âœ… |
| Create User | POST | 201 | âœ… |
| Gateway Health | GET | 200 | âœ… |
| Stego Health | GET | 200 | âœ… |

## Detailed Responses

### IAM Health Check
- **Method:** GET
- **Endpoint:** http://127.0.0.1:8081/iam-service/api/health
- **Status:** 200
- **Result:** PASSED
- **Response:** `{"status":"UP","service":"iam-service","version":"1.0.0","uptimeSeconds":53,"timestamp":"2026-01-10T16:24:01.816060426Z"}`
### IAM Ready Check
- **Method:** GET
- **Endpoint:** http://127.0.0.1:8081/iam-service/api/health/ready
- **Status:** 200
- **Result:** PASSED
- **Response:** `{"status":"READY"}`
### IAM Live Check
- **Method:** GET
- **Endpoint:** http://127.0.0.1:8081/iam-service/api/health/live
- **Status:** 200
- **Result:** PASSED
- **Response:** `{"status":"ALIVE"}`
### Get All Users
- **Method:** GET
- **Endpoint:** http://127.0.0.1:8081/iam-service/api/users
- **Status:** 200
- **Result:** PASSED
- **Response:** `[{"email":"john.smith@securegate.io","fullName":"John Smith","mfaEnabled":true,"roles":[],"status":"ACTIVE","userId":"9ce4cfac-b042-4b33-9902-7d7ffd676cac","username":"jsmith"},{"email":"sarah.johnson`
### Get Notifications
- **Method:** GET
- **Endpoint:** http://127.0.0.1:8081/iam-service/api/notifications
- **Status:** 200
- **Result:** PASSED
- **Response:** `[{"category":"System","createdAt":"2026-01-03T16:24:05.642438342","message":"SecureGate IAM Portal v1.0.0 has been successfully deployed to production. All services are operational.","notificationId":`
### Unread Count
- **Method:** GET
- **Endpoint:** http://127.0.0.1:8081/iam-service/api/notifications/unread-count
- **Status:** 200
- **Result:** PASSED
- **Response:** `{"count":4}`
### Get Policies
- **Method:** GET
- **Endpoint:** http://127.0.0.1:8081/iam-service/api/policies
- **Status:** 200
- **Result:** PASSED
- **Response:** `[{"action":"*","active":true,"conditions":"{\"role\": \"ROLE_ADMIN\"}","description":"Administrators have full access to all resources","effect":"PERMIT","name":"AdminFullAccess","policyId":"e1f5a4c6-`
### Get Settings
- **Method:** GET
- **Endpoint:** http://127.0.0.1:8081/iam-service/api/settings
- **Status:** 200
- **Result:** PASSED
- **Response:** `[{"category":"Authentication","dataType":"number","description":"Access token expiration in seconds (15 minutes)","editable":true,"lastModifiedAt":"2026-01-10T16:24:05.853823942Z","settingKey":"auth_s`
### Get Audit Logs
- **Method:** GET
- **Endpoint:** http://127.0.0.1:8081/iam-service/api/audit
- **Status:** 200
- **Result:** PASSED
- **Response:** `[{"action":"authenticate","actor":"admin","details":"Successful login via MFA from Chrome/Windows at 10.0.1.50","ip":"10.0.1.50","logId":"a276b485-dd29-4158-b1ce-c8ad7d910fdf","resource":"session:sess`
### Audit Statistics
- **Method:** GET
- **Endpoint:** http://127.0.0.1:8081/iam-service/api/audit/stats
- **Status:** 200
- **Result:** PASSED
- **Response:** `{"dangerCount":24,"topActors":[{"count":892,"actor":"admin"},{"count":456,"actor":"jsmith"},{"count":312,"actor":"sjohnson"},{"count":287,"actor":"ebrown"},{"count":198,"actor":"system"}],"totalEvents`
### Create User
- **Method:** POST
- **Endpoint:** http://127.0.0.1:8081/iam-service/api/users
- **Status:** 201
- **Result:** PASSED
- **Response:** `{"email":"apitest@test.com","fullName":"API Test User","mfaEnabled":false,"passwordHash":"$argon2id$v=19$m=65536,t=3,p=1$WakRVu6pLwkHQCapc3WYog==$h2EF1AaHAUR01z4btCDo2NkHM8MKo00hJbxoZlGeA9E=","roles":`
### Gateway Health
- **Method:** GET
- **Endpoint:** http://127.0.0.1:8080/api-gateway/api/health
- **Status:** 200
- **Result:** PASSED
- **Response:** `{"status":"UP","service":"api-gateway","version":"1.0.0","uptimeSeconds":1839,"timestamp":"2026-01-10T16:24:06.719237800Z"}`
### Stego Health
- **Method:** GET
- **Endpoint:** http://127.0.0.1:8084/stego-module/api/health
- **Status:** 200
- **Result:** PASSED
- **Response:** `{"status":"UP","service":"stego-module","version":"1.0.0","algorithm":"STC","encryption":"ChaCha20-Poly1305","uptimeSeconds":1003,"timestamp":"2026-01-10T16:24:06.791398424Z"}`

