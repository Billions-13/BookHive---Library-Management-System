# BookHive

BookHive is a university library management website. Students and lecturers can search for, borrow, and reserve books. Librarians manage the catalog, individual physical copies, returns, and fines. Administrators manage user accounts and roles.

The project uses **Java 17**, **Spring Boot 4.1.2**, **Spring MVC**, **Thymeleaf**, **Spring Security**, **Spring Data JPA**, **MySQL 8**, and **Maven**.

## Running on Windows

### Initial Setup

1. Install **JDK 17 or later**, **Maven**, and **MySQL Server 8.0.16 or later**. Verify your Java and Maven installations:

   powershell
   java -version
   mvn -version
   

   If you use VS Code, install the **Extension Pack for Java** and open the entire project folder containing `pom.xml`.

2. Start the MySQL service. In MySQL Workbench, connect with an account that has permission to create databases, then run [schema.sql](database/schema.sql) followed by [seed-data.sql](database/seed-data.sql). The first script creates `bookhive_db` and 14 tables; the second inserts roles, accounts, and sample data. Run the seed script **only once on a fresh database**, because it uses fixed IDs. You can run [verify.sql](database/verify.sql) to check the results.

3. Use a MySQL account with read and write permissions on `bookhive_db`. If you need to create a dedicated application account, run the following statements as a MySQL administrator, replacing the example password with your own:

   ```sql
   CREATE USER 'bookhive_app'@'localhost' IDENTIFIED BY 'replace-with-your-own-password';
   GRANT SELECT, INSERT, UPDATE, DELETE ON bookhive_db.* TO 'bookhive_app'@'localhost';
   ```

4. In PowerShell, from the project root directory, create `.env` from the example file if it does not already exist:

   ```powershell
   if (-not (Test-Path .env)) { Copy-Item .env.example .env }
   ```

   Open `.env` and set `DB_USERNAME` and `DB_PASSWORD` to your MySQL credentials. By default, `DB_URL` points to `bookhive_db` on port `3306`. The `dev` profile automatically reads this file from the project root. Git ignores `.env`. See [.env.example](.env.example).

### Starting the Application

1. Make sure the MySQL service is running. On Windows, the service is commonly named `MySQL80`.
2. Open [BookhiveApplication.java](src/main/java/com/bookhive/BookhiveApplication.java), press **F5**, and select **BookHive (dev - .env)** if VS Code prompts you to choose a launch configuration. See the [VS Code launch configuration](.vscode/launch.json).
3. When the terminal displays `Started BookhiveApplication`, open [http://localhost:8080](http://localhost:8080). Keep the application running in VS Code while using the website. Once MySQL and BookHive are running, you can access the website directly through this URL.

Alternatively, run the application from PowerShell in the directory containing `pom.xml`:

```powershell
mvn spring-boot:run
```

### Troubleshooting

| Message or Issue | Solution |
| --- | --- |
| `BUILD SUCCESS` appears after packaging | Start BookHive using **F5** or `mvn spring-boot:run`, then wait for `Started BookhiveApplication`. |
| `package org.springframework.boot does not exist` when using `javac` | Use the **BookHive (dev - .env)** launch configuration or Maven to load the dependencies declared in `pom.xml`. |
| `Access denied for user` or a MySQL connection error | Verify that the MySQL service is running and check the credentials in `.env` and the user's permissions on `bookhive_db`. |
| `Schema-validation: missing table` | Verify `DB_URL` and run `schema.sql` if the database has not been initialized. Hibernate uses `ddl-auto=validate` to validate the table structure. |
| Port `8080` is already in use | If BookHive is already running, open the website directly. If another application is using the port, set `PORT=8081` in `.env`, restart BookHive, and open the new port. |

## Demo Accounts and Access Control

After loading the sample data, open the [login page](http://localhost:8080/login). All demo accounts use the password **`BookHiveDemo!2026`** and are intended **only for local development**. See the [database design documentation](database/database-design.md).

| Role | Demo Email | Permissions |
| --- | --- | --- |
| `STUDENT` | `student@bookhive.example.test` | Borrow and reserve books; view borrowing history and fines. Sample data allows up to 5 books for 14 days. |
| `LECTURER` | `lecturer@bookhive.example.test` | Access reader features. Sample data allows up to 10 books for 30 days. |
| `LIBRARIAN` | `librarian@bookhive.example.test` | Manage books, authors, categories, acquisition batches, physical copies, loans, returns, fines, and CSV imports/exports. |
| `ADMIN` | `admin@bookhive.example.test` | All librarian permissions, plus the ability to create and edit accounts, change roles, reset passwords, and disable accounts. |

Guests can browse the catalog and register for an account. Newly registered users receive the `STUDENT` role. Signed-in users can update their profiles and change their passwords. The application uses Spring Security for authorization and CSRF protection on forms. Passwords are hashed with BCrypt. After five failed login attempts, an account is temporarily locked for 15 minutes.

## Library Features

- **Catalog search:** Search by book title, ISBN, author, category, publisher, language, and publication year. The catalog supports sorting, pagination, and displaying the number of available copies.
- **Inventory management:** Manage book titles, acquisition batches, and individual physical copies. Each batch contains between 1 and 1,000 copies. Every copy has a unique identifier, such as `COPY010191`. Librarians can mark copies as damaged, lost, or withdrawn. Copies that are currently on loan or held for reservations must be processed through their respective workflows.
- **Borrowing and returns:** Apply borrowing limits and loan periods based on user roles. Librarians can issue loans and process returns. Each physical copy can have only one active loan. Late returns incur a fine of **VND 2,000 per day**. Librarians can confirm payment or waive fines.
- **Reservations:** Users can reserve a book title when no copies are available. When a copy is returned or new stock is added, the system prioritizes the oldest reservation and holds a copy for the requester for two days. A scheduled task checks for expired holds daily at **01:15 (Vietnam time)**.
- **Activity logging:** Record actions such as adding acquisition batches, issuing loans, processing returns, and handling fines in the `audit_logs` table.

## Website Routes and API

| Route | Purpose |
| --- | --- |
| `/`, `/books`, `/books/{id}` | Home page, book catalog, and book details. |
| `/login`, `/register` | User login and registration. |
| `/my/loans`, `/my/reservations`, `/my/fines`, `/my/profile` | Reader dashboard and account pages. |
| `/librarian/books/{id}/inventory`, `/librarian/loans`, `/librarian/fines` | Inventory, loans and returns, and fine management. |
| `/librarian/catalog/csv` | Catalog CSV import and export for librarians and administrators. |
| `/admin/users` | User account management. |
| `GET /api/stats` | JSON statistics for librarians and administrators, containing `totalTitles`, `availableCopies`, `borrowedCopies`, and `overdueLoans`. |

CSV imports must use **UTF-8**, contain at most **1,000 book rows**, and include the following columns:

```csv
isbn,title,publisher,publication_year,language
```

For all routes, HTTP methods, and access requirements, see the [endpoint documentation](docs/ENDPOINTS.md).

## Source Code and Database Structure

| Directory | Description |
| --- | --- |
| `src/main/java/com/bookhive/controller` | Handles HTTP requests and returns Thymeleaf pages or JSON responses. |
| `src/main/java/com/bookhive/service` | Implements business logic, transactions, logging, and scheduled tasks. |
| `src/main/java/com/bookhive/security` | Handles authentication, authorization, and failed login tracking. |
| `src/main/java/com/bookhive/entity`, `repository`, `dto` | Defines MySQL entity mappings, data access queries, and form/API data transfer objects. |
| `src/main/resources/templates` | Contains HTML pages and Thymeleaf forms. |
| `database` | Contains database schema scripts, demo data, and verification queries. |
| `src/test`, `docs` | Contains automated tests and business documentation. |

The inventory model follows the relationship **`books` → `book_batches` → `book_copies`**. The number of copies available for borrowing is calculated from the status of each physical copy. Authors and categories are linked to book titles through two junction tables.

See the [ER diagram](database/erd.png) and [database design documentation](database/database-design.md).

## Testing and Packaging

Start MySQL and ensure that the database schema has been created. Then run Maven to execute tests and build the application:

```powershell
mvn verify
```

The current automated test verifies that the application can load the Spring context with the database configuration. Use the [business test checklist](docs/TEST_CASES.md) to manually verify additional workflows in the browser.

After packaging, run the generated JAR from the project root:

```powershell
java -jar target/bookhive-0.0.1-SNAPSHOT.jar
```

## Production Configuration

Set the following environment variables on the server: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Use a separate database schema and database account for production. Set `PORT` if your hosting platform assigns a specific port.

Email reminders are disabled by default. To enable them under the `prod` profile, set `MAIL_ENABLED=true` along with `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, and `SMTP_PASSWORD`. A scheduled task runs daily at **08:00 (Vietnam time)** to notify borrowers about active loans due within two days or already overdue. See [application-prod.yml](src/main/resources/application-prod.yml).

## Team Responsibilities

- **Member 1:** Backend development and database design/implementation.
- **Member 2:** CSS styling and UI/UX design.
- **Member 3:** JavaScript development and deployment.

## License

See [LICENSE](LICENSE) for the project's license terms.
