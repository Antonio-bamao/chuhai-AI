# C-6 Recharge and Advertising UI Closure Design

## Goal

Make the existing recharge and advertising entry points usable offline at the UI layer only. Both pages must render stable empty states and must not start payment, advertising, billing, authorization, or external navigation.

## Scope

- Recharge enters only through the existing bottom-left global `充值` button in the main shell.
- Advertising enters only through the existing WhatsApp menu item `C3460_001` and the original `com.sbf.main.theme.ad.f` shell, including its original `广告充值` entry.
- The implementation produces a candidate package only. It does not modify live `App.dll` or deploy.

## Evidence and Constraints

- There is no evidenced WhatsApp side-menu recharge node. No new side-menu node may be created.
- The original advertising shell is `com.sbf.main.theme.ad.f`; it contains the decoded labels `广告充值` and `帐号余额：`, and its route family includes `/views/overseasAds/dataBoard`.
- The offline package does not contain the complete advertising or payment frontend resources. The original shells are retained, while their content is supplied by narrow local empty-state contracts.
- Real payment routes include `/pc/alipay/enterpriseAuth`, `/pc/alipay/personal/auth`, `/pc/userPayofflineOrder/my`, and external xpay or Alipay URLs. None may be requested.

## Design

### Recharge

The existing global button is patched at its original action boundary to open the original in-application payment surface in offline mode. The content shows that balance information is unavailable and that there are no local recharge records. Payment, order creation, cashier navigation, QR polling, and third-party redirects are disabled before they can dispatch a request.

### Advertising

The existing `C3460_001` menu action is patched to mount the original `theme.ad.f` shell. Its data-board and `广告充值` content receive only local empty-state contracts: no advertising plans, no delivery data, and no available balance. Creating a plan, opening platform authorization, recharging, launching delivery, and deducting balance are disabled before request dispatch.

### Local Contract Boundary

Only the exact routes and read contracts observed while mounting these two surfaces may receive local responses. There is no wildcard API proxy. Empty responses use explicit `localOnly` and empty-list semantics; they never manufacture balance, order, campaign, delivery, or authorization data. Any action that bypasses a disabled control is locally rejected without state mutation.

## Verification

1. Tests are written first for the global recharge entry, `C3460_001` advertising entry, original-shell mounting, empty-state contracts, disabled action guards, and absence of real payment or advertising endpoints.
2. The candidate starts from the product selector and reaches both pages without a white screen or stuck spinner.
3. Screenshots show the original entry, rendered empty state, and disabled business actions.
4. Logs show the route and module markers, local contract hits, zero unpaired `app.xdxsoft.com` or `diangxiaomi.com` business requests, and no console error.
5. The complete regression suite passes. The live package, collection chain, `.cnf`, `cloud.spider.b`, `libmytrpc`, and system clock remain untouched.

## Out of Scope

- Payment, orders, cashiers, QR codes, balances, refunds, and billing.
- Advertising account authorization, campaign creation, delivery, optimization, reporting, callbacks, or charges.
- Any live swap, deployment, or external request.
