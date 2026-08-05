-- OrionVault - schema de sync em nuvem (Supabase/Postgres).
--
-- Zero-knowledge: nenhuma coluna aqui contém texto claro sensível. Os campos cifrados
-- (username/email/password/url/notes) chegam como blobs AEAD XChaCha20-Poly1305 produzidos
-- no dispositivo com a chave derivada da Master Password + Secret Key, que NUNCA saem do
-- cliente. As únicas colunas em texto claro são `items.title` e `folders.name`, que já eram
-- texto claro no banco local por decisão de design.
--
-- Formato das colunas cifradas: `text` contendo Base64 (padrão RFC 4648) do nonce e do
-- ciphertext, separados em duas colunas (`*_ciphertext` e `*_nonce`). O cliente armazena
-- localmente como `Base64(nonce).Base64(ciphertext)` (ver crypto/CipherBlob.kt) e apenas
-- separa as duas metades ao enviar - nenhuma re-cifragem acontece no caminho de upload.

create extension if not exists pgcrypto;

create table folders (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  parent_id uuid references folders(id) on delete cascade,
  name text not null,
  depth int not null,
  version int not null default 1,
  deleted boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index folders_user_id_idx on folders(user_id);
create index folders_user_updated_idx on folders(user_id, updated_at);

create table items (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  folder_id uuid references folders(id) on delete set null,
  title text not null,
  username_ciphertext text,
  username_nonce text,
  email_ciphertext text,
  email_nonce text,
  password_ciphertext text not null,
  password_nonce text not null,
  url_ciphertext text,
  url_nonce text,
  notes_ciphertext text,
  notes_nonce text,
  version int not null default 1,
  deleted boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index items_user_id_idx on items(user_id);
create index items_user_updated_idx on items(user_id, updated_at);
create index items_folder_id_idx on items(folder_id);

-- Histórico de versões por item: existe para que a UI de conflito consiga mostrar
-- "o que o outro dispositivo escreveu", então carrega a mesma fidelidade de `items`.
create table item_versions (
  item_id uuid not null references items(id) on delete cascade,
  version int not null,
  title text not null,
  username_ciphertext text,
  username_nonce text,
  email_ciphertext text,
  email_nonce text,
  password_ciphertext text not null,
  password_nonce text not null,
  url_ciphertext text,
  url_nonce text,
  notes_ciphertext text,
  notes_nonce text,
  device_id text not null,
  created_at timestamptz not null default now(),
  primary key (item_id, version)
);

-- Follow-up (fora do escopo desta fase): agendar um job pg_cron para purgar fisicamente
-- linhas com `deleted = true` mais antigas que N dias, e versões antigas em item_versions.
