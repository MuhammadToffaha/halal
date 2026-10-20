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
| 2️⃣ | Halal certification label on the product (Open Food Facts) | ✅ HALAL |
| 3️⃣ | Ingredient check with `data/rules.json` | ⛔ / 🟠 / 🟢 |

- ⛔ **Not halal** — pork, gelatine (pork in DE), alcohol, carmine E120, blood
- 🟠 **Doubtful** — E471 & animal emulsifiers, beef gelatine, animal rennet, meat without halal slaughter…
- 🟢 **No haram ingredients found** — ⚠️ not a certificate
- ❔ **Unknown** — no ingredient list

> ⚖️ Guidance only. Certified status always comes from the certifier (Halal Control, HQC, HFCE …).

## 🗂️ Repo layout
```
data/products.json    📗 verified products  (edit → app syncs live, no new APK)
data/rules.json       🧪 ingredient rules   (edit → app syncs live)
data/resources.json   📚 sources + "search inside" links
app/                  📱 Android app (Kotlin, Jetpack Compose, CameraX, ML Kit)
.github/workflows/    🤖 build APK + release, validate data
```

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

## 🔐 Signing
`app/release.jks` signs every release so updates install over the old app. Password can be overridden with the `KEYSTORE_PASSWORD` secret. Keep the same keystore forever — a new key = uninstall/reinstall.

## 🔥 Firebase?
Not needed. GitHub is the database (raw JSON). Firebase only makes sense later if many users should submit products live.
