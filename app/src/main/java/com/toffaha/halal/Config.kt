package com.toffaha.halal

/** Everything points at the GitHub repo — it is the central place for code, data and releases. */
object Config {
    const val OWNER = "MuhammadToffaha"
    const val REPO = "halal"
    const val REPO_URL = "https://github.com/$OWNER/$REPO"
    const val RAW_DATA = "https://raw.githubusercontent.com/$OWNER/$REPO/main/data/"
    const val LATEST_RELEASE_API = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
    const val USER_AGENT = "HalalScanner/1.0 (Android; $REPO_URL)"
    const val OFF = "https://world.openfoodfacts.org"
}
