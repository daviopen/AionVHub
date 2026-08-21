# AION V Hub — distribuição privada e assinatura fixa

## Objetivo

A partir da v0.5.0, os builds de release podem usar uma chave fixa. Isso permite instalar futuras versões por cima da anterior e prepara o AAB para uma faixa privada de testes da Google Play.

A chave privada **não deve ser adicionada ao repositório**.

## GitHub Actions secrets

Cadastre em Settings > Secrets and variables > Actions > New repository secret:

- `AION_KEYSTORE_BASE64`
- `AION_KEYSTORE_PASSWORD`
- `AION_KEY_ALIAS`
- `AION_KEY_PASSWORD`

Quando os quatro segredos existirem, o workflow gera automaticamente:

- `app-release.apk` — APK assinado para instalação direta
- `app-release.aab` — Android App Bundle para Google Play

Sem os segredos, o workflow continua produzindo apenas o APK debug para desenvolvimento.

## Primeira migração para a chave fixa

Builds debug anteriores foram assinados por chaves temporárias diferentes do GitHub Actions. Por isso, ao migrar para o primeiro APK release com chave fixa, será necessária uma última desinstalação do APK debug.

Depois disso, mantenha a mesma chave e aumente `versionCode` a cada versão. As atualizações release poderão ser instaladas por cima.

## Google Play — faixa privada

Para testar sem publicar o app ao público:

1. Criar o aplicativo no Google Play Console com o package `com.aionvhub.app`.
2. Ativar Play App Signing.
3. Usar uma faixa de teste interno ou fechado.
4. Adicionar apenas as contas Google autorizadas como testadores.
5. Enviar `app-release.aab`.
6. Instalar o aplicativo no aparelho usando o link da faixa de teste e a Google Play.

A distribuição privada não torna o aplicativo público na loja.

## Segurança da chave

Guarde uma cópia offline do keystore e das senhas. Se a chave de upload for perdida, a recuperação depende do fluxo permitido pela Google Play. Para instalações diretas fora da Play, perder a chave impede atualizar por cima de versões assinadas com ela.
