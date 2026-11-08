import SwiftUI

struct PhotosView: View {
    @EnvironmentObject private var settings: AppSettings
    @StateObject private var model = PhotosModel()
    @State private var query = ""
    @State private var selectedPhoto: JetPhoto?
    @FocusState private var searchFocused: Bool

    private let columns = [GridItem(.adaptive(minimum: 145), spacing: 10)]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    searchBar

                    if let error = model.error {
                        ContentUnavailableView("No photos found", systemImage: "photo.badge.exclamationmark",
                                               description: Text(error))
                    } else if model.photos.isEmpty && !model.isLoading {
                        ContentUnavailableView("Search aircraft photos", systemImage: "photo.on.rectangle.angled",
                                               description: Text("Enter a registration like N142UW or an aircraft type like A350."))
                    } else {
                        LazyVGrid(columns: columns, spacing: 10) {
                            ForEach(model.photos) { photo in
                                Button {
                                    selectedPhoto = photo
                                } label: {
                                    photoCell(photo)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("JetPhotos")
            .navigationBarTitleDisplayMode(.large)
            .sheet(item: $selectedPhoto) { photo in
                PhotoDetailView(photo: photo)
                    .presentationDetents([.large])
            }
        }
    }

    private var searchBar: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            TextField("Registration or aircraft type", text: $query)
                .textInputAutocapitalization(.characters)
                .autocorrectionDisabled()
                .focused($searchFocused)
                .onSubmit(runSearch)
            if model.isLoading {
                ProgressView()
            } else if !query.isEmpty {
                Button {
                    query = ""
                    model.reset()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                }
                .buttonStyle(.plain)
            } else {
                Button("Search", action: runSearch)
                    .buttonStyle(.borderedProminent)
            }
        }
        .padding(12)
        .background(.background.secondary, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func photoCell(_ photo: JetPhoto) -> some View {
        AsyncImage(url: photo.thumbnailURL) { phase in
            switch phase {
            case .success(let image):
                image.resizable().scaledToFill()
            default:
                ZStack {
                    Rectangle().fill(.quaternary)
                    Image(systemName: "photo")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .frame(height: 118)
        .frame(maxWidth: .infinity)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(alignment: .bottomLeading) {
            Text(photo.airline.isEmpty ? photo.aircraft : photo.airline)
                .font(.caption2.weight(.semibold))
                .padding(7)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(.ultraThinMaterial)
                .lineLimit(1)
        }
    }

    private func runSearch() {
        searchFocused = false
        Task { await model.search(query: query, photoCount: settings.photoCount) }
    }
}

@MainActor
final class PhotosModel: ObservableObject {
    @Published var photos: [JetPhoto] = []
    @Published var isLoading = false
    @Published var error: String?

    func search(query: String, photoCount: Int) async {
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        isLoading = true
        error = nil
        photos = []
        defer { isLoading = false }

        do {
            let response = try await JetAPIClient().lookup(query: query, photos: photoCount)
            photos = response.jetPhotos?.images ?? []
            if photos.isEmpty {
                error = "Nothing found for “\(query)”. Check the registration and try again."
            }
        } catch {
            self.error = error.localizedDescription
        }
    }

    func reset() {
        photos = []
        error = nil
    }
}

struct PhotoDetailView: View {
    let photo: JetPhoto

    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @State private var saving = false
    @State private var savedMessage: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    AsyncImage(url: photo.imageURL) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().scaledToFit()
                        default:
                            Rectangle()
                                .fill(.quaternary)
                                .frame(height: 260)
                                .overlay(ProgressView())
                        }
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                    HStack(spacing: 12) {
                        Button {
                            download()
                        } label: {
                            if saving {
                                ProgressView()
                            } else {
                                Label("Save photo", systemImage: "square.and.arrow.down")
                            }
                        }
                        .buttonStyle(.borderedProminent)

                        if let url = photo.imageURL {
                            ShareLink(item: url) {
                                Label("Share", systemImage: "square.and.arrow.up")
                            }
                            .buttonStyle(.bordered)
                        }

                        Button {
                            if let url = URL(string: photo.link) { openURL(url) }
                        } label: {
                            Label("Source", systemImage: "link")
                        }
                        .buttonStyle(.bordered)
                    }

                    if let savedMessage {
                        Text(savedMessage)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }

                    photoDetails
                }
                .padding()
            }
            .navigationTitle(photo.airline.isEmpty ? "Photo" : photo.airline)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private var photoDetails: some View {
        VStack(alignment: .leading, spacing: 10) {
            detailRow("Aircraft", photo.aircraft)
            detailRow("Airline", photo.airline)
            detailRow("Serial", photo.serial)
            detailRow("Location", photo.location)
            detailRow("Photographer", photo.photographer)
            detailRow("Date taken", photo.dateTaken)
            detailRow("Uploaded", photo.dateUploaded)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(.background.secondary, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func detailRow(_ title: String, _ value: String) -> some View {
        HStack(alignment: .top) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(width: 100, alignment: .leading)
            Text(value.isEmpty ? "—" : value)
                .font(.subheadline)
            Spacer(minLength: 0)
        }
    }

    private func download() {
        saving = true
        savedMessage = nil
        Task {
            do {
                try await ImageSaver.save(from: photo.image)
                savedMessage = "Saved to your photo library."
            } catch {
                savedMessage = error.localizedDescription
            }
            saving = false
        }
    }
}
