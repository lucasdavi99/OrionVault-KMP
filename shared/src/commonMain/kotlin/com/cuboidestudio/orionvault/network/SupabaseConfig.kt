package com.cuboidestudio.orionvault.network

/**
 * Endereço e chave pública (`anon`) do projeto Supabase.
 *
 * Os valores vêm de [GeneratedSupabaseConfig], um arquivo Kotlin escrito em build time pela
 * task `:shared:generateSupabaseConfig` a partir de `secrets.properties` (git-ignorado) ou das
 * variáveis de ambiente `SUPABASE_URL`/`SUPABASE_ANON_KEY`. Nada de chave literal em código
 * versionado. Ver `backend/supabase/README.md`.
 *
 * A chave `anon` é pública por natureza (vai embutida no app); o que protege os dados é RLS +
 * o fato de todo conteúdo sensível trafegar apenas como ciphertext AEAD. A chave
 * `service_role` NUNCA pode aparecer aqui.
 */
object SupabaseConfig {
    // A tela "Data API" do Supabase mostra a URL já com `/rest/v1/` no final (é o valor que a
    // pessoa mais provavelmente copia para SUPABASE_URL, mesmo o guia pedindo só a origem) -
    // removemos esse sufixo aqui para que auth/rest sempre montem o path certo independente do
    // que foi colado em `secrets.properties`.
    val url: String get() = GeneratedSupabaseConfig.URL.trimEnd('/').removeSuffix("/rest/v1")
    val anonKey: String get() = GeneratedSupabaseConfig.ANON_KEY

    val authBaseUrl: String get() = "$url/auth/v1"
    val restBaseUrl: String get() = "$url/rest/v1"
}
