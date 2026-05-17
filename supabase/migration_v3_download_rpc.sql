-- Run in Supabase SQL Editor: atomic download count increment
create or replace function public.increment_download_count(material_id uuid)
returns void
language plpgsql
security definer
as $$
begin
  update public.materials
  set download_count = download_count + 1
  where id = material_id;
end;
$$;

grant execute on function public.increment_download_count(uuid) to anon, authenticated;
