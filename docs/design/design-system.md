---
name: OrionVault
theme: dark-only
colors:
  background: '#0a0a0f'
  surface: '#0a0a0f'
  surface-dim: '#0a0a0f'
  surface-bright: '#2c2c3b'
  surface-container-lowest: '#070709'
  surface-container-low: '#101017'
  surface-container: '#12121a'
  surface-container-high: '#1b1b26'
  surface-container-highest: '#242432'
  surface-variant: '#242432'
  on-background: '#f4f4f7'
  on-surface: '#f4f4f7'
  on-surface-variant: '#a1a1b5'
  text-muted: '#6e6e85'
  outline: '#3a3a4a'
  outline-variant: '#26262f'
  surface-tint: '#a78bfa'
  primary: '#a78bfa'
  on-primary: '#21103f'
  primary-container: '#6d46f2'
  on-primary-container: '#ffffff'
  inverse-primary: '#5b34d6'
  secondary: '#34d399'
  on-secondary: '#04291c'
  secondary-container: '#0e4535'
  on-secondary-container: '#6ee7b7'
  tertiary: '#fbbf24'
  on-tertiary: '#2e1f00'
  tertiary-container: '#4a3306'
  on-tertiary-container: '#fde68a'
  error: '#f87171'
  on-error: '#2e0a0a'
  error-container: '#4c1414'
  on-error-container: '#fca5a5'
  inverse-surface: '#f4f4f7'
  inverse-on-surface: '#12121a'
  scrim: '#000000'
typography:
  display-lg:
    fontFamily: IBM Plex Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.03em
  headline-lg:
    fontFamily: IBM Plex Sans
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: IBM Plex Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.015em
  title-lg:
    fontFamily: IBM Plex Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: IBM Plex Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: IBM Plex Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 21px
  label-sm:
    fontFamily: IBM Plex Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.06em
  code-md:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.02em
  code-lg:
    fontFamily: JetBrains Mono
    fontSize: 18px
    fontWeight: '500'
    lineHeight: 30px
    letterSpacing: 0.08em
rounded:
  input: 10px
  button: 12px
  card: 14px
  card-lg: 18px
  dialog: 24px
  full: 9999px
spacing:
  xxs: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 48px
  screen-margin: 20px
---

> Este documento descreve o sistema **em vigor no código**. A implementação vive em
> `shared/src/commonMain/kotlin/com/cuboidestudio/orionvault/ui/theme/` (tokens) e
> `.../ui/components/` (biblioteca). Se algo aqui divergir do código, o código é a verdade —
> atualize este arquivo.

## Marca e estilo

O conceito é o **Cofre Digital**: um ambiente de alta segurança que parece impenetrável e ao mesmo
tempo tecnologicamente avançado. A personalidade é autoritativa, precisa e sofisticada.

O canvas é um **preto neutro** (`#0A0A0F`), não um azul-marinho escuro. Essa foi a mudança mais
consequente da reformulação: um fundo neutro faz as cores de acento "acenderem" em vez de competirem
com o matiz do fundo. Sobre ele, profundidade vem de **camadas tonais e vidro**, nunca de sombras
pesadas.

## Cores

Dark-only por decisão de produto — não existe conjunto de tokens claros, e adicionar um exigiria
uma segunda paleta completa.

- **Primária (Violeta).** `#A78BFA` para ícones, links, foco e momentos de marca sobre o canvas
  (7.2:1). O preenchimento de botão é mais escuro, `#6D46F2 → #5B34D6` em gradiente, com texto
  branco (5.5–6.5:1). Essa separação existe por contraste: o violeta claro com texto claro por cima
  media 3.7:1, abaixo do mínimo AA.
- **Secundária (Esmeralda).** `#34D399`, reservada **estritamente** a "seguro", "verificado",
  "copiado" e força de senha alta. Nunca decorativa — se o verde aparece, significa algo.
- **Terciária (Âmbar).** `#FBBF24` para avisos e força de senha média.
- **Erro (Vermelho).** `#F87171` para destrutivo, violado e inseguro.
- **Neutros.** Rampa de containers de `#070709` (fundo de input, mais escuro que o pai) até
  `#2C2C3B`, criando hierarquia sem sombra.

Texto: `#F4F4F7` primário (18:1), `#A1A1B5` secundário (8.6:1), `#6E6E85` de apoio (4.2:1 — só para
texto grande ou não essencial).

## Tipografia

**IBM Plex Sans** para toda a interface e **JetBrains Mono** para dados sensíveis. Ambas empacotadas
em `shared/src/commonMain/composeResources/font/` (licença OFL; textos em `docs/licenses/`).

A escolha foi feita por **desambiguação de caracteres**, não por estética. Em Plex Sans o `l`
minúsculo tem cauda curva, o `I` maiúsculo é barra reta e o `1` tem base — impossível confundir. Em
JetBrains Mono o zero é cortado e os caracteres são largos. Num cofre de senhas isso é funcional: é
a diferença entre transcrever uma Secret Key certa ou errada.

O estilo `code-lg` existe só para a exibição única da Secret Key no onboarding, com tracking bem
aberto, porque é o texto que o usuário vai copiar à mão para um papel.

Headlines levam tracking negativo para ficarem compactas e "travadas".

## Layout e espaçamento

Escala de 4px, exposta em `OrionSpacing`. Margem lateral de tela: 20px.

Larguras máximas de coluna (`OrionSizes`), com o conteúdo centralizado acima delas:
`contentNarrow 420dp` (unlock, onboarding, pastas), `contentForm 560dp` (editor de item, sync),
`contentWide 960dp` (listas do cofre).

A grade de pastas é adaptativa (`GridCells.Adaptive(168dp)`) — o número de colunas sai da largura
disponível, sem breakpoints escritos à mão.

## Elevação e profundidade

Profundidade vem de **camadas tonais e vidro**, não de sombra.

1. **Nível 0 — Canvas.** `#0A0A0F` com dois halos radiais que derivam lentamente (violeta no topo à
   esquerda, esmeralda embaixo à direita, ciclo de 22s). Implementado em `OrionBackground`, com raio
   derivado do tamanho real da tela.
2. **Nível 1 — Painéis.** `OrionSurface`: preenchimento em gradiente vertical (`#1A1A26 → #121219`)
   e borda hairline também em gradiente (branco 12% no topo → 4% na base), simulando luz batendo na
   quina superior do vidro.
3. **Nível 2 — Diálogos.** `surface-container-high` com raio de 24px.

Desfoque de fundo real (`backdrop-filter`) **não é usado**: não é portável entre os alvos do Compose
Multiplatform. A profundidade é aproximada com gradiente e borda.

## Formas

- Inputs: 10px · Botões: 12px · Cards: 14px · Cards grandes: 18px · Diálogos: 24px · Badges: pílula.
- O medidor de força de senha usa cantos externos arredondados e internos retos — o detalhe de
  "hardware usinado" para barras de progresso.

## Movimento

`OrionMotion` define o vocabulário: 150ms (rápido), 250ms (médio), 400ms (lento), com easing
enfatizado, mais o ciclo ambiente de 22s.

Onde há movimento: transição entre rotas (deslize + fade, direção pela profundidade da rota),
entrada e reordenação de itens de lista, escala de 0.97–0.985 no toque de botões e cards, anel de
foco dos campos, cor e preenchimento do medidor de senha, ícone de sync girando, respiração do
cadeado no unlock, indicador de passo do onboarding.

## Componentes

Tudo em `.../ui/components/`. Nada de `Box + clickable` avulso: se precisa de um botão, use
`OrionButton`.

- **`OrionButton`** — variantes `Primary` (gradiente violeta), `Secondary` (sólido discreto),
  `Ghost` (só contorno), `Destructive` (contorno vermelho; nunca preenchido, para não convidar ao
  clique). Suporta ícone, estado de carregamento e desabilitado.
- **`OrionTextField`** — rótulo acima do campo, fundo mais escuro que o pai, anel de foco violeta
  animado, slot de erro animado, e flag `mono` para senhas e chaves.
- **`OrionSurface`** — o card de vidro. **`OrionBackground`** — o canvas ambiente.
- **`OrionScaffold` / `OrionTopBar` / `OrionLargeTopBar`** — estrutura de tela com insets de sistema
  tratados e containers transparentes.
- **`SecurityBadge`** — pílula `Secure` (esmeralda, escudo) / `Warning` (âmbar) / `Danger`
  (vermelho).
- **`ItemAvatar`** — inicial sobre gradiente determinístico pelo título, para reencontrar
  credenciais de relance numa lista longa.
- **`PasswordStrengthMeter`** — quatro segmentos com cor interpolada.
- **`OrionEmptyState`**, **`OrionSectionHeader`**, **`OrionDialog`** — estados vazios, cabeçalhos de
  seção em caixa alta e confirmações (toda ação destrutiva passa por uma).
