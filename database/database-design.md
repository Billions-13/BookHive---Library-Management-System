# BookHive Database Design (Part II)

This design follows the University Library Management System final project brief on pages 2–4 and the checklist on pages 8–9.

- MySQL 8.0.16 or later, with the `utf8mb4` character set and the InnoDB storage engine.
- The schema has 14 tables, including two junction tables with composite primary keys, `book_authors` and `book_categories`.
- The catalog includes 31 published IT titles compiled from publisher and catalog sources. The library holdings, users, and transactions are simulated.
- The sample data contains 615 records across the 14 tables.

| Table | Sample records |
| --- | ---: |
| `roles` | 4 |
| `users` | 28 |
| `authors` | 53 |
| `categories` | 14 |
| `books` | 31 |
| `book_authors` | 55 |
| `book_categories` | 55 |
| `book_batches` | 38 |
| `book_copies` | 190 |
| `loans` | 48 |
| `reservations` | 19 |
| `fines` | 12 |
| `notifications` | 22 |
| `audit_logs` | 46 |

## Third normal form (3NF)

- `books` stores title metadata by ISBN. `book_batches` records each stock receipt and the user who received it. `book_copies` identifies each physical copy by its own code and links it to its title through the batch.
- `book_authors` and `book_categories` represent many-to-many relationships between titles, authors, and categories.
- `loans` references the borrower and the physical copy and records the loan dates. `fines` references its loan, which supplies the associated title and borrower.
- Availability is calculated from `book_copies.status`, so the title record has no stored stock counter to maintain.
- `roles` defines borrowing limits and loan periods. Multiple users can share a role through `users.role_id`.

## Business rules and constraints

- Unique constraints apply to `books.isbn`, `users.email`, `book_copies.copy_code`, and `book_batches.batch_code`.
- A unique constraint on the generated column `loans.active_copy_id` limits each copy to one `ACTIVE` loan.
- `CHECK` constraints enforce positive batch quantities, valid statuses, and nonnegative fine amounts.
- The Java `BookCopy` entity maps its batch as `@ManyToOne BookBatch batch`. The title is reached through `copy.getBatch().getBook()`. Repository queries use `batch.book.id`.
- Foreign keys enforce referential integrity. Java transactions, locking, and business rules also protect reservation processing, borrowing limits, and copies currently on loan.
- Setting a copy to `DISCARDED` keeps its batch and loan history available.
- Five titles in the seed data have zero `AVAILABLE` copies, including one title for which stock has yet to be received. These records support out-of-stock tests.

## Setup in MySQL Workbench

1. Keep the scripts in the project's `database/` folder alongside the rest of the repository.
2. Connect to MySQL 8 with an account authorized to create databases and tables. Use a fresh development database for the sample records, which have fixed IDs. Back up existing development data before running scripts against it.
3. Open [schema.sql](schema.sql) and click **Execute** (the lightning icon).
4. Open [seed-data.sql](seed-data.sql) and click **Execute**. Run it once on a fresh schema.
5. Run [verify.sql](verify.sql). Expect 14 tables, more than 50 columns, and more than 100 records in total. The queries that detect data integrity issues should return zero rows.
6. Configure Spring Boot with `DB_URL=jdbc:mysql://localhost:3306/bookhive_db` and the MySQL credentials `DB_USERNAME` and `DB_PASSWORD`. Keep `spring.jpa.hibernate.ddl-auto=validate` so Hibernate validates the schema defined by SQL.

## Local demo accounts

| Role | Email |
| --- | --- |
| Admin | `admin@bookhive.example.test` |
| Librarian | `librarian@bookhive.example.test` |
| Lecturer | `lecturer@bookhive.example.test` |
| Student | `student@bookhive.example.test` |

The shared password for local demonstration is `BookHiveDemo!2026`. The seed script stores its BCrypt hash. For email reminder tests, use a real test mailbox. The `example.test` addresses are placeholders.

Provision separate production accounts and credentials before deploying to Railway or another host. If deployment automation uses `ADMIN_EMAIL` and `ADMIN_PASSWORD` to provision an administrator, supply them through environment variables.

## Catalog sources and sample data

- [catalog_sources.csv](catalog_sources.csv) lists references for titles, ISBNs, and editions.
- Publication years come from catalog sources where available. Print and ebook formats may have different dates and ISBNs. Verify the edition when cataloging a physical copy.
- Data in `users`, `book_batches`, `book_copies`, `loans`, `reservations`, `fines`, `notifications`, and `audit_logs` is simulated.
- The sample `cover_url` values are `NULL` until the team identifies stable image URLs with verified usage rights. Member 2 can add approved covers during UI development.
- Let the SQL scripts create the tables and use Hibernate schema validation for the application.

## Database files

- [schema.sql](schema.sql) defines tables, primary and foreign keys, unique and check constraints, and indexes.
- [seed-data.sql](seed-data.sql) inserts published IT titles and sample operational data.
- [erd.mmd](erd.mmd) contains the Mermaid ER diagram for all 14 tables.
- [verify.sql](verify.sql) contains aggregate and data integrity queries.
- [catalog_sources.csv](catalog_sources.csv) records the sources for the catalog data.
