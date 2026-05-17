-- Run this AFTER the original schema.sql if you already ran it.
-- Fixes cloud upload (storage policies) and adds missing tables/columns.

-- Tags on materials
alter table public.materials add column if not exists tags text[] not null default '{}';

-- Allow delete (submissions removal)
drop policy if exists "materials_delete" on public.materials;
create policy "materials_delete" on public.materials for delete using (true);
grant delete on public.materials to anon, authenticated;

-- Profiles
create table if not exists public.profiles (
    id text primary key,
    display_name text not null,
    section text not null default '',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
alter table public.profiles enable row level security;
drop policy if exists "profiles_select" on public.profiles;
drop policy if exists "profiles_insert" on public.profiles;
drop policy if exists "profiles_update" on public.profiles;
create policy "profiles_select" on public.profiles for select using (true);
create policy "profiles_insert" on public.profiles for insert with check (true);
create policy "profiles_update" on public.profiles for update using (true);
grant select, insert, update on public.profiles to anon, authenticated;

-- Quiz sets / questions / history
create table if not exists public.quiz_sets (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    subject text not null,
    difficulty text not null default 'EASY',
    created_at timestamptz not null default now()
);

create table if not exists public.quiz_questions (
    id uuid primary key default gen_random_uuid(),
    quiz_set_id uuid not null references public.quiz_sets(id) on delete cascade,
    prompt text not null,
    options jsonb not null default '[]',
    correct_index int not null default 0,
    sort_order int not null default 0
);

create table if not exists public.quiz_history (
    id uuid primary key default gen_random_uuid(),
    user_id text not null,
    quiz_set_id uuid not null references public.quiz_sets(id) on delete cascade,
    score int not null,
    total int not null,
    taken_at timestamptz not null default now()
);

alter table public.quiz_sets enable row level security;
alter table public.quiz_questions enable row level security;
alter table public.quiz_history enable row level security;

create policy "quiz_sets_select" on public.quiz_sets for select using (true);
create policy "quiz_questions_select" on public.quiz_questions for select using (true);
create policy "quiz_history_select" on public.quiz_history for select using (true);
create policy "quiz_history_insert" on public.quiz_history for insert with check (true);
create policy "quiz_history_delete" on public.quiz_history for delete using (true);

grant select on public.quiz_sets to anon, authenticated;
grant select on public.quiz_questions to anon, authenticated;
grant select, insert, delete on public.quiz_history to anon, authenticated;

-- STORAGE: required for uploads to work (bucket must exist and be public)
drop policy if exists "materials_public_read" on storage.objects;
drop policy if exists "materials_anon_insert" on storage.objects;
drop policy if exists "materials_anon_update" on storage.objects;
drop policy if exists "materials_anon_delete" on storage.objects;

create policy "materials_public_read"
on storage.objects for select
using (bucket_id = 'materials');

create policy "materials_anon_insert"
on storage.objects for insert
with check (bucket_id = 'materials');

create policy "materials_anon_update"
on storage.objects for update
using (bucket_id = 'materials');

create policy "materials_anon_delete"
on storage.objects for delete
using (bucket_id = 'materials');
