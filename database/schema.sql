-- BOOKHIVE / PART II - DATABASE - MySQL 8.0.16+
-- Use this script ONLY against a fresh development schema. No DROP DATABASE/TABLE.
-- Run schema.sql before seed-data.sql.
-- Database names, table names and column names match the Java/JPA structure defined for member 1.
CREATE DATABASE IF NOT EXISTS bookhive_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bookhive_db;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(20) NOT NULL,
  loan_limit INT NOT NULL,
  loan_days INT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_roles PRIMARY KEY (id),
  CONSTRAINT uq_roles_name UNIQUE (name),
  CONSTRAINT chk_roles_limits CHECK (loan_limit > 0 AND loan_days > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(191) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role_id BIGINT NOT NULL,
  phone VARCHAR(20),
  student_code VARCHAR(30),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until DATETIME(6),
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_users PRIMARY KEY (id),
  CONSTRAINT uq_users_email UNIQUE (email),
  CONSTRAINT uq_users_student_code UNIQUE (student_code),
  CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id),
  CONSTRAINT chk_users_attempts CHECK (failed_attempts >= 0),
  INDEX idx_users_role_enabled (role_id, enabled),
  INDEX idx_users_full_name (full_name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS authors (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  country VARCHAR(100),
  birth_year INT,
  death_year INT,
  biography TEXT,
  website VARCHAR(255),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_authors PRIMARY KEY (id),
  INDEX idx_authors_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categories (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  slug VARCHAR(120) NOT NULL,
  description TEXT,
  target_audience VARCHAR(80),
  display_order INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_categories PRIMARY KEY (id),
  CONSTRAINT uq_categories_name UNIQUE (name),
  CONSTRAINT uq_categories_slug UNIQUE (slug)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS books (
  id BIGINT NOT NULL AUTO_INCREMENT,
  isbn VARCHAR(20) NOT NULL,
  title VARCHAR(255) NOT NULL,
  edition VARCHAR(80),
  publisher VARCHAR(150),
  publication_year INT,
  language VARCHAR(50),
  description TEXT,
  cover_url VARCHAR(500),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_books PRIMARY KEY (id),
  CONSTRAINT uq_books_isbn UNIQUE (isbn),
  INDEX idx_books_title (title),
  INDEX idx_books_pub_year (publication_year),
  INDEX idx_books_active (active)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS book_authors (
  book_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  CONSTRAINT pk_book_authors PRIMARY KEY (book_id, author_id),
  CONSTRAINT fk_ba_book FOREIGN KEY (book_id) REFERENCES books(id),
  CONSTRAINT fk_ba_author FOREIGN KEY (author_id) REFERENCES authors(id),
  INDEX idx_book_authors_author (author_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS book_categories (
  book_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  CONSTRAINT pk_book_categories PRIMARY KEY (book_id, category_id),
  CONSTRAINT fk_bc_book FOREIGN KEY (book_id) REFERENCES books(id),
  CONSTRAINT fk_bc_category FOREIGN KEY (category_id) REFERENCES categories(id),
  INDEX idx_book_categories_category (category_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS book_batches (
  id BIGINT NOT NULL AUTO_INCREMENT,
  book_id BIGINT NOT NULL,
  batch_code VARCHAR(60) NOT NULL,
  quantity INT NOT NULL,
  received_at DATETIME(6) NOT NULL,
  supplier VARCHAR(150),
  created_by BIGINT NOT NULL,
  notes TEXT,
  CONSTRAINT pk_book_batches PRIMARY KEY (id),
  CONSTRAINT uq_book_batches_code UNIQUE (batch_code),
  CONSTRAINT fk_batches_book FOREIGN KEY (book_id) REFERENCES books(id),
  CONSTRAINT fk_batches_creator FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT chk_batches_quantity CHECK (quantity > 0),
  INDEX idx_batches_book_time (book_id, received_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS book_copies (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  copy_code VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
  acquired_at DATE,
  condition_note VARCHAR(500),
  removed_at DATETIME(6),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT pk_book_copies PRIMARY KEY (id),
  CONSTRAINT uq_book_copies_code UNIQUE (copy_code),
  CONSTRAINT fk_copies_batch FOREIGN KEY (batch_id) REFERENCES book_batches(id),
  CONSTRAINT chk_copies_status CHECK (status IN
      ('AVAILABLE','BORROWED','RESERVED','DAMAGED','LOST','DISCARDED')),
  CONSTRAINT chk_copies_version CHECK (version >= 0),
  INDEX idx_copies_batch_status (batch_id, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS loans (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  copy_id BIGINT NOT NULL,
  borrowed_at DATETIME(6) NOT NULL,
  due_date DATE NOT NULL,
  return_date DATE,
  status VARCHAR(20) NOT NULL,
  issued_by BIGINT,
  returned_by BIGINT,
  -- MySQL UNIQUE allows multiple NULLs; only one ACTIVE loan per physical copy.
  active_copy_id BIGINT GENERATED ALWAYS AS
    (CASE WHEN status = 'ACTIVE' THEN copy_id ELSE NULL END) STORED,
  CONSTRAINT pk_loans PRIMARY KEY (id),
  CONSTRAINT uq_loans_active_copy UNIQUE (active_copy_id),
  CONSTRAINT fk_loans_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_loans_copy FOREIGN KEY (copy_id) REFERENCES book_copies(id),
  CONSTRAINT fk_loans_issued_by FOREIGN KEY (issued_by) REFERENCES users(id),
  CONSTRAINT fk_loans_returned_by FOREIGN KEY (returned_by) REFERENCES users(id),
  CONSTRAINT chk_loans_status CHECK (status IN ('ACTIVE','RETURNED')),
  CONSTRAINT chk_loans_return CHECK
    ((status = 'ACTIVE' AND return_date IS NULL) OR
     (status = 'RETURNED' AND return_date IS NOT NULL)),
  INDEX idx_loans_user_status (user_id, status),
  INDEX idx_loans_due_status (due_date, status),
  INDEX idx_loans_borrowed_at (borrowed_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reservations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  book_id BIGINT NOT NULL,
  copy_id BIGINT,
  reserved_at DATETIME(6) NOT NULL,
  status VARCHAR(20) NOT NULL,
  hold_until DATETIME(6),
  CONSTRAINT pk_reservations PRIMARY KEY (id),
  CONSTRAINT fk_res_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_res_book FOREIGN KEY (book_id) REFERENCES books(id),
  CONSTRAINT fk_res_copy FOREIGN KEY (copy_id) REFERENCES book_copies(id),
  CONSTRAINT chk_res_status CHECK (status IN
    ('REQUESTED','WAITING','READY','FULFILLED','CANCELLED','EXPIRED')),
  INDEX idx_res_queue (book_id, status, reserved_at, id),
  INDEX idx_res_user_status (user_id, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fines (
  id BIGINT NOT NULL AUTO_INCREMENT,
  loan_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  paid_at DATETIME(6),
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  notes VARCHAR(500),
  CONSTRAINT pk_fines PRIMARY KEY (id),
  CONSTRAINT uq_fines_loan UNIQUE (loan_id),
  CONSTRAINT fk_fines_loan FOREIGN KEY (loan_id) REFERENCES loans(id),
  CONSTRAINT chk_fines_amount CHECK (amount >= 0),
  CONSTRAINT chk_fines_status CHECK (status IN ('UNPAID','PAID','WAIVED')),
  INDEX idx_fines_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  message TEXT NOT NULL,
  type VARCHAR(40) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  sent_at DATETIME(6),
  CONSTRAINT pk_notifications PRIMARY KEY (id),
  CONSTRAINT fk_notify_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_notifications_user_read (user_id, is_read, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  actor_id BIGINT,
  action VARCHAR(60) NOT NULL,
  entity_type VARCHAR(60) NOT NULL,
  entity_id BIGINT,
  details TEXT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_audit_logs PRIMARY KEY (id),
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id),
  INDEX idx_audit_entity (entity_type, entity_id),
  INDEX idx_audit_actor_time (actor_id, created_at)
) ENGINE=InnoDB;
