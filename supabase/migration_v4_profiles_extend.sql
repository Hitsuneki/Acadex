-- Extend profiles with bio fields (run in Supabase SQL Editor)
alter table public.profiles
    add column if not exists about_me text not null default '',
    add column if not exists gender text not null default '',
    add column if not exists status text not null default 'student';
