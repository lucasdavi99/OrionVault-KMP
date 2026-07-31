# Documento de Design — App Cofre de Senhas

> Documento de referência técnica e de produto. Deve ser atualizado conforme decisões evoluem. Última atualização: 30/07/2026.

---

## 1. Visão Geral e Escopo

**Objetivo:** Cofre de senhas multiplataforma, offline-first por padrão, com opção de sincronização em nuvem para assinantes.

**Plataformas:** Mobile e Desktop via Compose Multiplatform (CMP). Sem versão browser na v1.

**Públicos-alvo:**
- Usuário comum: organização simples, busca rápida.
- Usuário técnico: hierarquia mais profunda de pastas (ex.: Projeto → Banco de Dados → MySQL/Oracle/PostgreSQL).

**Fora de escopo na v1:**
- Autofill nativo (Android AutofillService / iOS-macOS Password AutoFill Extension)
- Armazenamento de TOTP/2FA
- Verificação de senha vazada (breach monitoring)
- Compartilhamento entre usuários (family/team plans)
- Versão web/browser

---

## 2. Modelo de Ameaça

**Premissa central:** Zero-knowledge. O backend nunca tem acesso às senhas em texto claro, apenas a blobs cifrados.

**Cenários cobertos:**
- Comprometimento do backend/banco de dados → atacante obtém apenas dados cifrados, sem visibilidade de senhas.
- Dispositivo perdido/roubado → mitigado por bloqueio automático, biometria e cifragem local.
- Ataque de força bruta contra master password → mitigado por KDF de custo elevado (Argon2id).
- Interceptação de arquivo de export → mitigado por cifragem própria do arquivo (AES-256-GCM) + passphrase de alta entropia.

**Fora do modelo de proteção (aceito como limitação):**
- Perda da master password sem Secret Key salvo → dados no cofre local irrecuperáveis (mitigado parcialmente pela sincronização em nuvem, para usuários PRO).
- Dispositivo comprometido por malware/keylogger no momento do uso (fora do escopo de qualquer cofre de senhas).

---

## 3. Arquitetura de Segurança e Criptografia

### 3.1 Derivação de chave (estilo 1Password)

- No cadastro, o app gera uma **Secret Key** aleatória (alta entropia, ex. 128+ bits), armazenada apenas no dispositivo do usuário — nunca no servidor.
- A chave de criptografia do cofre é derivada da combinação: **Master Password + Secret Key**, via **Argon2id**.
- A senha de login da conta (autenticação do backend, apenas para usuários PRO) é um segredo **totalmente separado** — nunca participa da derivação da chave de criptografia.

### 3.2 Parâmetros do KDF (Argon2id)

- Parâmetros (memória, iterações, paralelismo) devem ser definidos com base em benchmark de dispositivo-alvo mínimo, e **versionados** — mudanças futuras de custo não podem invalidar cofres já existentes.
- Salt único por usuário/cofre, gerado aleatoriamente.

### 3.3 Cifragem dos dados

- Cofre local: **XChaCha20-Poly1305** (AEAD — autenticação integrada, detecta corrupção/adulteração), via libsodium (`crypto_aead_xchacha20poly1305_ietf`).
- **Atualizado em 30/07/2026:** a escolha original era AES-256-GCM. Na implementação do núcleo local optou-se por XChaCha20-Poly1305 porque o AES-256-GCM do libsodium (`crypto_aead_aes256gcm`) só é seguro/eficiente com suporte de hardware AES-NI, não garantido em todos os dispositivos ARM/mobile — sem isso, cairia numa implementação por software mais lenta e potencialmente vulnerável a ataques de timing. XChaCha20-Poly1305 é seguro em software puro em qualquer plataforma e usa nonce de 192 bits, grande o suficiente para ser gerado aleatoriamente sem risco prático de colisão (dispensando gestão de contador). Ver decisão registrada na seção 12.
- Cada blob cifrado usa nonce único de 24 bytes, gerado aleatoriamente, nunca reaproveitado.

### 3.4 Recovery da master password

- Fluxo estilo 1Password: usuário deve guardar a Secret Key (exibida uma vez no cadastro, recomendado impressão/anotação física ou salvamento seguro).
- Sem a Secret Key **e** a Master Password corretas, não há recuperação — isso deve estar explícito na UI de onboarding.
- 2FA e confirmação de email/celular protegem o **acesso à conta** (login), não substituem nem recuperam a chave de criptografia do cofre.

---

## 4. Modelo de Dados e Estrutura

### 4.1 Hierarquia de pastas

- Estrutura em árvore (pastas dentro de pastas, contas dentro de pastas), inspirada em sistema de arquivos.
- **Limite de profundidade de aninhamento a definir** (sugestão inicial: 4–5 níveis), para evitar recursão sem teto em sync, export e contagem de itens.
- Avaliar se pastas "travadas" por limite de plano (ver seção 7) propagam o bloqueio em cascata para subpastas.

### 4.2 Esquema de item (conta)

Campos mínimos sugeridos:
- Nome/título
- Usuário/login
- Senha (cifrada)
- URL associada
- Notas (cifradas)
- Pasta/localização na árvore
- Metadados: data de criação, data de última modificação, versão do item

### 4.3 Versionamento

- Cada item mantém histórico de versões (não apenas last-write-wins).
- Base para resolução de conflitos de sincronização (ver seção 6.2).

---

## 5. Armazenamento Local (Offline-First)

- Padrão do app: 100% local, sem qualquer dado enviado à nuvem.
- Armazenamento seguro por plataforma:
  - Android: Keystore (hardware-backed quando disponível)
  - iOS/macOS: Keychain
  - Windows: DPAPI
  - Linux: libsecret/gnome-keyring (ou mecanismo próprio adicional, a avaliar)
- Bloqueio automático por inatividade.
- Suporte a biometria (Face ID / Touch ID / impressão digital) para desbloqueio, mantendo a master password como fallback obrigatório.

---

## 6. Sincronização em Nuvem (PRO)

### 6.1 Fluxo de ativação

- Ao ativar o modo online, ocorre sync único inicial de todos os itens locais para a nuvem.
- A partir daí, sincronização automática e incremental a cada criação/edição.

### 6.2 Resolução de conflitos — versionamento

- **Decisão fechada:** conflitos são resolvidos via versionamento (não last-write-wins silencioso).
- Cada edição gera uma nova versão do item; ao detectar edições concorrentes em dispositivos diferentes, o sistema preserva o histórico e evita sobrescrita silenciosa de dados.
- A definir: comportamento de UI no momento do conflito (merge automático quando possível vs. notificação ao usuário para escolha manual).

### 6.3 Retenção pós-cancelamento

- Dados mantidos na nuvem por até 45 dias após não-renovação.
- Notificação por e-mail informando exclusão em 48 horas ao final do prazo.
- Avaliar botão de "exclusão imediata" a pedido do usuário (relevante para conformidade com LGPD, direito de exclusão).

---

## 7. Modelo de Planos (Free vs. PRO)

### 7.1 Free

- Armazenamento exclusivamente local.
- Limite de quantidade de contas e pastas (valor a definir).

### 7.2 PRO

- Criação ilimitada de contas e pastas.
- Armazenamento e sincronização em nuvem.
- Cadastro de usuário (conta), autenticação separada da master password, com 2FA e confirmação de email/celular.

### 7.3 Regras de downgrade (cancelamento)

- **Decisão fechada:** quantidade de itens existentes não diminui ao cancelar, mas novas criações ficam bloqueadas acima do limite free.
- Exemplo: usuário com 50 itens cancela → mantém 50, mas não pode criar o 51º.
- Se deletar um item (50 → 49), **não pode** recriar para voltar a 50 — a vaga não é "recuperada", o teto é a contagem atual sempre que abaixo do limite anterior.
- A definir: comportamento de subpastas quando a pasta-pai está "travada" por limite.

---

## 8. Export/Import Criptografado (Free — troca de dispositivo)

### 8.1 Segredo de proteção do arquivo

- Passphrase gerada aleatoriamente, estilo BIP39 (8–10 palavras de wordlist), para alta entropia com boa usabilidade de digitação/anotação.
- A passphrase **não** é usada diretamente como chave — passa por Argon2id para derivar a chave AES-256.

### 8.2 Cifragem do arquivo

- AES-256-GCM sobre o arquivo completo (estrutura + itens).
- Salt (Argon2id) e IV/nonce (AES-GCM) únicos por export.
- Estrutura sugerida do arquivo: `[versão do formato] [salt] [parâmetros Argon2id] [IV] [ciphertext] [auth tag]`.
- Formato versionado, para permitir mudança futura de parâmetros do KDF sem quebrar exports antigos.

### 8.3 Transporte da passphrase

- Fluxo principal: QR code para transferência direta entre dispositivos próximos.
- Fluxo alternativo: arquivo + passphrase anotável, para backup guardado (Drive pessoal, e-mail para si mesmo, etc.).

### 8.4 Comportamento na importação — **em aberto**

- Definir se a importação faz **substituição total** do cofre local ou **merge** com os itens existentes, especialmente relevante quando o dispositivo novo já possui itens antes da importação.

---

## 9. Autenticação de Conta (PRO)

- Cadastro de usuário isolado da criptografia do cofre.
- Login com e-mail/senha + 2FA.
- Confirmação de e-mail e/ou celular no cadastro.
- Reforça-se: nenhum desses mecanismos participa da derivação da chave de criptografia (ver seção 3.1).

---

## 10. Roadmap / Fora do MVP

- Autofill nativo por plataforma
- Armazenamento de TOTP/2FA
- Verificação de senha vazada/reutilizada (breach monitoring)
- Compartilhamento entre usuários (family/team)
- Auditoria de segurança externa
- Avaliação de código aberto (ao menos das camadas de criptografia)
- Conformidade LGPD/GDPR formalizada (política de privacidade, base legal de retenção)

---

## 11. Decisões em Aberto

| # | Questão | Status |
|---|---------|--------|
| 1 | Comportamento de importação: merge vs. substituição total | Em aberto |
| 2 | Limite exato de profundidade de aninhamento de pastas | Fechado — `MAX_FOLDER_DEPTH = 5` (ver 12.1) |
| 3 | Cascata de bloqueio em subpastas quando pasta-pai atinge limite do plano | Em aberto |
| 4 | UI de resolução de conflito: automática vs. escolha manual do usuário | Em aberto |
| 5 | Valor exato do limite de contas/pastas no plano Free | Em aberto |
| 6 | Parâmetros específicos do Argon2id (memória/iterações/paralelismo) por plataforma | Em aberto |
| 7 | Mecanismo de armazenamento seguro adicional para Linux (sem keystore padrão) | Em aberto |

---

## 12. Decisões Fechadas (Registro Histórico)

- Zero-knowledge como princípio arquitetural inegociável.
- Recovery de master password: modelo Secret Key (estilo 1Password), sem recuperação server-side.
- Resolução de conflitos de sync: versionamento, não last-write-wins silencioso.
- Export protegido por passphrase BIP39-style, derivada via Argon2id, cifrando o arquivo com AES-256-GCM. **(Revisar quando o export for implementado — avaliar migrar também para XChaCha20-Poly1305 por consistência com a decisão abaixo.)**
- Regra de downgrade: contagem de itens não diminui, mas criação é bloqueada acima do limite.
- Retenção de dados em nuvem: 45 dias + aviso de exclusão em 48h.
- **(30/07/2026)** Cifragem do cofre local: XChaCha20-Poly1305 via libsodium, substituindo a escolha original de AES-256-GCM (ver seção 3.3) — motivo: independência de hardware AES-NI, comportamento seguro uniforme em todas as plataformas-alvo (Android, iOS, Desktop).

### 12.1 Decisões da fase de implementação do núcleo local (offline)

- **Campos em claro vs. cifrados:** título do item e nome da pasta ficam em texto claro no armazenamento local, para permitir listagem/busca sem decriptar todo o cofre. Usuário/login, senha, URL e notas são sempre cifrados (XChaCha20-Poly1305). Título não é considerado segredo por si só (ex.: "Gmail pessoal").
- **Versionamento nesta fase:** cada item mantém apenas um contador `version: Int`, incrementado a cada edição — sem histórico completo persistido. Histórico completo (necessário para resolução de conflitos de sync, seção 6.2) fica para quando a sincronização em nuvem for implementada.
- **Armazenamento seguro por plataforma nesta fase:** implementação real (Windows DPAPI) apenas para Desktop, plataforma prioritária de testes. Android (Keystore) e iOS (Keychain) e Linux (libsecret/gnome-keyring) ficam com placeholders funcionais simples, claramente marcados no código para endurecimento antes de qualquer uso em produção.
- **Limite de profundidade de pastas:** fixado em `MAX_FOLDER_DEPTH = 5` (dentro da faixa sugerida na seção 4.1).
