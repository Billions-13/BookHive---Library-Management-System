# Deploying BookHive to Railway

BookHive runs on Railway as two services in one project: the Spring Boot application, built from the [Dockerfile](../Dockerfile), and a MySQL 8 database. Railway gives the application a public HTTPS domain.

## 1. Create the services

1. In Railway, create a project, choose **Deploy from GitHub repo**, and select this repository and the `main` branch. Railway reads [railway.json](../railway.json): it builds the Dockerfile and waits for `/login` to answer before sending traffic.
2. In the same project, add a **MySQL** database service.

## 2. Load the schema and demo data

The application validates the tables at startup (`ddl-auto=validate`); it does not create them.

1. Open the MySQL service, go to **Connect**, and copy the public host, port, user and password.
2. Connect with MySQL Workbench and run, in this order:
   1. [database/schema.sql](../database/schema.sql): creates the `bookhive_db` database and its tables.
   2. [database/seed-data.sql](../database/seed-data.sql): roles, demo accounts and sample data. Run it **once**, on the empty database.
3. Optionally run [database/verify.sql](../database/verify.sql) to check the result.

## 3. Variables of the application service

`${{MySQL.…}}` are Railway reference variables that follow the database service's credentials.

| Variable | Value |
| --- | --- |
| `DB_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/bookhive_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8` |
| `DB_USERNAME` | `${{MySQL.MYSQLUSER}}` |
| `DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |

The Dockerfile already sets the `prod` profile, HTTPS-only session cookies, the 2 MB CSV upload limit and the Vietnam time zone. Railway provides `PORT`, which [application.yml](../src/main/resources/application.yml) reads. Never commit real passwords: `.env` is ignored by Git.

## 4. Public domain and HTTPS

Under the application service, open **Settings → Networking** and generate a domain. Railway issues the certificate; `server.forward-headers-strategy: framework` keeps redirects on HTTPS.

## 5. Email reminders (optional)

The reminder job (08:00 Vietnam time) e-mails readers whose loans are due within two days or overdue. To turn it on, add `MAIL_ENABLED=true`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME` and `SMTP_PASSWORD`. With Gmail use `smtp.gmail.com`, port `587`, and an App Password instead of the account password. Demo accounts (`@bookhive.example.test`) are skipped on purpose; test with an account registered with a real address.

## 6. Check the deployment

1. Import [BookHive.postman_collection.json](BookHive.postman_collection.json) into Postman, set the collection variable `baseUrl` to the public URL, and run the collection. Every request has tests; all should pass.
2. In the browser, with the demo accounts from the README (password `BookHiveDemo!2026`):
   - [ ] Home page and catalog load over HTTPS
   - [ ] Each role sees only its own pages (a student gets HTTP 403 on `/librarian/loans`)
   - [ ] Borrow, reserve, return late (a fine appears) and resolve the fine
   - [ ] Receive a batch, change a copy status, import and export CSV
   - [ ] `/librarian/dashboard.html` shows the numbers and the chart
   - [ ] After a redeploy the data is still there
   - [ ] The browser console shows no errors

## 7. Operations

- **Logs:** the application service's **Logs** tab.
- **Restarts:** the `/login` health check and the `ON_FAILURE` restart policy in railway.json.
- **Backup** before the demo: `mysqldump -h <host> -P <port> -u <user> -p --databases bookhive_db > bookhive-backup.sql`
- **Rollback:** redeploy an earlier deployment from the **Deployments** tab.
