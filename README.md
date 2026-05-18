# ACADEX
### The Student Knowledge Archive

> *"Information becomes education when it is organized."*

Acadex is a native Android application designed to close the resource gap among students. It functions as an institutional-grade academic archive where students and educators upload, discover, and engage with learning materials — structured, searchable, and built for academic collaboration.

---

## The Problem

Not all students have equal access to quality study materials. Reviewers, notes, and past exam guides live scattered across group chats, personal drives, and social media threads. There is no standardized system for student-to-student academic sharing. Students waste time hunting for materials instead of actually studying.

Acadex solves this by turning scattered student knowledge into a structured, indexed, and shared archive.

---

## Features

### Core
- **Upload & Archive** — Submit PDFs, Word documents, PowerPoint presentations, images, and plain text files to a shared academic archive. Materials are tagged by subject and searchable by anyone.
- **Browse & Search** — Real-time search and subject filtering across the full archive. Sort by newest, most downloaded, or highest rated. Switch between Row, Tile, and Compact view modes.
- **In-App File Viewer** — Preview all supported file types without leaving the app.
  - PDF pages rendered natively via `PdfRenderer`
  - DOCX paragraphs, tables, and images parsed via Apache POI
  - PPTX slides rendered with background color and positioned text via Apache POI
  - TXT files displayed as selectable, scrollable text
  - Images displayed with pinch-to-zoom via PhotoView
- **Download** — Save any material to your device's Downloads folder via the system `DownloadManager`.
- **Ratings** — One rating per user per material, 1–5 stars. Average rating is recomputed automatically by the database.
- **Comments** — Post and delete comments on any material. Your own comments can be deleted; others' cannot.
- **Saved Index** — Bookmark any material or Gutendex book for quick retrieval from your personal index.

### Books (Gutendex Integration)
- **Public Domain Book Library** — Browse and search thousands of public domain books from Project Gutenberg via the [Gutendex API](https://gutendex.com).
- **Subject Filtering** — Filter books by Fiction, Philosophy, History, Science, Poetry, Mathematics, and more.
- **In-App Reading** — Read books in PDF, HTML (WebView), or plain text format without leaving the app.
- **Save Books** — Add Gutendex books to your Saved Index alongside your uploaded materials.

### Quizzes
- **Practice Index** — Browse quiz sets organized by subject and difficulty.
- **Quiz Taking** — Answer 4-choice questions with real-time progress tracking.
- **Results & History** — See your score after each quiz. All attempts are logged to your Quiz History.

### Profile & Account
- **Profile Page** — Display name, gender, role (Student / Teacher / Other), and an About Me bio.
- **My Submissions** — View and delete your uploaded materials.
- **Saved Index** — All bookmarked materials and books in one place.
- **Quiz History** — Log of all completed quiz attempts with scores.
- **Settings** — Dark mode, notification preferences, and archive management.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | XML Layouts + ViewBinding |
| Architecture | MVVM |
| Navigation | Navigation Component + SafeArgs |
| Authentication | Firebase Authentication |
| File Storage | Supabase Storage |
| Database | Supabase (PostgreSQL) |
| Books API | Gutendex (Project Gutenberg) |
| DOCX / PPTX Parsing | Apache POI (`poi-ooxml 5.2.3`) |
| PDF Rendering | Android `PdfRenderer` (framework) |
| Image Loading | Glide + PhotoView |
| Networking | Retrofit + OkHttp |
| Async | Kotlin Coroutines + StateFlow |
| Local Persistence | SharedPreferences |

---

## Architecture Overview

```
app/
├── data/
│   ├── model/              # Data classes: ResourceFile, Comment, QuizSet, UserProfile, etc.
│   ├── repository/         # MaterialRepository, ProfileRepository, SavedRepository,
│   │                         GutendexRepository, QuizRepository
│   └── remote/             # Supabase client singleton, Gutendex Retrofit service
├── ui/
│   ├── home/               # HomeFragment + HomeViewModel
│   ├── browse/             # BrowseFragment + ViewPager2 tabs (Archive, Books)
│   ├── upload/             # UploadFragment + UploadViewModel
│   ├── detail/             # FileDetailFragment + FileDetailViewModel
│   ├── viewer/             # DocxViewer, PptxViewer, TxtViewer (custom View components)
│   ├── quiz/               # QuizFragment, QuizTakingFragment
│   ├── gutendex/           # GutendexFragment, GutendexDetailFragment
│   ├── profile/            # ProfileFragment, EditProfileFragment
│   │                         MySubmissionsFragment, SavedIndexFragment, QuizHistoryFragment
│   ├── settings/           # SettingsFragment
│   └── about/              # AboutFragment
└── adapter/
    ├── FileCardAdapter     # Handles Row / Tile / Compact ViewHolder types
    ├── CommentAdapter
    ├── QuizSetAdapter
    └── QuizHistoryAdapter
```

---

## Database Schema (Supabase)

| Table | Description |
|---|---|
| `materials` | Uploaded files — title, subject, type, uploader, storage path, rating stats |
| `comments` | Comments on materials — cascades on material delete |
| `ratings` | One row per (user, material) pair — unique constraint enforced |
| `saved_materials` | Bookmarked uploads and Gutendex books |
| `profiles` | User profile — display name, gender, role, about me |
| `quiz_sets` | Quiz metadata — title, subject, difficulty |
| `quiz_questions` | Questions linked to quiz sets |
| `quiz_history` | Completed quiz attempts per user |

Row-level security (RLS) is enabled on all tables. Rating averages are recomputed automatically by a Postgres trigger on every insert, update, or delete to the `ratings` table.

---

## Supported File Types

| Extension | MIME Type | Preview Method |
|---|---|---|
| `.pdf` | `application/pdf` | PdfRenderer (native) |
| `.docx` | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | Apache POI XWPF |
| `.pptx` | `application/vnd.openxmlformats-officedocument.presentationml.presentation` | Apache POI XSLF |
| `.jpg` / `.jpeg` | `image/jpeg` | Glide + PhotoView |
| `.png` | `image/png` | Glide + PhotoView |
| `.txt` | `text/plain` | Native TextView |

---

## Setup

### Prerequisites
- Android Studio Hedgehog or newer
- Android SDK 26+ (minSdk 26)
- A Firebase project with Email/Password Authentication enabled
- A Supabase project with the schema applied

### 1. Clone the repository
```bash
git clone https://github.com/yourusername/acadex.git
cd acadex
```

### 2. Configure credentials

Create or edit `local.properties` in the project root and add:

```
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

Place your `google-services.json` (from the Firebase console) in the `app/` directory.

### 3. Apply the Supabase schema

Run the SQL files in your Supabase SQL Editor in this order:
1. `acadex_schema_initial.sql` — core tables, RLS policies, indexes
2. `acadex_schema_additions.sql` — profiles, quiz tables, quiz history, rating trigger

### 4. Configure the Storage bucket

In your Supabase dashboard, go to Storage and create a public bucket named `materials`. Apply the public read and authenticated insert policies.

### 5. Build and run

Open the project in Android Studio and run on a device or emulator running API 26 or above.

---

## Roadmap

- [ ] AI Summarizer — generate indexed summaries from uploaded files
- [ ] AI Quiz Generator — auto-generate quiz questions from uploaded documents
- [ ] In-app PDF annotation
- [ ] Push notifications for new uploads in followed subjects
- [ ] Cross-device sync and persistent accounts
- [ ] Web version

---

## Brand

**Name:** ACADEX — a combination of *Academic*, *Index*, and *Codex*

**Philosophy:** Information becomes education when it is organized.

**Palette:**
- Deep Navy `#0F172A`
- Electric Blue `#2563EB`
- Cyan `#06B6D4`
- White `#FFFFFF`

**Typography:** Sora (display) · Source Sans Pro (body)

---

## License

This project was developed as a capstone project and is intended for academic and educational use. Not affiliated with any academic institution.

© 2025 Acadex. All rights reserved.
