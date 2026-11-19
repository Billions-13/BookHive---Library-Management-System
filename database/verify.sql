USE bookhive_db;

-- 1. EXPECTED: 14 tables, 50+ columns, at least 2 many-to-many junction tables.
SELECT COUNT(*) AS total_tables FROM information_schema.tables
 WHERE table_schema='bookhive_db' AND table_type='BASE TABLE';
SELECT COUNT(*) AS total_columns FROM information_schema.columns
 WHERE table_schema='bookhive_db';
SELECT COUNT(*) AS total_foreign_keys FROM information_schema.table_constraints
 WHERE table_schema='bookhive_db' AND constraint_type='FOREIGN KEY';

-- 2. Record counts for EVERY main table.
SELECT 'roles' table_name, COUNT(*) total FROM roles UNION ALL
SELECT 'users',COUNT(*) FROM users UNION ALL
SELECT 'authors',COUNT(*) FROM authors UNION ALL
SELECT 'categories',COUNT(*) FROM categories UNION ALL
SELECT 'books',COUNT(*) FROM books UNION ALL
SELECT 'book_authors',COUNT(*) FROM book_authors UNION ALL
SELECT 'book_categories',COUNT(*) FROM book_categories UNION ALL
SELECT 'book_batches',COUNT(*) FROM book_batches UNION ALL
SELECT 'book_copies',COUNT(*) FROM book_copies UNION ALL
SELECT 'loans',COUNT(*) FROM loans UNION ALL
SELECT 'reservations',COUNT(*) FROM reservations UNION ALL
SELECT 'fines',COUNT(*) FROM fines UNION ALL
SELECT 'notifications',COUNT(*) FROM notifications UNION ALL
SELECT 'audit_logs',COUNT(*) FROM audit_logs;

-- 3. Books with author/category, per-copy inventory and out-of-stock status.
SELECT b.id,b.title,b.isbn,COUNT(DISTINCT c.id) AS copies_total,
  COUNT(DISTINCT CASE WHEN c.status='AVAILABLE' THEN c.id END) AS available,
  IF(COUNT(DISTINCT CASE WHEN c.status='AVAILABLE' THEN c.id END)=0,
    'HIEN KHONG CO SACH DE MUON','CON SACH') AS availability
 FROM books b LEFT JOIN book_batches bt ON bt.book_id=b.id
 LEFT JOIN book_copies c ON c.batch_id=bt.id
 GROUP BY b.id,b.title,b.isbn ORDER BY b.id;

-- 4. There must be NO duplicated physical copy codes.
SELECT copy_code,COUNT(*) n FROM book_copies GROUP BY copy_code HAVING n>1;

-- 5. There must be NO copy actively borrowed twice.
SELECT copy_id,COUNT(*) n FROM loans WHERE status='ACTIVE'
 GROUP BY copy_id HAVING n>1;

-- 6. All active loans must have BORROWED copies; return zero rows.
SELECT l.id AS bad_loan_id,c.copy_code,c.status
 FROM loans l JOIN book_copies c ON c.id=l.copy_id
 WHERE l.status='ACTIVE' AND c.status<>'BORROWED';

-- 7. All READY reservations must have RESERVED copies; return zero rows.
SELECT r.id AS bad_reservation_id,c.copy_code,c.status
 FROM reservations r LEFT JOIN book_copies c ON c.id=r.copy_id
 WHERE r.status='READY' AND (c.id IS NULL OR c.status<>'RESERVED');

-- 8. Each batch must have exactly its declared number of physical copies; return zero rows.
SELECT b.id,b.batch_code,b.quantity,COUNT(c.id) actual_copies
 FROM book_batches b LEFT JOIN book_copies c ON c.batch_id=b.id
 GROUP BY b.id,b.batch_code,b.quantity
 HAVING actual_copies<>b.quantity;

-- 9. 3NF: title is found through batch, not redundantly stored in book_copies.
SELECT c.id AS copy_id,b.book_id AS derived_book_id
 FROM book_copies c JOIN book_batches b ON c.batch_id=b.id LIMIT 5;

-- 10. Demo of overdue loan/fine relationship.
SELECT l.id,l.due_date,u.full_name,c.copy_code,
       DATEDIFF(CURDATE(),l.due_date) AS overdue_days,f.amount,f.status AS fine_status
 FROM loans l JOIN users u ON l.user_id=u.id
 JOIN book_copies c ON l.copy_id=c.id
 LEFT JOIN fines f ON f.loan_id=l.id
 WHERE l.status='ACTIVE' AND l.due_date<CURDATE()
 ORDER BY l.due_date;

-- 11. Book catalog and multi-author query.
SELECT b.id,b.title,GROUP_CONCAT(DISTINCT a.name ORDER BY a.name SEPARATOR ', ') authors,
       GROUP_CONCAT(DISTINCT cat.name ORDER BY cat.name SEPARATOR ', ') categories
 FROM books b LEFT JOIN book_authors ba ON ba.book_id=b.id
 LEFT JOIN authors a ON ba.author_id=a.id
 LEFT JOIN book_categories bc ON bc.book_id=b.id
 LEFT JOIN categories cat ON bc.category_id=cat.id
 GROUP BY b.id,b.title ORDER BY b.id;
