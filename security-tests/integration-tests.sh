#!/bin/bash
# SecureGate Integration Test Suite
# Run: bash security-tests/integration-tests.sh

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
IAM_URL="${IAM_URL:-http://localhost:8080/iam}"
API_URL="${API_URL:-http://localhost:8081/api}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:5173}"

echo "================================================"
echo "  SecureGate Integration Test Suite"
echo "================================================"
echo ""
echo "IAM URL: $IAM_URL"
echo "API URL: $API_URL"
echo "Frontend URL: $FRONTEND_URL"
echo ""

PASSED=0
FAILED=0

# Test function
run_test() {
    local name="$1"
    local command="$2"
    
    echo -n "Testing: $name... "
    
    if eval "$command" > /dev/null 2>&1; then
        echo -e "${GREEN}PASSED${NC}"
        ((PASSED++))
    else
        echo -e "${RED}FAILED${NC}"
        ((FAILED++))
    fi
}

echo ""
echo "--- Backend Health Checks ---"

# Test 1: API Gateway health
run_test "API Gateway health" \
    "curl -sf '$API_URL/test' | grep -q 'ok'"

# Test 2: IAM JWKS endpoint
run_test "IAM JWKS endpoint" \
    "curl -sf '$IAM_URL/jwk' | grep -q 'keys'"

# Test 3: IAM JWKS contains Ed25519 keys
run_test "JWKS contains Ed25519 keys" \
    "curl -sf '$IAM_URL/jwk' | grep -q 'Ed25519'"

echo ""
echo "--- OAuth 2.1 Endpoints ---"

# Test 4: Authorization endpoint accessible
run_test "Authorization endpoint" \
    "curl -sf '$IAM_URL/authorize?client_id=demo-client&response_type=code&redirect_uri=http://localhost:5173/callback&code_challenge=test&code_challenge_method=S256' | grep -q 'Login\|login'"

# Test 5: Token endpoint rejects missing parameters
run_test "Token endpoint validates params" \
    "curl -sf -X POST '$IAM_URL/token' -d 'grant_type=authorization_code' | grep -q 'error'"

echo ""
echo "--- API Gateway Security ---"

# Test 6: Protected endpoint requires token
run_test "Protected endpoint requires auth" \
    "curl -s '$API_URL/users/me' | grep -q 'unauthorized\|Unauthorized'"

# Test 7: Invalid token rejected
run_test "Invalid token rejected" \
    "curl -s -H 'Authorization: Bearer invalid-token' '$API_URL/users/me' | grep -q 'invalid\|unauthorized'"

# Test 8: Public endpoints accessible
run_test "Public health endpoint accessible" \
    "curl -sf '$API_URL/users/health' | grep -q 'healthy'"

echo ""
echo "--- ABAC Policy Endpoints ---"

# Test 9: Policy list requires auth
run_test "Policy list requires auth" \
    "curl -s '$API_URL/policies' | grep -q 'unauthorized\|Unauthorized'"

echo ""
echo "--- Frontend Availability ---"

# Test 10: Frontend accessible
run_test "Frontend accessible" \
    "curl -sf '$FRONTEND_URL' | grep -q 'SecureGate\|html'"

echo ""
echo "================================================"
echo "  Test Results"
echo "================================================"
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed!${NC}"
    exit 1
fi
