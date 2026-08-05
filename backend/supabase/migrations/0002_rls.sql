-- OrionVault - Row Level Security.
-- Só o dono (auth.uid() = user_id) enxerga/modifica suas linhas. A role `anon` (a que a
-- chave pública `anon` do app usa antes do login) não deve alcançar nenhuma linha.

alter table folders enable row level security;
alter table items enable row level security;
alter table item_versions enable row level security;

create policy folders_owner on folders
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy items_owner on items
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy item_versions_owner on item_versions
  for all using (
    exists (select 1 from items where items.id = item_versions.item_id and items.user_id = auth.uid())
  )
  with check (
    exists (select 1 from items where items.id = item_versions.item_id and items.user_id = auth.uid())
  );

-- Defensivo: o Supabase às vezes concede privilégios amplos por padrão em tabelas do
-- schema `public` para `anon`/`authenticated`. RLS já bloquearia, mas revogamos explicitamente.
revoke all on folders, items, item_versions from anon;
