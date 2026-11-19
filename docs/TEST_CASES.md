# BookHive Business Test Checklist

Run all 15 scenarios on Windows with Spring Boot connected to a MySQL development database. Back up the database before running scenarios that write data. Record the observed results for each scenario.

| # | Test case | Expected result |
| --- | --- | --- |
| 1 | Open `/` and `/books` while signed out | Both pages are accessible |
| 2 | Search by title, ISBN, author, category, publisher, language, and year | Results match the filters. Titles with multiple authors appear once. |
| 3 | Change the sort order, try page sizes of 10, 20, 50, and 100, and use Next and Previous | Results sort and paginate correctly |
| 4 | Register with a new email address and sign in | The password is stored as a BCrypt hash and the account receives the `STUDENT` role |
| 5 | Register with a duplicate email address or sign in with an incorrect password | The application displays an appropriate error |
| 6 | Sign in with an incorrect password five times, then retry | The account is locked for 15 minutes. A successful sign-in after the lock expires resets the attempts. |
| 7 | As a student, open `/admin/users` or `/librarian/loans` | The server responds with HTTP 403 |
| 8 | As a librarian, create, edit, and archive titles, then attempt to archive a title with an active loan | Eligible titles update correctly. The server rejects archiving while a transaction is open. |
| 9 | Receive a batch of three copies | The batch history contains the receipt and exactly three unique copy codes are created |
| 10 | Attempt to mark a borrowed copy as damaged or discard it | The server rejects the change and preserves the loan history |
| 11 | Reach the student borrowing limit and attempt another checkout | The server rejects the checkout. Each copy has at most one `ACTIVE` loan. |
| 12 | Check out the last `AVAILABLE` copy | The catalog shows `OUT OF STOCK` and further checkouts are blocked |
| 13 | Reserve an out-of-stock title, then return a copy or receive a new batch | The oldest reservation becomes `READY` and its copy becomes `RESERVED` |
| 14 | Return a book late and resolve the fine | The loan becomes `RETURNED`, the fine is calculated at VND 2,000 per day, and staff can mark it `PAID` or `WAIVED` |
| 15 | Import a valid CSV, try a CSV with a duplicate ISBN, export CSV, and open `/api/stats` | The valid import succeeds and the invalid import rolls back. CSV and JSON output reflect MySQL data. |

Run [database verification queries](../database/verify.sql) to check table and column counts, duplicate copy codes, multiple active loans for a copy, ready reservations without a held copy, and discrepancies in batch quantities. Check backend logs when a test fails and capture screenshots for the report.

At the original handoff, the Maven build and end-to-end MySQL checks were pending because the test environment could not download Maven dependencies and lacked MySQL. Run the manual scenarios above before delivery.
