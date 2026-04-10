-- Clean lesson seed (run once)
-- Works for both local (lms_db) and Railway (railway)

INSERT INTO lessons (course_id, title, position, video_url, duration) VALUES
(1, 'Introduction to HTML',            1, '', '15 min'),
(1, 'HTML Semantic Elements',          2, '', '22 min'),
(1, 'CSS Box Model & Flexbox',         3, '', '35 min'),
(1, 'CSS Grid Layout',                 4, '', '28 min'),
(1, 'Responsive Design',               5, '', '30 min'),
(1, 'JS Variables & Data Types',       6, '', '20 min'),
(1, 'Functions & Arrow Functions',     7, '', '25 min'),
(1, 'DOM Manipulation',                8, '', '40 min'),
(1, 'React Introduction & Setup',      9, '', '18 min'),

(2, 'Python Basics',                   1, '', '25 min'),
(2, 'NumPy Intro',                     2, '', '30 min'),
(2, 'Pandas',                          3, '', '40 min'),
(2, 'Scikit-learn',                    4, '', '35 min'),
(2, 'Linear Regression',               5, '', '45 min'),
(2, 'Decision Trees',                  6, '', '30 min'),
(2, 'Neural Networks',                 7, '', '50 min'),
(2, 'TensorFlow Basics',               8, '', '60 min'),
(2, 'Model Deployment',                9, '', '20 min'),

(3, 'Figma Basics',                    1, '', '20 min'),
(3, 'Prototyping',                     2, '', '25 min'),
(3, 'User Research',                   3, '', '30 min'),

(4, 'AWS EC2',                         1, '', '35 min'),
(4, 'S3 Storage',                      2, '', '25 min'),
(4, 'Lambda Functions',                3, '', '40 min'),

(5, 'Flutter Intro',                   1, '', '20 min'),
(5, 'Dart Language',                   2, '', '25 min'),

(6, 'Kali Linux Setup',                1, '', '30 min'),
(6, 'Metasploit Basics',               2, '', '35 min'),

(7, 'Docker Basics',                   1, '', '25 min'),
(7, 'Kubernetes Intro',                2, '', '40 min'),

(8, 'Pandas for Data',                 1, '', '30 min'),
(8, 'Data Visualization',              2, '', '25 min'),

(9, 'React Hooks',                     1, '', '20 min'),
(9, 'Redux State Management',          2, '', '30 min');

SELECT CONCAT('Seeded ', COUNT(*), ' lessons!') AS status FROM lessons;
