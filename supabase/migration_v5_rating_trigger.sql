-- Recalculate materials.rating_avg and rating_count when ratings change
create or replace function public.refresh_material_rating()
returns trigger
language plpgsql
security definer
as $$
declare
  mid uuid;
begin
  mid := coalesce(new.material_id, old.material_id);
  update public.materials m
  set
    rating_avg = coalesce((
      select avg(r.rating)::real from public.ratings r where r.material_id = mid
    ), 0),
    rating_count = (
      select count(*)::int from public.ratings r where r.material_id = mid
    )
  where m.id = mid;
  return coalesce(new, old);
end;
$$;

drop trigger if exists trg_ratings_refresh on public.ratings;
create trigger trg_ratings_refresh
after insert or update or delete on public.ratings
for each row execute function public.refresh_material_rating();

-- Backfill existing materials
update public.materials m
set
  rating_avg = coalesce((select avg(r.rating)::real from public.ratings r where r.material_id = m.id), 0),
  rating_count = coalesce((select count(*)::int from public.ratings r where r.material_id = m.id), 0);
