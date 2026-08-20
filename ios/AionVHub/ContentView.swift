import SwiftUI
import UIKit

struct HubItem: Identifiable {
    let id = UUID()
    let title: String
    let subtitle: String
    let systemImage: String
    let primaryURL: URL
    let fallbackURL: URL?
}

struct ContentView: View {
    @Environment(\.openURL) private var openURL

    private let items: [HubItem] = [
        HubItem(
            title: "Google Maps",
            subtitle: "Navegação",
            systemImage: "map.fill",
            primaryURL: URL(string: "comgooglemaps://")!,
            fallbackURL: URL(string: "https://maps.google.com")!
        ),
        HubItem(
            title: "Waze",
            subtitle: "Trânsito e rotas",
            systemImage: "car.fill",
            primaryURL: URL(string: "waze://")!,
            fallbackURL: URL(string: "https://waze.com/ul")!
        ),
        HubItem(
            title: "Spotify",
            subtitle: "Música",
            systemImage: "music.note",
            primaryURL: URL(string: "spotify://")!,
            fallbackURL: URL(string: "https://open.spotify.com")!
        ),
        HubItem(
            title: "YouTube",
            subtitle: "Vídeos",
            systemImage: "play.rectangle.fill",
            primaryURL: URL(string: "youtube://")!,
            fallbackURL: URL(string: "https://youtube.com")!
        ),
        HubItem(
            title: "Apple Maps",
            subtitle: "Navegação nativa",
            systemImage: "location.fill",
            primaryURL: URL(string: "maps://")!,
            fallbackURL: nil
        ),
        HubItem(
            title: "Ajustes",
            subtitle: "Configurações do iPhone",
            systemImage: "gearshape.fill",
            primaryURL: URL(string: UIApplication.openSettingsURLString)!,
            fallbackURL: nil
        )
    ]

    private let columns = [
        GridItem(.flexible(), spacing: 14),
        GridItem(.flexible(), spacing: 14)
    ]

    var body: some View {
        NavigationStack {
            ZStack {
                LinearGradient(
                    colors: [Color.black, Color(red: 0.05, green: 0.09, blue: 0.14)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("AionVHub")
                                .font(.system(size: 34, weight: .bold, design: .rounded))
                                .foregroundStyle(.white)
                            Text("Seu hub rápido para navegação, mídia e utilidades")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }

                        LazyVGrid(columns: columns, spacing: 14) {
                            ForEach(items) { item in
                                Button {
                                    open(item)
                                } label: {
                                    HubCard(item: item)
                                }
                                .buttonStyle(.plain)
                            }
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Label("Versão iOS experimental", systemImage: "iphone")
                                .font(.headline)
                            Text("O iOS limita a interação direta com outros apps e com o sistema do veículo. O AionVHub usa links oficiais e atalhos permitidos pelo sistema.")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                        .padding()
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 18))
                    }
                    .padding(20)
                }
            }
            .toolbar(.hidden, for: .navigationBar)
        }
        .preferredColorScheme(.dark)
    }

    private func open(_ item: HubItem) {
        openURL(item.primaryURL) { accepted in
            guard !accepted, let fallback = item.fallbackURL else { return }
            openURL(fallback)
        }
    }
}

private struct HubCard: View {
    let item: HubItem

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Image(systemName: item.systemImage)
                .font(.system(size: 28, weight: .semibold))
                .frame(width: 48, height: 48)
                .background(.white.opacity(0.09), in: RoundedRectangle(cornerRadius: 14))

            Spacer(minLength: 4)

            Text(item.title)
                .font(.headline)
                .foregroundStyle(.white)

            Text(item.subtitle)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, minHeight: 150, alignment: .leading)
        .padding(16)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 20))
        .overlay {
            RoundedRectangle(cornerRadius: 20)
                .stroke(.white.opacity(0.08), lineWidth: 1)
        }
    }
}

#Preview {
    ContentView()
}
