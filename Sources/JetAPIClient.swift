import Foundation

enum JetAPIError: LocalizedError {
    case badQuery
    case http(Int)
    case decoding

    var errorDescription: String? {
        switch self {
        case .badQuery:
            return "Search using an aircraft registration or keyword."
        case .http(let code):
            if code == 404 { return "No photos found for that search." }
            return "jetapi.dev returned error \(code). Try again in a bit."
        case .decoding:
            return "Couldn't read the photo results from jetapi.dev."
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
        let sanitized = query
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .uppercased()
            .replacingOccurrences(of: " ", with: "-")

        guard !sanitized.isEmpty,
              sanitized.allSatisfy({ $0.isLetter || $0.isNumber || $0 == "-" }) else {
            throw JetAPIError.badQuery
        }

        var components = URLComponents(url: baseURL.appendingPathComponent("api"), resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "reg", value: sanitized),
            URLQueryItem(name: "photos", value: String(max(1, photos))),
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
