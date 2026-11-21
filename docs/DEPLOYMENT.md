# Deploying BookHive to Railway

BookHive runs on Railway as two services in one project: the Spring Boot application (built from the [Dockerfile](../Dockerfile)) and a MySQL 8 database. Railway gives the application a public domain with HTTPS.

## 1. Before deploying

1. Merge the release branch into `main` through a reviewed pull request.
2. Run the automated tests locally. They use an in-memory H2 database, so MySQL is not needed:

   ```powershell
   mvn verify
   ```

3. Build the image locally if Docker is available (optional): `docker build -t bookhive .`

## 2. Create the services

1. In Railway, create a project and choose **Deploy from GitHub repo**, then select this repository and the `main` branch. Railway reads [railway.json](../railway.json): it builds the Dockerfile and checks `/login` before routing traffic.
2. In the same project, add a **MySQL** database service.

## 3. Load the schema and demo data

The application validates the schema at startup (`ddl-auto=validate`) and does not create tables.

1. Open the MySQL service, go to **Connect**, and copy the public connection details (host, port, user, password).
2. Connect with MySQL Workbench or the `mysql` client and run, in this order:
   1. [database/schema.sql](../database/schema.sql): creates the `bookhive_db` database and its 14 tables.
   2. [database/seed-data.sql](../database/seed-data.sql): roles, demo accounts and sample data. Run it **once**, on the empty database.
3. Optionally run [database/verify.sql](../database/verify.sql) to check the data.

## 4. Configure the application service

Under the application service, open **Variables** and add the following. `${{MySQL.…}}` are Railway reference variables; they follow the database service's credentials.

| Variable | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` (already set in the Dockerfile) |
| `DB_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/bookhive_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8` |
| `DB_USERNAME` | `${{MySQL.MYSQLUSER}}` |
| `DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `MAIL_ENABLED` | `false` until SMTP is configured (see section 6) |

Railway sets `PORT` itself; [application.yml](../src/main/resources/application.yml) reads it. Do not commit real values: `.env` is ignored by Git, and [.env.example](../.env.example) lists the names only.

## 5. Public domain and HTTPS

1. Under the application service, open **Settings → Networking** and generate a domain. Railway issues the HTTPS certificate.
2. The application trusts Railway's forwarded headers (`server.forward-headers-strategy: framework`), so redirects stay on HTTPS. Session cookies are `Secure` and `SameSite=Lax` in the `prod` profile.

## 6. Email reminders (advanced feature)

The reminder job runs every day at 08:00 (Vietnam time). It e-mails readers whose active loans are due within two days or already overdue, and records a notification so each message is only sent once.

To enable it, set these variables on the application service. With Gmail, use an [App Password](https://support.google.com/accounts/answer/185833), not the account password.

| Variable | Example |
| --- | --- |
| `MAIL_ENABLED` | `true` |
| `SMTP_HOST` | `smtp.gmail.com` |
| `SMTP_PORT` | `587` |
| `SMTP_USERNAME` | the sending address |
| `SMTP_PASSWORD` | the App Password |

Demo accounts use `@bookhive.example.test` addresses, which the job skips on purpose. To see a real e-mail, register an account with your own address, borrow a book, and set its due date in the database to tomorrow.

## 7. Check the deployment

Open the public URL and go through this list with the demo accounts (password `BookHiveDemo!2026`, see the README):

- [ ] The home page and catalog load over HTTPS without warnings
- [ ] Sign in as student, lecturer, librarian and admin; each sees only their own menus
- [ ] Borrow, reserve, return (late return creates a fine), and resolve a fine
- [ ] Receive a batch, change a copy status, discard a batch
- [ ] Import and export CSV; open **Thủ thư → Thống kê** and check that the charts load
- [ ] Redeploy the service and confirm that the data is still there
- [ ] The browser console shows no errors on the pages above

## 8. Operations

- **Logs:** the application service's **Logs** tab (Spring Boot logs to stdout).
- **Uptime:** the `/login` health check restarts a failed deployment; an external monitor (for example UptimeRobot) can ping the public URL every 5 minutes.
- **Backup:** before the demo and after important changes, export the database:

  ```powershell
  mysqldump -h <host> -P <port> -u <user> -p --databases bookhive_db > bookhive-backup.sql
  ```

- **Rollback:** redeploy an earlier successful deployment from the **Deployments** tab.
