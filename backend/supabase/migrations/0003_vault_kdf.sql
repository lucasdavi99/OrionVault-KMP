-- OrionVault - parametros de KDF do cofre (Argon2id), uma linha por conta.
--
-- Existe para viabilizar a restauracao do cofre num segundo dispositivo: a chave do cofre e
-- Argon2id(Master Password + Secret Key, salt), e o salt e gerado aleatoriamente no dispositivo
-- que criou o cofre. Sem publicar esse salt (e os parametros de custo), um segundo dispositivo
-- derivaria uma chave diferente e nao conseguiria decifrar nada que baixasse do servidor.
--
-- Zero-knowledge preservado: salt e parametros de custo NAO sao segredos - o salt existe para
-- impedir rainbow tables, nao para esconder informacao. O que continua sem sair do dispositivo:
-- Master Password, Secret Key e a chave derivada delas.
--
-- Primeiro dispositivo a escrever vence: o cliente envia com
-- `Prefer: resolution=ignore-duplicates`, entao um conflito de PK e um no-op silencioso e a
-- linha jamais e sobrescrita por um dispositivo que criou um cofre novo por engano.

create table vault_kdf (
  user_id uuid primary key references auth.users(id) on delete cascade,
  version int not null,
  ops_limit bigint not null,
  mem_limit_bytes int not null,
  salt text not null,
  created_at timestamptz not null default now()
);

alter table vault_kdf enable row level security;

create policy vault_kdf_owner on vault_kdf
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- Defensivo, mesmo motivo de 0002_rls.sql: RLS ja bloquearia, mas revogamos explicitamente.
revoke all on vault_kdf from anon;
