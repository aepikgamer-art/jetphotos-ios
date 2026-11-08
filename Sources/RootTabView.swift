import SwiftUI

/// Uses the system tab bar. iOS 26 renders its native Liquid Glass tab bar;
/// earlier supported iOS versions use Apple's standard tab bar appearance.
struct RootTabView: View {
    var body: some View {
        TabView {
            PhotosView()
                .tabItem {
                    Label("Photos", systemImage: "photo.on.rectangle.angled")
                }

            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
        }
        .tint(.cyan)
    }
}
