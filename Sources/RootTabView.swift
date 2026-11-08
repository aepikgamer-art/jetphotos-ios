import SwiftUI

// MARK: - Root

struct RootTabView: View {
    @AppStorage("jetglass.tabBarStyle") private var tabBarStyle = "liquidGlass"
    @State private var selectedTab: Tab = .map

    enum Tab: String, CaseIterable {
        case map, photos, settings

        var title: String {
            switch self {
            case .map: return "Map"
            case .photos: return "Photos"
            case .settings: return "Settings"
            }
        }

        var symbol: String {
            switch self {
            case .map: return "map"
            case .photos: return "photo.stack"
            case .settings: return "gearshape"
            }
        }
    }

    var body: some View {
        Group {
            switch selectedTab {
            case .map:
                LiveMapView(openPhotosFor: { query in
                    PhotoSearch.shared.pendingQuery = query
                    selectedTab = .photos
                })
            case .photos:
                PhotosView()
            case .settings:
                SettingsView()
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            tabBar
                .padding(.horizontal, 18)
                .padding(.top, 8)
                .padding(.bottom, 6)
        }
        .tint(.cyan)
    }

    private var tabBar: some View {
        HStack(spacing: 8) {
            ForEach(Tab.allCases, id: \.self) { tab in
                Button {
                    selectedTab = tab
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: tab.symbol)
                            .font(.system(size: 18, weight: selectedTab == tab ? .semibold : .regular))
                        Text(tab.title)
                            .font(.system(size: 10, weight: selectedTab == tab ? .semibold : .medium))
                    }
                    .foregroundStyle(selectedTab == tab ? Color.cyan : Color.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(tab.title)
                .accessibilityAddTraits(selectedTab == tab ? .isSelected : [])
            }
        }
        .padding(.horizontal, 8)
        .background {
            if #available(iOS 26.0, *), tabBarStyle == "liquidGlass" {
                Capsule().fill(.clear).glassEffect(.regular, in: .capsule)
            } else {
                Capsule().fill(.ultraThinMaterial)
                    .overlay(Capsule().strokeBorder(.white.opacity(0.12), lineWidth: 1))
            }
        }
        .shadow(color: .black.opacity(0.10), radius: 12, y: 4)
    }
}

/// Allows the map to hand a registration or keyword to the Photos tab.
final class PhotoSearch: ObservableObject {
    static let shared = PhotoSearch()
    @Published var pendingQuery: String?
    private init() {}
}
