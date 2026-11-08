import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var settings: AppSettings
    @State private var cacheCleared = false

    private let altDirect = URL(string: "https://altdirect.app/?url=https%3A%2F%2Fraw.githubusercontent.com%2Faepikgamer-art%2Fjetphotos-ios%2Fmain%2Faltstore-source.json")!
    private let liveContainerSource = URL(string: "livecontainer://source?url=https%3A%2F%2Fraw.githubusercontent.com%2Faepikgamer-art%2Fjetphotos-ios%2Fmain%2Faltstore-source.json")!
    private let ipaDownload = URL(string: "https://github.com/aepikgamer-art/jetphotos-ios/releases/latest/download/JetPhotos.ipa")!
    private let sourceLink = URL(string: "https://github.com/aepikgamer-art/jetphotos-ios")!
    private let apiLink = URL(string: "https://www.jetapi.dev")!

    var body: some View {
        NavigationStack {
            Form {
                Section("Photos") {
                    Stepper("Photos per search: \(settings.photoCount)", value: $settings.photoCount, in: 1...20)
                    Button("Clear image cache") {
                        URLCache.shared.removeAllCachedResponses()
                        cacheCleared = true
                    }
                    if cacheCleared {
                        Text("Image cache cleared")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Section("Install") {
                    Link(destination: altDirect) {
                        Label("Add to AltStore via AltDirect", systemImage: "arrow.down.app")
                    }
                    Link(destination: liveContainerSource) {
                        Label("Add source to LiveContainer", systemImage: "plus.circle")
                    }
                    Link(destination: ipaDownload) {
                        Label("Download latest IPA", systemImage: "square.and.arrow.down")
                    }
                    Link(destination: sourceLink) {
                        Label("Source on GitHub", systemImage: "curlybraces")
                    }
                }

                Section("About") {
                    LabeledContent("JetPhotos", value: "v2.06")
                    LabeledContent("Photo source", value: "JetPhotos")
                    Link("jetapi.dev", destination: apiLink)
                    Text("Photos remain credited to their photographers on JetPhotos. JetPhotos is an independent photo browsing app and is not affiliated with JetPhotos.com.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
