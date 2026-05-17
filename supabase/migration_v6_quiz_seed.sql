-- Seed practice quizzes: Networking, Programming, Cybersecurity
-- Safe to re-run: removes prior seed rows first

delete from public.quiz_questions
where quiz_set_id in (
  'a1000001-0000-4000-8000-000000000001',
  'a1000001-0000-4000-8000-000000000002',
  'a1000001-0000-4000-8000-000000000003'
);

delete from public.quiz_sets
where id in (
  'a1000001-0000-4000-8000-000000000001',
  'a1000001-0000-4000-8000-000000000002',
  'a1000001-0000-4000-8000-000000000003'
);

insert into public.quiz_sets (id, title, subject, difficulty) values
  ('a1000001-0000-4000-8000-000000000001', 'Networking Fundamentals', 'Networking', 'EASY'),
  ('a1000001-0000-4000-8000-000000000002', 'Programming Basics', 'Programming', 'EASY'),
  ('a1000001-0000-4000-8000-000000000003', 'Cybersecurity Essentials', 'Cybersecurity', 'MEDIUM');

insert into public.quiz_questions (quiz_set_id, prompt, options, correct_index, sort_order) values
  ('a1000001-0000-4000-8000-000000000001', 'Which layer of the OSI model handles routing?', '["Physical","Network","Transport","Application"]'::jsonb, 1, 1),
  ('a1000001-0000-4000-8000-000000000001', 'What does IP stand for?', '["Internet Protocol","Internal Port","Integrated Process","Input Packet"]'::jsonb, 0, 2),
  ('a1000001-0000-4000-8000-000000000001', 'Which port is commonly used for HTTPS?', '["80","443","21","53"]'::jsonb, 1, 3),
  ('a1000001-0000-4000-8000-000000000001', 'A MAC address operates at which layer?', '["Layer 2 (Data Link)","Layer 3 (Network)","Layer 4 (Transport)","Layer 7 (Application)"]'::jsonb, 0, 4),
  ('a1000001-0000-4000-8000-000000000001', 'What device connects multiple networks and forwards packets?', '["Hub","Switch","Router","Repeater"]'::jsonb, 2, 5),
  ('a1000001-0000-4000-8000-000000000002', 'Which keyword declares a constant in Kotlin?', '["var","val","const val","static"]'::jsonb, 2, 1),
  ('a1000001-0000-4000-8000-000000000002', 'What is the time complexity of binary search?', '["O(n)","O(log n)","O(n²)","O(1)"]'::jsonb, 1, 2),
  ('a1000001-0000-4000-8000-000000000002', 'Which structure uses FIFO order?', '["Stack","Queue","Tree","Graph"]'::jsonb, 1, 3),
  ('a1000001-0000-4000-8000-000000000002', 'Git command to create a new branch?', '["git merge","git checkout -b","git push","git init"]'::jsonb, 1, 4),
  ('a1000001-0000-4000-8000-000000000002', 'OOP principle that hides implementation details?', '["Inheritance","Encapsulation","Polymorphism","Abstraction"]'::jsonb, 1, 5),
  ('a1000001-0000-4000-8000-000000000003', 'What is phishing?', '["Hardware failure","Social engineering attack","Network upgrade","Data compression"]'::jsonb, 1, 1),
  ('a1000001-0000-4000-8000-000000000003', 'Which is stronger for password storage?', '["Plain text","MD5 only","bcrypt/Argon2 hashing","Base64 encoding"]'::jsonb, 2, 2),
  ('a1000001-0000-4000-8000-000000000003', 'HTTPS primarily protects against?', '["Only viruses","Eavesdropping on transit","Slow CPUs","Disk failure"]'::jsonb, 1, 3),
  ('a1000001-0000-4000-8000-000000000003', 'Principle of giving users only needed access?', '["Least privilege","Open access","Default allow","Shared accounts"]'::jsonb, 0, 4),
  ('a1000001-0000-4000-8000-000000000003', '2FA adds security by requiring?', '["Two passwords only","A second verification factor","Two email accounts","Two IP addresses"]'::jsonb, 1, 5);
