ALTER TABLE public.orders
ADD COLUMN IF NOT EXISTS failed_match_result_json text;
