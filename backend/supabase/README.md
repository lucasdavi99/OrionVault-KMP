
# OrionVault — conectando ao Supabase

Guia passo a passo, **executado por uma pessoa** (nada aqui é automatizado pelo app ou pelo
build). Ao final, o app compila com a configuração de nuvem e sincroniza cofres entre
dispositivos preservando zero-knowledge.

---

## 1. Criar o projeto Supabase

1. Crie um projeto em <https://supabase.com>.
2. Em **Project Settings → API**, anote:
   - **Project URL** — algo como `https://<project-ref>.supabase.co`.
   - **`anon` public key** — a chave pública que vai embutida no app.
3. **NUNCA** copie a chave `service_role` para o app, para `secrets.properties`, para o CI de
   build do cliente ou para qualquer arquivo versionado: ela ignora RLS por completo. Ela só é
   usada em tarefas administrativas (ex.: rodar migrações via CLI) a partir da máquina de um
   desenvolvedor ou de um cofre de segredos de CI.

## 2. Aplicar o schema

Rode, nesta ordem, pelo **SQL Editor** do Supabase:

1. `migrations/0001_schema.sql`
2. `migrations/0002_rls.sql`
3. `migrations/0003_vault_kdf.sql`

Ou, usando a Supabase CLI localmente com as migrações em `backend/supabase/migrations/`:

```bash
supabase db push
```

## 3. Configurar autenticação por e-mail

1. **Authentication → Providers**: garanta que o provedor **Email** está habilitado.
2. **Authentication → Settings**: habilite **Confirm email**, para que o cadastro exija
   verificação antes do primeiro login.
3. Com "Confirm email" ligado, o endpoint `/auth/v1/signup` responde **sem sessão** (só os
   dados do usuário). Isso **não é erro**: o app trata esse caso como
   `SignUpResult.ConfirmationRequired` e a `AuthScreen` mostra "Conta criada. Confira sua caixa
   de entrada..." em vez de uma mensagem de falha. Com auto-confirm ligado, a mesma chamada
   devolve uma sessão completa e o app entra direto.

## 4. Configurar as chaves no build (sem versionar segredo)

Crie um arquivo **`secrets.properties` na raiz do repositório** (já está no `.gitignore`):

```properties
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_ANON_KEY=<chave anon publica do projeto>
```

Alternativa para CI: exportar as **variáveis de ambiente** `SUPABASE_URL` e
`SUPABASE_ANON_KEY` (o build usa o arquivo primeiro e cai para o ambiente).

Como funciona: a task Gradle `:shared:generateSupabaseConfig` (definida em
`shared/build.gradle.kts`) escreve `GeneratedSupabaseConfig.kt` dentro de
`shared/build/generated/supabaseConfig/kotlin/`, consumido por
`shared/src/commonMain/kotlin/com/cuboidestudio/orionvault/network/SupabaseConfig.kt`. Como o
arquivo mora em `build/`, ele nunca existe dentro de uma source set versionada — não há nada a
lembrar de colocar no `.gitignore`.

**Sem essas duas propriedades o build falha, de propósito**, com uma mensagem explícita
apontando para este documento. Isso é preferível a compilar silenciosamente com uma chave vazia.

## 5. Checklist de verificação de RLS (rodar uma vez, manualmente)

1. **Sem JWT, só com a anon key**, tente listar dados:
   ```bash
   curl -s "$SUPABASE_URL/rest/v1/folders?select=*" -H "apikey: $SUPABASE_ANON_KEY"
   ```
   Deve retornar lista vazia ou 401/403 — **nunca** linhas de outros usuários.
2. Faça login no app com um usuário de teste, crie uma pasta e um item, e confirme no SQL
   Editor que as linhas existem com o `user_id` correto e com as colunas `*_ciphertext`
   ilegíveis (Base64 de blobs AEAD).
3. Com o JWT do usuário A, tente ler explicitamente uma linha do usuário B
   (`?id=eq.<id-do-B>`): deve voltar 0 linhas.
4. Repita 1–3 para `items` e `item_versions`.

## 6. Rotação da chave `anon`

**Project Settings → API → Rotate anon key**. Limitação conhecida da arquitetura atual: não há
configuração over-the-air — rotacionar a chave exige **publicar uma nova versão do app** com a
chave nova embutida via o mecanismo da seção 4. A chave `anon` é, por design, semi-pública
(vai dentro do binário) e é protegida por rate limiting + RLS; a rotação só é realmente
necessária se ela vazar de um jeito que importe (ex.: abuso de quota).

## 7. O que nunca sai do dispositivo

- Master Password, Secret Key e a chave do cofre derivada delas: **nunca** trafegam.
- O que sobe: blobs AEAD (XChaCha20-Poly1305) por campo, mais os metadados que já eram texto
  claro localmente por decisão de design — `items.title` e `folders.name`.
- A tabela `vault_kdf` guarda apenas o salt e os parâmetros de custo do Argon2id — que **não são
  segredo por design** (o salt existe para inviabilizar rainbow tables, não para esconder algo) —
  e jamais a chave derivada, a Secret Key ou a Master Password; ela existe só para que um segundo
  dispositivo derive exatamente a mesma chave ao restaurar o cofre.
- A senha **da conta na nuvem** é uma credencial separada, sem nenhuma relação com a
  criptografia do cofre; ela vai para o Supabase Auth como qualquer login de e-mail/senha.

## 8. Follow-ups conhecidos (fora do escopo desta entrega)

- Job `pg_cron` para purgar fisicamente linhas com `deleted = true` antigas e versões antigas
  de `item_versions`.
- `updated_at` hoje é enviado pelo cliente (o PostgREST não expressa `now()` num PATCH). Um
  trigger `BEFORE UPDATE` no Postgres que force `updated_at = now()` deixaria o cursor de pull
  incremental imune a relógios desalinhados entre dispositivos.
- Paginação do pull (`Range` header). A v1 assume cofres pequenos, dentro do tamanho de página
  padrão do PostgREST.
- Armazenamento do refresh token no Android/iOS ainda usa SharedPreferences/NSUserDefaults —
  mesmo TODO já existente para os segredos do cofre (migrar para Keystore/Keychain).
