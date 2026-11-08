import SwiftUI

@main
struct JetPhotosApp: App {
    @StateObject private var settings = AppSettings()

    var body: some Scene {
        WindowGroup {
            RootTabView()
                .environmentObject(settings)
        }
    }
}

final class AppSettings: ObservableObject {
    @AppStorage("photoCount") var photoCount: Int = 12
}
