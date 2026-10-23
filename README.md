# ☪️ Halal Scanner

Simple Android app 📱 — scan any barcode in a German supermarket 🛒 and see if it is **Halal**, **Doubtful (Mashbooh)** or **Not halal**.

This GitHub repo is the **central place** for everything: code, halal database, rules, sources and app updates.

## 📥 Install
1. Open **[Releases](../../releases/latest)** on your phone → download `halal.apk`
2. Allow "install unknown apps" → install
3. Done ✅ — new versions show a 🎉 **Update** banner inside the app

## 🧭 How a verdict is made
| Order | Source | Result |
|---|---|---|
| 1️⃣ | `data/products.json` — your verified list (with certifier) | ✅ / ⛔ / 🟠 |
| 2️⃣ | `data/off_halal.json` — every halal-labelled product sold in Germany (auto, weekly) | ✅ HALAL |
| 2️⃣b | Halal label on the scanned product (Open Food Facts / Open Beauty Facts live) | ✅ HALAL |
| 3️⃣ | Ingredient check with `data/rules.json` | ⛔ / 🟠 / 🟢 |

- ⛔ **Not halal** — pork, gelatine (pork in DE), alcohol, carmine E120, blood
- 🟠 **Doubtful** — E471 & animal emulsifiers, beef gelatine, animal rennet, meat without halal slaughter…
- 🟢 **No haram ingredients found** — ⚠️ not a certificate
- ❔ **Unknown** — no ingredient list

> ⚖️ Guidance only. Certified status always comes from the certifier (Halal Control, HQC, HFCE …).

## 🗂️ Repo layout
```
data/products.json    📗 verified products  (edit → app syncs live, no new APK)
data/off_halal.json   🏷️ AUTO weekly import: halal-labelled products in Germany (food + dm/Rossmann cosmetics, with shops)
data/rules.json       🧪 ingredient rules   (edit → app syncs live)
scripts/import_off.py 🤖 the importer (Actions → "Import halal products" → Run workflow to refresh now)
data/resources.json   📚 sources + "search inside" links
app/                  📱 Android app (Kotlin, Jetpack Compose, CameraX, ML Kit)
.github/workflows/    🤖 build APK + release, validate data
```

## 🏪 Which products are covered?
- 🔍 **Any barcode** in German supermarkets and drugstores (REWE, EDEKA, ALDI, Lidl, Kaufland, dm, Rossmann…) is looked up live in Open Food Facts (food) and Open Beauty Facts (cosmetics), then checked.
- 🏷️ **Halal-labelled ones** are also saved in the repo (`off_halal.json`), searchable by name, brand or shop, and work offline.

## ➕ Add a product
Edit `data/products.json` on GitHub:
```json
{ "barcode": "4001234567890", "name": "Product", "brand": "Brand",
  "status": "halal", "source": "Halal Control GmbH", "note": "", "url": "https://…certificate" }
```
Or tap **📝 Add / correct** in the app → opens a pre-filled GitHub issue.

## 🔄 Updates
- 📦 Push code to `main` → GitHub Actions builds signed APK → new Release `vN` → app offers update
- 📗 Push to `data/` → no build needed; app downloads data at start (or ⚙️ Sync data)

## 🤫 Silent updates
- ⚙️ Settings → **Silent auto-update** (on by default, Wi-Fi only by default)
- 🕐 Checks GitHub every 6 h and when you leave the app → downloads → installs while the app is closed
- ℹ️ Android rule: the **first** self-update asks once; after that updates install with no prompt (Android 12+)

## 🔐 Signing
`app/release.jks` signs every release so updates install over the old app. Password can be overridden with the `KEYSTORE_PASSWORD` secret. Keep the same keystore forever — a new key = uninstall/reinstall.

## 🔥 Firebase?
Not needed. GitHub is the database (raw JSON). Firebase only makes sense later if many users should submit products live.
