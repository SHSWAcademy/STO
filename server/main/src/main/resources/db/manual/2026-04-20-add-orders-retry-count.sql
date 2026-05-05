alter table public.orders
add column if not exists retry_count integer not null default 0;
