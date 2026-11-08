import Foundation

enum JetAPIError: LocalizedError {
    case badQuery
    case http(Int)
    case decoding

    var errorDescription: String? {
        switch self {
        case .badQuery:
            return "Enter an aircraft registration or photo search term."
        case .http(let code):
            if code == 404 { return "No photos were returned for that search. Try another registration or keyword." }
            return "jetapi.dev returned error \(code). Try again in a bit."
        case .decoding:
            return "jetapi.dev returned photo data the app couldn't read."
        }
    }
}

/// Photo-only client for https://www.jetapi.dev.
/// GET /api?reg=<query>&photos=N&only_jp=true
final class JetAPIClient {
    private let session: URLSession
    private let baseURL = URL(string: "https://www.jetapi.dev")!

    init() {
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 30
        config.waitsForConnectivity = true
        session = URLSession(configuration: config)
    }

    func lookup(query: String, photos: Int) async throws -> JetAPIResponse {
        // Keep spaces and punctuation in keyword searches intact. URLQueryItem
        // performs the required percent-encoding for the API request.
        let searchTerm = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !searchTerm.isEmpty else { throw JetAPIError.badQuery }

        var components = URLComponents(url: baseURL.appendingPathComponent("api"), resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "reg", value: searchTerm),
            URLQueryItem(name: "photos", value: String(min(20, max(1, photos)))),
            URLQueryItem(name: "only_jp", value: "true")
        ]
        guard let url = components?.url else { throw JetAPIError.badQuery }

        let (data, response) = try await session.data(from: url)
        if let http = response as? HTTPURLResponse, !(200...299).contains(http.statusCode) {
            throw JetAPIError.http(http.statusCode)
        }
        do {
            return try JSONDecoder().decode(JetAPIResponse.self, from: data)
        } catch {
            throw JetAPIError.decoding
        }
    }
}
