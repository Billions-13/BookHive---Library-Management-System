# BookHive Endpoints and API Handoff

## Roles and permissions

- `STUDENT` and `LECTURER` can access their account pages, check out available copies when there is no waiting queue, and reserve titles that are out of stock.
- `LIBRARIAN` can add and edit titles, receive stock, manage physical copies, issue loans, process returns, manage fines, and import or export CSV files.
- `ADMIN` has all librarian permissions and can manage accounts. Spring Security enforces permissions on the server.
- Keep `th:action` and `method="post"` on POST forms so Spring Security can handle CSRF protection.

## URLs and methods

Staff access includes `LIBRARIAN` and `ADMIN`.

| URL | Method | Access | Purpose |
| --- | --- | --- | --- |
| `/` | GET | Public | Home page |
| `/books` | GET | Public | Catalog with seven filters, sorting, and page sizes of 10, 20, 50, and 100 |
| `/books/{id}` | GET | Public | Title details, stock availability, and borrow or reserve actions |
| `/register`, `/login` | GET/POST | Public | Registration and sign-in |
| `/logout` | POST | Signed in | Sign out |
| `/my/profile`, `/my/password` | GET/POST | Signed in | Edit a profile or change a password |
| `/my/loans` | GET | Signed in | View current loans and loan history |
| `/my/borrow/{bookId}` | POST | Student or lecturer | Check out an available copy |
| `/my/reservations` | GET | Signed in | View personal reservations |
| `/my/reservations/{bookId}` | POST | Student or lecturer | Reserve a title that is out of stock |
| `/my/reservations/{id}/cancel` | POST | Reservation owner | Cancel a reservation |
| `/my/fines` | GET | Signed in | View personal fines |
| `/librarian/books/new` | GET/POST | Staff | Create a book title |
| `/librarian/books/{id}/edit` | GET/POST | Staff | Edit a book title |
| `/librarian/books/{id}/archive` | POST | Staff | Archive a title once all open transactions have been resolved |
| `/librarian/books/{id}/inventory` | GET | Staff | View batches, physical copies, and their statuses |
| `/librarian/books/{id}/batches` | POST | Staff | Receive stock and generate a unique code for each copy |
| `/librarian/books/{id}/batches/{batchId}/discard` | POST | Staff | Discard a batch once all borrowed or held copies are cleared |
| `/librarian/books/{id}/copies/{copyId}/status` | POST | Staff | Set a copy status to `AVAILABLE`, `DAMAGED`, `LOST`, or `DISCARDED` |
| `/librarian/loans` | GET/POST | Staff | View or issue loans |
| `/librarian/loans/{id}/return` | POST | Staff | Process a return and calculate any late fine |
| `/librarian/fines` | GET | Staff | List fines |
| `/librarian/fines/{id}/resolve` | POST | Staff | Mark a fine as `PAID` or `WAIVED` |
| `/librarian/authors`, `/librarian/categories` | GET | Staff | List records with five filters, sorting, and pagination. Each resource also supports `/new`, `/{id}/edit`, and `/{id}/archive`. |
| `/librarian/catalog/csv` | GET | Staff | Open the CSV import and export page |
| `/librarian/catalog/import` | POST | Staff | Upload a CSV file. A validation error rolls back the import. |
| `/librarian/catalog/export` | GET | Staff | Export the catalog as CSV with safeguards against spreadsheet formula injection |
| `/admin/users` | GET | Admin | List users with five filters, sorting, and pagination |
| `/admin/users/new`, `/admin/users/{id}/edit` | GET/POST | Admin | Create or edit accounts, change roles, and reset passwords |
| `/admin/users/{id}/toggle` | POST | Admin | Enable or disable an account |
| `/api/stats` | GET | Staff | Return JSON statistics for use by Chart.js |
| `/api/stats/charts` | GET | Staff | Chart data: `copyStatus`, `loansPerMonth` (6 months), `topBooks` (5 titles) |
| `/librarian/dashboard` | GET | Staff | Statistics page drawn by `/js/dashboard.js` |
| `/api/books/{id}/availability` | GET | Public | `totalCopies`, `availableCopies`, `borrowedCopies`, `damagedCopies` for one title |

`GET /api/stats` returns the fields shown below. Live counts come from MySQL.

```json
{"totalTitles":31,"availableCopies":0,"borrowedCopies":0,"overdueLoans":0}
```

## Inventory rules

Each `book_batches` record stores the number of physical copies received in `quantity`. Receiving a batch creates that many `book_copies` records. Each copy gets a new `copy_code`, such as `COPY010191`. When no copies are `AVAILABLE`, the detail page shows that the title is out of stock and offers a reservation action. To replace stock with an edition that has a different ISBN, create a new title, receive the new batch, and discard the old copies once all open transactions are resolved.

New copy codes are derived from auto-increment IDs assigned by MySQL. For example, ID `10191` produces `COPY010191`. This keeps the codes unique when multiple batches are received concurrently. The codes `COPY001` and `COPY002` in the assignment illustrate the format.

## Integration notes for Members 2 and 3

- Preserve the URLs above, the form attributes `th:field`, `name`, and `th:action`, and the field IDs used by labels.
- Member 2 adds CSS and responsive layouts while preserving the Java and SQL structure and the CSRF tokens.
- Member 3 adds JavaScript using Fetch and DOM APIs, a Chart.js dashboard, CSV and email integration, and deployment. Reuse the existing borrowing and return logic.
- Page endpoints return HTML. The statistics endpoint returns JSON, and the CSV export returns a file. Agree on request and response contracts with the team before adding further AJAX interactions.

## Front-end hooks

Added during integration (Member 3). Templates turn on JavaScript behaviour with the `data-*` attributes below, handled by [app.js](../src/main/resources/static/js/app.js). When restyling a page, keep these attributes, the element IDs they point to, and the `th:action`/`th:field` attributes.

| Attribute | Element | Behaviour |
| --- | --- | --- |
| `data-confirm="message"` | `form` | Bootstrap confirmation dialog before submitting |
| `data-confirm-when-value="A,B"` | `form` with `data-confirm` | Confirm only when the form's `status` field has one of these values |
| `data-autosubmit` | `select` | Submit the filter form when the value changes |
| `data-table-filter="#tableId"` | `input` | Filter table rows by text; Vietnamese accents are ignored |
| `data-row-filter="#tableId"` | `select` | Filter table rows by their `data-status` attribute |
| `data-filter-empty="#tableId"` | any | Shown when the filters hide every row |
| `data-match="otherFieldId"` | `input` | Must equal another field (password confirmation) |
| `data-pattern-message="text"` | `input[pattern]` | Message shown when the pattern does not match |
| `data-cover-preview="#imgId"` | `input` | Live preview of a cover image URL |
| `data-slug-from="#nameId"` | `input` | Fill a URL slug from a Vietnamese name |
| `data-max-bytes`, `data-file-info`, `data-csv-preview` | CSV `input[type=file]` and siblings | Check extension, size, required columns and row count; preview the first rows |
| `data-availability-url` + `data-count` | stock counters on `/books/{id}` | Refresh counters every 30 s; warn when stock crosses zero |
| `data-autohide` | `.alert` | Close after 6 seconds |
| `data-history-back` | `button` | Go back to the previous page |

Forms with constraints (`required`, `min`, `pattern`, ...) are validated in the browser with Vietnamese messages, and the server validates them again. POST forms show a spinner and cannot be submitted twice. Success messages after an action are flash attributes named `success`; the `alerts` fragment in [layout.html](../src/main/resources/templates/fragments/layout.html) displays them.
