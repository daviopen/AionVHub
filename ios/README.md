# AionVHub para iOS

Versão experimental em SwiftUI do AionVHub para iPhone.

## Build no GitHub Actions

O workflow `.github/workflows/ios.yml` usa um runner macOS, gera o projeto Xcode com XcodeGen, compila sem assinatura e publica o artefato `AionVHub-iOS-unsigned` contendo o arquivo `AionVHub-unsigned.ipa`.

## Instalação sem Mac

O IPA gerado é **não assinado**. Para instalar em um iPhone sem publicar na App Store, ele precisa ser assinado com um Apple ID por uma ferramenta de sideload compatível, como SideStore ou AltStore. Contas Apple gratuitas normalmente exigem renovação periódica da assinatura.

## Requisitos

- iOS 16 ou superior
- SwiftUI
- XcodeGen no ambiente de build
