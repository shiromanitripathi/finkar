# Finkar

A money manager for students that notices when you pay, asks "Record this payment?" instantly, and keeps track of what friends owe you. Android app (Kotlin + Jetpack Compose). All data stays on your phone.

## What it does

**Automatic capture**
- Reads bank SMS and notifications from GPay, PhonePe, Paytm, BHIM, CRED and Amazon Pay.
- A pop-up appears the moment you pay, with **Record**, **Split** and **Ignore** buttons.
- The category (Food, Travel, Study...) is guessed from the merchant name.
- If the SMS and the UPI app both fire for one payment, you only get one pop-up.

**Student tools**
- Home dashboard: spent this month, left to spend, and a **safe-to-spend per day** figure.
- Monthly budget plus per-category limits with progress bars (turn amber near the limit, red when over).
- Savings goals (laptop, trip, phone) with "add money".
- Track pocket money and other income.
- Search and filter every payment, edit category or notes, delete.
- Insights: category donut chart, last 7 days bar chart, top spots, CSV export.
- Light and dark mode.

**Friends and shared expenses**
- Add friends (name, optional phone).
- On any payment tap **Split with friends**: split equally (with or without you) or enter custom amounts.
- Your spending only counts **your share**; what friends owe is tracked separately.
- Each friend has a ledger: every shared expense, every payback, and the pending balance.
- **Remind with all records** sends a ready message (itemised list, paid so far, pending, your UPI ID) straight to their WhatsApp, or shares it through any app.
- Record paybacks (full or partial) when a friend pays you.

## Get the APK (no installs needed)

1. Create a **private** repository on github.com and upload this whole folder, including the hidden `.github` folder.
2. Open the repo's **Actions** tab, choose **Build APK**, click **Run workflow**.
3. After about 5 minutes download **Finkar-debug-apk** from the run page, unzip, and send `app-debug.apk` to your phone.

Or open the folder in Android Studio and use **Build, Build APK(s)**.

## First run on the phone

1. Allow "install unknown apps" when installing.
2. On the Home screen tap **Allow** next to each item in the "Turn on automatic capture" card (SMS, pop-ups, UPI apps).
3. Test it: send yourself an SMS like `Rs.250 debited from A/c XX1234 to VPA shop@upi`.
4. On Xiaomi, Oppo, Vivo or OnePlus phones, turn off battery optimisation for Finkar and allow auto-start, or the phone may stop it in the background.

## Limits

- Android only. iOS does not let apps read SMS or other apps' notifications.
- Friends' balances live on your phone only. Friends do not get the app or a live view; they receive your WhatsApp reminder.
- Parsing uses text patterns, so some banks' SMS formats may need a pattern added in `PaymentParser.kt`.
- Cash payments are not captured (use **＋ Add**).
- Publishing on Google Play needs an SMS-permission declaration. Fine for personal use.

## Project layout

- `capture/` SMS receiver, UPI notification listener, payment parser, pop-up
- `data/` Room database, categories, ledger and stats logic
- `ui/` theme, charts, screens and dialogs
