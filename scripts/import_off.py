#!/usr/bin/env python3
"""
Imports every product sold in Germany that carries a HALAL label on
Open Food Facts (food, supermarkets) and Open Beauty Facts (cosmetics, dm / Rossmann)
into data/off_halal.json. Run weekly by .github/workflows/import-products.yml.
"""
import json, os, sys, time, datetime, urllib.request, urllib.parse

OUT = os.path.join(os.path.dirname(__file__), "..", "data", "off_halal.json")
UA = "HalalScanner-import/1.0 (https://github.com/MuhammadToffaha/halal)"
FIELDS = "code,product_name,product_name_de,product_name_en,brands,stores,stores_tags,labels_tags,quantity"
SOURCES = [
    ("Open Food Facts", "https://world.openfoodfacts.org"),
    ("Open Beauty Facts", "https://world.openbeautyfacts.org"),
]
COUNTRY = os.environ.get("COUNTRY", "germany")
PAGE_SIZE = 100
SLEEP = 7  # search API limit: 10 requests / minute

STORE_NAMES = {
    "dm": "dm", "dm-drogerie-markt": "dm", "rossmann": "Rossmann", "rewe": "REWE", "edeka": "EDEKA",
    "aldi": "ALDI", "aldi-sud": "ALDI Süd", "aldi-nord": "ALDI Nord", "lidl": "Lidl", "kaufland": "Kaufland",
    "netto": "Netto", "netto-marken-discount": "Netto", "penny": "PENNY", "norma": "NORMA", "real": "real",
    "globus": "Globus", "tegut": "tegut", "muller": "Müller", "budni": "Budni", "denns": "denn's",
    "alnatura": "Alnatura", "metro": "METRO",
}


def get(url):
    for attempt in range(3):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=90) as r:
                return json.load(r)
        except Exception as e:
            print(f"  retry {attempt + 1}: {e}", file=sys.stderr)
            time.sleep(20 * (attempt + 1))
    raise RuntimeError("failed: " + url)


def stores(p):
    tags = p.get("stores_tags") or []
    names = []
    for t in tags:
        n = STORE_NAMES.get(t, t.replace("-", " ").title())
        if n and n not in names:
            names.append(n)
    st = p.get("stores")
    if not names and st:
        st = ",".join(st) if isinstance(st, list) else str(st)
        names = [s.strip() for s in st.split(",") if s.strip()]
    return ", ".join(names[:6])


def certifiers(p):
    out = []
    for t in p.get("labels_tags") or []:
        if "halal" in t and t != "en:halal":
            out.append(t.split(":", 1)[-1].replace("-", " "))
    return out


def warn(msg):
    print(f"::warning::{msg}")


def page_urls(base, page):
    """Different OFF search endpoints — the first one that answers is used."""
    common = {"fields": FIELDS, "page_size": PAGE_SIZE, "page": page}
    urls = []
    if "openfoodfacts" in base:
        q = f'countries_tags:"en:{COUNTRY}" AND labels_tags:"en:halal"'
        urls.append(("search-a-licious", "https://search.openfoodfacts.org/search?" +
                     urllib.parse.urlencode({"q": q, **common})))
    urls.append(("v2", f"{base}/api/v2/search?" + urllib.parse.urlencode(
        {"countries_tags_en": COUNTRY, "labels_tags": "en:halal", **common})))
    urls.append(("facet", f"{base}/country/{COUNTRY}/label/halal/{page}.json?" + urllib.parse.urlencode(
        {"fields": FIELDS, "page_size": PAGE_SIZE})))
    urls.append(("cgi", f"{base}/cgi/search.pl?" + urllib.parse.urlencode({
        "action": "process", "json": 1, "tagtype_0": "countries", "tag_contains_0": "contains", "tag_0": COUNTRY,
        "tagtype_1": "labels", "tag_contains_1": "contains", "tag_1": "halal", **common})))
    return urls


def parse_page(d):
    prods = d.get("products") or d.get("hits") or []
    count = int(d.get("count") or 0)
    return prods, count


def fetch(name, base):
    items, page, pages, method = [], 1, 1, None
    while page <= pages:
        prods, count = [], 0
        candidates = page_urls(base, page)
        if method:
            candidates = [c for c in candidates if c[0] == method]
        for m, url in candidates:
            try:
                prods, count = parse_page(get(url))
            except Exception as e:
                warn(f"{name} {m} failed: {e}")
                continue
            if prods or method:
                method = m
                break
            warn(f"{name} {m}: 0 results")
        if page == 1:
            print(f"::notice::{name}: method={method} count={count}")
        pages = max(1, -(-count // PAGE_SIZE))
        print(f"{name}: page {page}/{pages} ({count} total)")
        for p in prods:
            code = str(p.get("code") or "").strip()
            if not code.isdigit():
                continue
            nm = p.get("product_name_de") or p.get("product_name") or p.get("product_name_en") or ""
            cert = certifiers(p)
            items.append({
                "barcode": code,
                "name": str(nm).strip(),
                "brand": (p["brands"][0] if isinstance(p.get("brands"), list) and p["brands"] else str(p.get("brands") or "")).split(",")[0].strip(),
                "status": "halal",
                "source": f"Halal label ({name})",
                "note": ("Certifier: " + ", ".join(cert)) if cert else "",
                "url": f"{base}/product/{code}",
                "stores": stores(p),
            })
        if not prods:
            break
        page += 1
        time.sleep(SLEEP)
    return items


def main():
    all_items, stats = {}, {}
    for name, base in SOURCES:
        try:
            items = fetch(name, base)
        except Exception as e:
            warn(f"{name} failed: {e}")
            items = []
        stats[name] = len(items)
        for it in items:
            all_items.setdefault(it["barcode"], it)

    products = sorted(all_items.values(), key=lambda x: (x["brand"].lower(), x["name"].lower()))
    by_store = {}
    for p in products:
        for s in filter(None, (x.strip() for x in p["stores"].split(","))):
            by_store[s] = by_store.get(s, 0) + 1
    top_stores = dict(sorted(by_store.items(), key=lambda x: -x[1])[:15])

    if not products:
        print("::error::0 products imported — all search endpoints failed")
        sys.exit(1)
    old = []
    if os.path.exists(OUT):
        old = json.load(open(OUT, encoding="utf-8")).get("products", [])
    if len(products) < 0.5 * len(old):
        print(f"❌ only {len(products)} products vs {len(old)} before — keeping old file")
        sys.exit(1)
    if products == old:
        print("✅ no changes")
        return

    out = {
        "updated": datetime.date.today().isoformat(),
        "_help": "AUTO-GENERATED weekly from Open Food Facts / Open Beauty Facts (products sold in Germany with a halal label). "
                 "Do not edit — put your own corrections in products.json (it wins).",
        "stats": {"total": len(products), "by_source": stats, "by_store": top_stores},
        "products": products,
    }
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=0)
    print(f"✅ wrote {len(products)} products · {stats} · stores {top_stores}")


if __name__ == "__main__":
    main()
