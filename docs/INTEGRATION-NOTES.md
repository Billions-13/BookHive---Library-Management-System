# Integration notes (Member 3)

What Member 3 has added, and the small changes the other members need to make so it works on every page. Member 3 does not edit Java code, templates or CSS owned by Members 1 and 2.

## Added by Member 3

| File | Purpose |
| --- | --- |
| [Dockerfile](../Dockerfile), [.dockerignore](../.dockerignore), [railway.json](../railway.json) | Build and deploy on Railway |
| [static/js/app.js](../src/main/resources/static/js/app.js) | Vietnamese validation messages, confirmation before destructive actions, no double submit, `BookHive.fetchJson()` |
| [static/librarian/dashboard.html](../src/main/resources/static/librarian/dashboard.html), [static/js/dashboard.js](../src/main/resources/static/js/dashboard.js) | Chart.js statistics page at `/librarian/dashboard.html`, based on `GET /api/stats` |
| [docs/DEPLOYMENT.md](DEPLOYMENT.md) | Railway deployment steps and checks |
| [docs/BookHive.postman_collection.json](BookHive.postman_collection.json) | Postman tests: public pages, sign-in, statistics API, access rules, sign-out |

## Needed from Member 2 (templates / CSS)

1. Load the shared script on every page (once, in the shared layout if there is one):

   ```html
   <script th:src="@{/js/app.js}" defer></script>
   ```

2. Add a **Thống kê** link to `/librarian/dashboard.html` in the menu for librarians and admins.
3. `app.js` recognises the forms to confirm by their URL, so keep the existing `th:action` values (`…/archive`, `…/discard`, `…/cancel`, `…/return`, `…/toggle`, `…/resolve`, `…/copies/{id}/status`). Any other form can ask for confirmation with `data-confirm="Câu hỏi?"`.
4. Optional: `data-match="password"` on a password confirmation field makes `app.js` check that both values match; `data-pattern-message="..."` sets the message shown for a `pattern` attribute.
5. Invalid fields get the Bootstrap class `is-invalid`, so Bootstrap's red border works without extra CSS.

## Suggestions for Member 1 (backend)

1. `CatalogCsvService` accepts CSV files up to 2,000,000 bytes, but Spring's default upload limit is 1 MB. The Dockerfile raises it for Railway; for local runs add to `application.yml`:

   ```yaml
   spring:
     servlet:
       multipart:
         max-file-size: 2MB
         max-request-size: 3MB
   ```

2. The PDF asks the dashboard to show loans over time and popular books. If `/api/stats` (or a new endpoint) also returned loans per month and the most borrowed titles, `dashboard.js` can draw them.
