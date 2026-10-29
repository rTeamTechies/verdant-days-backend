#!/bin/bash
# API Flow Test - Run against live backend (http://localhost:8085)
# Usage: ./scripts/api-flow-test.sh [base_url]
set -e
BASE_URL="${1:-http://localhost:8085}"
API="$BASE_URL/api"
PASS=0
FAIL=0
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

log() { echo -e "$1"; }
pass() { ((PASS++)); log "${GREEN}✓ $1${NC}"; }
fail() { ((FAIL++)); log "${RED}✗ $1${NC}"; }

# 1. Login
log "\n=== 1. Login ==="
LOGIN=$(curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" -d '{"phoneNumber":"8015761437","password":"Admin@123"}')
if echo "$LOGIN" | grep -q accessToken; then
  TOKEN=$(echo "$LOGIN" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
  pass "Login successful"
else
  fail "Login failed - ensure backend is running and admin 8015761437/Admin@123 exists"
  exit 1
fi

AUTH="Authorization: Bearer $TOKEN"

# 2. Get Products
log "\n=== 2. Get Products ==="
PRODS=$(curl -s -H "$AUTH" "$API/products")
if echo "$PRODS" | grep -q '"data"'; then
  pass "Products retrieved"
else
  fail "Products failed"
fi

# 3. Get Apartments
log "\n=== 3. Get Apartments ==="
APTS=$(curl -s -H "$AUTH" "$API/apartments")
if echo "$APTS" | grep -q '"data"'; then
  pass "Apartments retrieved"
else
  fail "Apartments failed"
fi

# 4. Get Customers (paginated)
log "\n=== 4. Get Customers (paginated) ==="
CUST=$(curl -s -H "$AUTH" "$API/customers?page=0&size=20")
if echo "$CUST" | grep -qE '"content"|"data".*"id"'; then
  pass "Customers paginated"
  CUST_ID=$(echo "$CUST" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
else
  fail "Customers failed"
  CUST_ID=""
fi

# 5. Create Order
log "\n=== 5. Create Order ==="
if [ -n "$CUST_ID" ]; then
  PROD_ID=$(echo "$PRODS" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
  if [ -n "$PROD_ID" ]; then
    ORDER=$(curl -s -X POST "$API/orders" -H "$AUTH" -H "Content-Type: application/json" \
      -d "{\"customerId\":$CUST_ID,\"paymentType\":\"CASH\",\"items\":[{\"productId\":$PROD_ID,\"quantity\":1}]}")
    if echo "$ORDER" | grep -q '"id"'; then
      pass "Order created"
      ORDER_ID=$(echo "$ORDER" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
    else
      fail "Order create failed: $ORDER"
    fi
  else
    fail "No product ID for order"
  fi
else
  fail "No customer for order - run with fresh seed data"
fi

# 6. Search Orders
log "\n=== 6. Search Orders ==="
ORDERS=$(curl -s -H "$AUTH" "$API/orders?page=0&size=10")
if echo "$ORDERS" | grep -qE '"content"|"orderNumber"|"data":\['; then
  pass "Orders search"
else
  fail "Orders search failed"
fi

# 7. Orders by Customer
log "\n=== 7. Orders by Customer ==="
if [ -n "$CUST_ID" ]; then
  OC=$(curl -s -H "$AUTH" "$API/orders/customer/$CUST_ID")
  if echo "$OC" | grep -q '"data"'; then
    pass "Orders by customer"
  else
    fail "Orders by customer failed"
  fi
fi

# 8. Dashboard Stats
log "\n=== 8. Dashboard Stats ==="
DASH=$(curl -s -H "$AUTH" "$API/dashboard/stats?startDate=2025-01-01&endDate=2025-12-31")
if echo "$DASH" | grep -q totalSales; then
  pass "Dashboard stats"
else
  fail "Dashboard stats failed"
fi

# 9. Apartment Details
log "\n=== 9. Apartment Details ==="
APT_ID=$(echo "$APTS" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
if [ -n "$APT_ID" ]; then
  AD=$(curl -s -H "$AUTH" "$API/apartments/$APT_ID/details")
  if echo "$AD" | grep -q '"apartment"'; then
    pass "Apartment details"
  else
    fail "Apartment details failed"
  fi
fi

# 10. WhatsApp Message
log "\n=== 10. WhatsApp Message ==="
if [ -n "$ORDER_ID" ]; then
  WA=$(curl -s -H "$AUTH" "$API/orders/$ORDER_ID/whatsapp-message")
  if echo "$WA" | grep -q '"url"'; then
    pass "WhatsApp message"
  else
    fail "WhatsApp message failed"
  fi
fi

# Summary
log "\n========== Summary =========="
log "${GREEN}Passed: $PASS${NC}"
log "${RED}Failed: $FAIL${NC}"
[ $FAIL -gt 0 ] && exit 1
exit 0
