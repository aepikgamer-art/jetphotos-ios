import Foundation

enum JetAPIError: LocalizedError {
    case badQuery
    case unsupportedQuery
    case http(Int)
    case decoding

    var errorDescription: String? {
        switch self {
        case .badQuery:
            return "Enter an aircraft registration or aircraft code."
        case .unsupportedQuery:
            return "Use a registration or a single aircraft code without spaces, like N787BK or A350-1000."
        case .http(let code):
            if code == 404 { return "No photos were returned for that search. Check the registration or aircraft code." }
            if code == 400 { return "jetapi.dev rejected that search. Use a registration or a single aircraft code without spaces." }
            return "jetapi.dev returned error \(code). Try again in a bit."
        case .decoding:
            return "jetapi.dev returned photo data the app couldn't read."
        }
    }
}

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
        let searchTerm = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !searchTerm.isEmpty else { throw JetAPIError.badQuery }
        // JetAPI's reg parameter accepts registration/code tokens, not
        // multiword phrases. Reject them locally instead of sending HTTP 400.
        guard !searchTerm.contains(where: { $0.isWhitespace }) else {
            throw JetAPIError.unsupportedQuery
        }

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
