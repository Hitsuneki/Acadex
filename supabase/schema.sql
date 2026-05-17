-- Run in Supabase SQL Editor (Dashboard → SQL → New query)
-- Keeps mock data in the app; this backs real uploads, files, ratings, and comments.

-- Storage bucket (Dashboard → Storage → New bucket)
-- Name: materials
-- Public bucket: ON (so download/preview URLs work with anon key)

create table if not exists public.materials (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    description text not null default '',
    subject text not null,
    file_type text not null,
    uploader_id text,
    uploader_name text not null,
    storage_path text,
    rating_avg real not null default 0,
    rating_count int not null default 0,
    download_count int not null default 0,
    created_at timestamptz not null default now()
);

create table if not exists public.comments (
    id uuid primary key default gen_random_uuid(),
    material_id uuid not null references public.materials(id) on delete cascade,
    user_id text,
    commenter_name text not null,
    body text not null,
    created_at timestamptz not null default now()
);

create table if not exists public.ratings (
    id uuid primary key default gen_random_uuid(),
    material_id uuid not null references public.materials(id) on delete cascade,
    user_id text not null,
    user_name text,
    rating real not null check (rating >= 0.5 and rating <= 5),
    created_at timestamptz not null default now(),
    unique (material_id, user_id)
);

create table if not exists public.saved_materials (
    user_id text not null,
    material_id uuid not null references public.materials(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, material_id)
);

create index if not exists idx_materials_created_at on public.materials(created_at desc);
create index if not exists idx_comments_material_id on public.comments(material_id);
create index if not exists idx_ratings_material_id on public.ratings(material_id);

alter table public.materials enable row level security;
alter table public.comments enable row level security;
alter table public.ratings enable row level security;
alter table public.saved_materials enable row level security;

-- Development-friendly policies (tighten for production)
create policy "materials_select" on public.materials for select using (true);
create policy "materials_insert" on public.materials for insert with check (true);
create policy "materials_update" on public.materials for update using (true);

create policy "comments_select" on public.comments for select using (true);
create policy "comments_insert" on public.comments for insert with check (true);

create policy "ratings_select" on public.ratings for select using (true);
create policy "ratings_insert" on public.ratings for insert with check (true);
create policy "ratings_update" on public.ratings for update using (true);

create policy "saved_select" on public.saved_materials for select using (true);
create policy "saved_insert" on public.saved_materials for insert with check (true);
create policy "saved_delete" on public.saved_materials for delete using (true);

grant select, insert, update on public.materials to anon, authenticated;
grant select, insert on public.comments to anon, authenticated;
grant select, insert, update on public.ratings to anon, authenticated;
grant select, insert, delete on public.saved_materials to anon, authenticated;

-- Storage policies (Storage → materials → Policies), or run:
-- create policy "Public read" on storage.objects for select using (bucket_id = 'materials');
-- create policy "Anon upload" on storage.objects for insert with check (bucket_id = 'materials');
